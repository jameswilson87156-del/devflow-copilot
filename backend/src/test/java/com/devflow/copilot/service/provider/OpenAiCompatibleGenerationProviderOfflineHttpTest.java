package com.devflow.copilot.service.provider;

import com.devflow.copilot.common.LlmProviderException;
import com.devflow.copilot.common.ProviderContentTypeCategory;
import com.devflow.copilot.common.ProviderDurationBucket;
import com.devflow.copilot.common.ProviderErrorType;
import com.devflow.copilot.common.ProviderFailureMetadata;
import com.devflow.copilot.common.ProviderFailureStage;
import com.devflow.copilot.common.ProviderHttpStatusFamily;
import com.devflow.copilot.common.ProviderResponseSizeBucket;
import com.devflow.copilot.config.AiProviderProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiCompatibleGenerationProviderOfflineHttpTest {

    private HttpServer server;
    private final AtomicInteger requestCount = new AtomicInteger();
    private final Set<String> clientRequestIds = new HashSet<>();
    private volatile int responseStatus;
    private volatile String responseBody;
    private volatile String contentType;
    private volatile Map<String, String> responseHeaders = Map.of();
    private volatile long responseDelayMs;
    private volatile String requestPath;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/v1/chat/completions", this::respond);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void parsesOfflineChatCompletionsSuccessWithoutFallback() {
        responseStatus = 200;
        responseBody = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "offline success"
                      }
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 10,
                    "completion_tokens": 4,
                    "total_tokens": 14
                  }
                }
                """;
        contentType = "application/json";
        responseHeaders = Map.of();
        responseDelayMs = 0;

        ProviderResult result = new OpenAiCompatibleGenerationProvider(properties()).generate(request());

        assertThat(requestCount.get()).isEqualTo(1);
        assertThat(requestPath).isEqualTo("/v1/chat/completions");
        assertThat(clientRequestIds).hasSize(1).allSatisfy(id -> assertThatCodeParsesAsUuid(id));
        assertThat(result.content()).isEqualTo("offline success");
        assertThat(result.providerName()).isNotBlank();
        assertThat(result.modelName()).isNotBlank();
        assertThat(result.promptTokens()).isEqualTo(10);
        assertThat(result.completionTokens()).isEqualTo(4);
        assertThat(result.totalTokens()).isEqualTo(14);
        assertThat(result.fallbackUsed()).isFalse();
        assertThat(result.fallbackReason()).isNull();
        assertThat(result.toString()).doesNotContain("offline-test-key", "Authorization", "Bearer");
    }

    @Test
    void retainsExact5xxStatusCodesAndSeparatesRateLimiting() {
        assertHttpFailure(500, "{}", "application/json", Map.of(), ProviderErrorType.UPSTREAM_SERVER_ERROR, 500);
        assertHttpFailure(502, "{}", "application/json", Map.of(), ProviderErrorType.UPSTREAM_SERVER_ERROR, 502);
        assertHttpFailure(503, "{}", "application/json", Map.of(), ProviderErrorType.UPSTREAM_SERVER_ERROR, 503);
        assertHttpFailure(504, "{}", "application/json", Map.of(), ProviderErrorType.UPSTREAM_SERVER_ERROR, 504);
        LlmProviderException rateLimited = assertHttpFailure(429, "{}", "application/json", Map.of(),
                ProviderErrorType.RATE_LIMITED, 429);

        assertThat(rateLimited.getFailureMetadata().httpStatusFamily()).isEqualTo(ProviderHttpStatusFamily.FOUR_XX);
        assertThat(clientRequestIds).hasSize(5).allSatisfy(id -> assertThatCodeParsesAsUuid(id));
    }

    @Test
    void capturesOnlySafeHtmlBodyAndHeaderFacts() {
        LlmProviderException failure = assertHttpFailure(502, "<html>offline error</html>", "text/html",
                Map.of("Retry-After", "ignored", "X-Request-Id", "ignored"),
                ProviderErrorType.UPSTREAM_SERVER_ERROR, 502);

        ProviderFailureMetadata metadata = failure.getFailureMetadata();
        assertThat(metadata.contentTypeCategory()).isEqualTo(ProviderContentTypeCategory.HTML);
        assertThat(metadata.responseBodyPresent()).isTrue();
        assertThat(metadata.responseBodySizeBucket()).isEqualTo(ProviderResponseSizeBucket.ONE_TO_1KB);
        assertThat(metadata.retryAfterHeaderPresent()).isTrue();
        assertThat(metadata.upstreamRequestIdHeaderPresent()).isTrue();
        assertThat(metadata.toString()).doesNotContain("offline error", "ignored");
    }

    @Test
    void capturesJsonAndAbsentBodiesWithoutKeepingTheirContents() {
        LlmProviderException jsonFailure = assertHttpFailure(503, "{\"error\":\"offline\"}", "application/json",
                Map.of(), ProviderErrorType.UPSTREAM_SERVER_ERROR, 503);
        assertThat(jsonFailure.getFailureMetadata().contentTypeCategory()).isEqualTo(ProviderContentTypeCategory.JSON);
        assertThat(jsonFailure.getFailureMetadata().toString()).doesNotContain("offline");

        LlmProviderException emptyFailure = assertHttpFailure(504, null, null, Map.of(),
                ProviderErrorType.UPSTREAM_SERVER_ERROR, 504);
        assertThat(emptyFailure.getFailureMetadata().responseBodyPresent()).isFalse();
        assertThat(emptyFailure.getFailureMetadata().responseBodySizeBucket()).isEqualTo(ProviderResponseSizeBucket.ZERO);
    }

    @Test
    void assignsFailureStagesForDeserializationContentTimeoutAndConnectionFailures() throws IOException {
        LlmProviderException invalidJson = failureFor(200, "not-json", "application/json", Map.of(), 0);
        assertThat(invalidJson.getErrorType()).isEqualTo(ProviderErrorType.INVALID_RESPONSE);
        assertThat(invalidJson.getFailureMetadata().failureStage())
                .isEqualTo(ProviderFailureStage.RESPONSE_DESERIALIZATION);

        LlmProviderException emptyContent = failureFor(200, "{\"choices\":[{\"message\":{\"content\":\"\"}}]}",
                "application/json", Map.of(), 0);
        assertThat(emptyContent.getErrorType()).isEqualTo(ProviderErrorType.EMPTY_CONTENT);
        assertThat(emptyContent.getFailureMetadata().failureStage()).isEqualTo(ProviderFailureStage.CONTENT_EXTRACTION);

        LlmProviderException timeout = failureFor(200, "{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}",
                "application/json", Map.of(), 1_200);
        assertThat(timeout.getErrorType()).isEqualTo(ProviderErrorType.TIMEOUT);
        assertThat(timeout.getFailureMetadata().failureStage()).isEqualTo(ProviderFailureStage.CONNECTION);
        assertThat(timeout.getFailureMetadata().durationBucket()).isEqualTo(ProviderDurationBucket.TIMEOUT);

        int unusedPort;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            unusedPort = socket.getLocalPort();
        }
        AiProviderProperties properties = properties();
        properties.setBaseUrl("http" + "://127.0.0.1:" + unusedPort + "/v1");
        LlmProviderException connection = capture(new OpenAiCompatibleGenerationProvider(properties));
        assertThat(connection.getErrorType()).isEqualTo(ProviderErrorType.CONNECTION_FAILED);
        assertThat(connection.getFailureMetadata().httpStatusCode()).isNull();
        assertThat(connection.getFailureMetadata().failureStage()).isEqualTo(ProviderFailureStage.CONNECTION);
    }

    private LlmProviderException assertHttpFailure(int status, String body, String type, Map<String, String> headers,
                                                   ProviderErrorType expectedType, int expectedStatus) {
        LlmProviderException failure = failureFor(status, body, type, headers, 0);
        assertThat(failure.getErrorType()).isEqualTo(expectedType);
        assertThat(failure.getFailureMetadata().failureStage()).isEqualTo(ProviderFailureStage.HTTP_STATUS_RECEIVED);
        assertThat(failure.getFailureMetadata().httpStatusCode()).isEqualTo(expectedStatus);
        assertThat(failure.getFailureMetadata().httpStatusFamily()).isEqualTo(
                expectedStatus >= 500 ? ProviderHttpStatusFamily.FIVE_XX : ProviderHttpStatusFamily.FOUR_XX);
        assertThat(requestCount.getAndSet(0)).isEqualTo(1);
        return failure;
    }

    private LlmProviderException failureFor(int status, String body, String type, Map<String, String> headers, long delayMs) {
        responseStatus = status;
        responseBody = body;
        contentType = type;
        responseHeaders = headers;
        responseDelayMs = delayMs;
        return capture(new OpenAiCompatibleGenerationProvider(properties()));
    }

    private LlmProviderException capture(OpenAiCompatibleGenerationProvider provider) {
        try {
            provider.generate(request());
        } catch (LlmProviderException exception) {
            return exception;
        }
        throw new AssertionError("Expected provider failure");
    }

    private AiProviderProperties properties() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.setApiKey("offline-test-key");
        properties.setProtocol("chat-completions-compatible");
        properties.setTimeoutSeconds(1);
        properties.setBaseUrl("http" + "://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        return properties;
    }

    private ProviderRequest request() {
        return new ProviderRequest("requirement-split", "offline test", "offline test", "project", "Java");
    }

    private void respond(HttpExchange exchange) throws IOException {
        requestCount.incrementAndGet();
        requestPath = exchange.getRequestURI().getPath();
        synchronized (clientRequestIds) {
            clientRequestIds.add(exchange.getRequestHeaders().getFirst("X-Client-Request-Id"));
        }
        try {
            if (responseDelayMs > 0) {
                Thread.sleep(responseDelayMs);
            }
            if (contentType != null) {
                exchange.getResponseHeaders().add("Content-Type", contentType);
            }
            responseHeaders.forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
            if (responseBody == null) {
                exchange.sendResponseHeaders(responseStatus, -1);
                return;
            }
            byte[] payload = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus, payload.length);
            exchange.getResponseBody().write(payload);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            exchange.close();
        }
    }

    private void assertThatCodeParsesAsUuid(String value) {
        assertThat(value).isNotBlank();
        assertThat(UUID.fromString(value).toString()).isEqualTo(value);
    }
}
