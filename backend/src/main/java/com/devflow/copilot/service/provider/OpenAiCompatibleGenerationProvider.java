package com.devflow.copilot.service.provider;

import com.devflow.copilot.common.LlmProviderException;
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
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new LlmProviderException("OpenAI-compatible Provider 缺少 DEVFLOW_AI_API_KEY");
        }
        try {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            int timeoutMs = Math.max(1, properties.getTimeoutSeconds()) * 1000;
            requestFactory.setConnectTimeout(timeoutMs);
            requestFactory.setReadTimeout(timeoutMs);
            RestClient client = RestClient.builder()
                    .baseUrl(stripTrailingSlash(properties.getBaseUrl()))
                    .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
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
            if (response == null || response.path("choices").isEmpty()) {
                throw new LlmProviderException("OpenAI-compatible Provider 未返回 choices");
            }
            String content = response.path("choices").path(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                throw new LlmProviderException("OpenAI-compatible Provider 返回了空内容");
            }
            JsonNode usage = response.path("usage");
            Integer promptTokens = usage.has("prompt_tokens") ? usage.path("prompt_tokens").asInt() : null;
            Integer completionTokens = usage.has("completion_tokens") ? usage.path("completion_tokens").asInt() : null;
            Integer totalTokens = usage.has("total_tokens") ? usage.path("total_tokens").asInt() : null;
            return new ProviderResult(content, key(), properties.getModel(), promptTokens, completionTokens, totalTokens, null);
        } catch (LlmProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LlmProviderException("OpenAI-compatible 调用失败：" + safeMessage(ex), ex);
        }
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            throw new LlmProviderException("OpenAI-compatible Provider 缺少 base-url");
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String safeMessage(Exception ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? ex.getClass().getSimpleName() : ex.getMessage();
    }
}
