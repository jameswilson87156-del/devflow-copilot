package com.devflow.copilot.service.provider;

import com.devflow.copilot.common.LlmProviderException;
import com.devflow.copilot.common.ProviderErrorType;
import com.devflow.copilot.common.ProviderFailureMetadata;
import com.devflow.copilot.common.ProviderFailureStage;
import com.devflow.copilot.config.AiProviderProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiCompatibleGenerationProvider implements GenerationProvider {

    private final AiProviderProperties properties;

    public OpenAiCompatibleGenerationProvider(AiProviderProperties properties) {
        this.properties = properties;
    }

    @Override
    public String key() {
        return "openai-compatible";
    }

    @Override
    public ProviderResult generate(ProviderRequest request) {
        validateProtocol();
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new LlmProviderException(ProviderErrorType.API_KEY_MISSING,
                    ProviderFailureMetadata.atStage(ProviderFailureStage.CONFIG_VALIDATION));
        }
        String baseUrl = normalizeBaseUrl(properties.getBaseUrl());
        String clientRequestId = ProviderFailureMetadata.newClientRequestId();
        long requestStartNanos = System.nanoTime();
        try {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            int timeoutMs = Math.max(1, properties.getTimeoutSeconds()) * 1000;
            requestFactory.setConnectTimeout(timeoutMs);
            requestFactory.setReadTimeout(timeoutMs);
            RestClient client = RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                    .defaultHeader("X-Client-Request-Id", clientRequestId)
                    .requestFactory(requestFactory)
                    .build();
            Map<String, Object> body = Map.of(
                    "model", properties.getModel(),
                    "messages", List.of(Map.of("role", "user", "content", request.renderedPrompt())),
                    "temperature", 0.2,
                    "max_tokens", properties.getMaxTokens()
            );
            JsonNode response = client.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.path("choices").isArray() || response.path("choices").isEmpty()) {
                throw new LlmProviderException(ProviderErrorType.INVALID_RESPONSE,
                        ProviderErrorClassifier.metadataForStage(ProviderFailureStage.RESPONSE_DESERIALIZATION,
                                clientRequestId, requestStartNanos));
            }
            JsonNode message = response.path("choices").path(0).path("message");
            if (!message.isObject() || !message.hasNonNull("content")) {
                throw new LlmProviderException(ProviderErrorType.INVALID_RESPONSE,
                        ProviderErrorClassifier.metadataForStage(ProviderFailureStage.RESPONSE_DESERIALIZATION,
                                clientRequestId, requestStartNanos));
            }
            String content = message.path("content").asText();
            if (content == null || content.isBlank()) {
                throw new LlmProviderException(ProviderErrorType.EMPTY_CONTENT,
                        ProviderErrorClassifier.metadataForStage(ProviderFailureStage.CONTENT_EXTRACTION,
                                clientRequestId, requestStartNanos));
            }
            JsonNode usage = response.path("usage");
            Integer promptTokens = usage.has("prompt_tokens") ? usage.path("prompt_tokens").asInt() : null;
            Integer completionTokens = usage.has("completion_tokens") ? usage.path("completion_tokens").asInt() : null;
            Integer totalTokens = usage.has("total_tokens") ? usage.path("total_tokens").asInt() : null;
            return new ProviderResult(content, key(), properties.getModel(), promptTokens, completionTokens, totalTokens, false, null);
        } catch (LlmProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw ProviderErrorClassifier.sanitize(ex, clientRequestId, requestStartNanos,
                    ProviderFailureStage.REQUEST_BUILD);
        }
    }

    private void validateProtocol() {
        if (!"chat-completions-compatible".equals(properties.getProtocol())) {
            throw new LlmProviderException(ProviderErrorType.PROTOCOL_UNSUPPORTED,
                    ProviderFailureMetadata.atStage(ProviderFailureStage.CONFIG_VALIDATION));
        }
    }

    private String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new LlmProviderException(ProviderErrorType.CONNECTION_FAILED,
                    ProviderFailureMetadata.atStage(ProviderFailureStage.CONFIG_VALIDATION));
        }
        String normalized = value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
        String path = "/chat/completions";
        return normalized.endsWith(path) ? normalized.substring(0, normalized.length() - path.length()) : normalized;
    }
}
