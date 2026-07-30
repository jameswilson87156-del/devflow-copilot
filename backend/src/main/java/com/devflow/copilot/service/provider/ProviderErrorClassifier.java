package com.devflow.copilot.service.provider;

import com.devflow.copilot.common.LlmProviderException;
import com.devflow.copilot.common.ProviderContentTypeCategory;
import com.devflow.copilot.common.ProviderDurationBucket;
import com.devflow.copilot.common.ProviderErrorType;
import com.devflow.copilot.common.ProviderFailureMetadata;
import com.devflow.copilot.common.ProviderFailureStage;
import com.devflow.copilot.common.ProviderHttpStatusFamily;
import com.devflow.copilot.common.ProviderResponseSizeBucket;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

final class ProviderErrorClassifier {

    private ProviderErrorClassifier() {
    }

    static LlmProviderException sanitize(Exception exception, String clientRequestId, long requestStartNanos,
                                         ProviderFailureStage defaultStage) {
        if (exception instanceof LlmProviderException providerException) {
            return providerException;
        }
        ProviderErrorType errorType = classify(exception);
        return new LlmProviderException(errorType,
                metadata(exception, clientRequestId, requestStartNanos, defaultStage, errorType), exception);
    }

    static ProviderFailureMetadata metadataForStage(ProviderFailureStage stage, String clientRequestId,
                                                    long requestStartNanos) {
        return new ProviderFailureMetadata(
                stage,
                null,
                ProviderHttpStatusFamily.UNKNOWN,
                null,
                ProviderResponseSizeBucket.UNKNOWN,
                null,
                ProviderContentTypeCategory.UNKNOWN,
                null,
                null,
                clientRequestId,
                durationBucket(requestStartNanos, false)
        );
    }

    static ProviderErrorType classify(Throwable exception) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current instanceof LlmProviderException providerException) {
                return providerException.getErrorType();
            }
            if (current instanceof RestClientResponseException responseException) {
                return fromStatus(responseException.getStatusCode());
            }
            if (current instanceof SocketTimeoutException || current instanceof HttpTimeoutException) {
                return ProviderErrorType.TIMEOUT;
            }
            if (current instanceof ConnectException) {
                return ProviderErrorType.CONNECTION_FAILED;
            }
            if (current instanceof JsonProcessingException) {
                return ProviderErrorType.INVALID_RESPONSE;
            }
            if (current instanceof HttpMessageNotReadableException) {
                return ProviderErrorType.INVALID_RESPONSE;
            }
            if (current instanceof ResourceAccessException) {
                if (hasCause(current, SocketTimeoutException.class) || hasCause(current, HttpTimeoutException.class)) {
                    return ProviderErrorType.TIMEOUT;
                }
                return ProviderErrorType.CONNECTION_FAILED;
            }
        }
        return ProviderErrorType.UNKNOWN_PROVIDER_ERROR;
    }

    private static boolean hasCause(Throwable exception, Class<? extends Throwable> type) {
        for (Throwable current = exception.getCause(); current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }

    private static ProviderFailureMetadata metadata(
            Throwable exception,
            String clientRequestId,
            long requestStartNanos,
            ProviderFailureStage defaultStage,
            ProviderErrorType errorType
    ) {
        RestClientResponseException responseException = findCause(exception, RestClientResponseException.class);
        if (responseException != null) {
            return responseMetadata(responseException, clientRequestId, durationBucket(requestStartNanos, false));
        }
        ProviderFailureStage stage = stageFor(exception, defaultStage);
        boolean timeout = errorType == ProviderErrorType.TIMEOUT;
        return new ProviderFailureMetadata(
                stage,
                null,
                ProviderHttpStatusFamily.UNKNOWN,
                null,
                ProviderResponseSizeBucket.UNKNOWN,
                null,
                ProviderContentTypeCategory.UNKNOWN,
                null,
                null,
                clientRequestId,
                durationBucket(requestStartNanos, timeout)
        );
    }

    private static ProviderFailureMetadata responseMetadata(
            RestClientResponseException responseException,
            String clientRequestId,
            ProviderDurationBucket durationBucket
    ) {
        HttpHeaders headers = responseException.getResponseHeaders();
        byte[] body = responseException.getResponseBodyAsByteArray();
        int statusCode = responseException.getStatusCode().value();
        return new ProviderFailureMetadata(
                ProviderFailureStage.HTTP_STATUS_RECEIVED,
                statusCode,
                statusFamily(statusCode),
                body.length > 0,
                sizeBucket(body.length),
                headers != null && headers.getContentType() != null,
                contentTypeCategory(headers == null ? null : headers.getContentType()),
                headers != null && headers.containsKey(HttpHeaders.RETRY_AFTER),
                hasUpstreamRequestId(headers),
                clientRequestId,
                durationBucket
        );
    }

    private static ProviderFailureStage stageFor(Throwable exception, ProviderFailureStage defaultStage) {
        if (findCause(exception, JsonProcessingException.class) != null
                || findCause(exception, HttpMessageNotReadableException.class) != null) {
            return ProviderFailureStage.RESPONSE_DESERIALIZATION;
        }
        if (findCause(exception, ResourceAccessException.class) != null
                || findCause(exception, ConnectException.class) != null
                || findCause(exception, SocketTimeoutException.class) != null
                || findCause(exception, HttpTimeoutException.class) != null) {
            return ProviderFailureStage.CONNECTION;
        }
        return defaultStage == null ? ProviderFailureStage.UNKNOWN : defaultStage;
    }

    private static ProviderDurationBucket durationBucket(long requestStartNanos, boolean timeout) {
        if (timeout) {
            return ProviderDurationBucket.TIMEOUT;
        }
        if (requestStartNanos <= 0) {
            return ProviderDurationBucket.UNKNOWN;
        }
        long elapsedMs = Math.max(0, (System.nanoTime() - requestStartNanos) / 1_000_000L);
        if (elapsedMs < 1_000) {
            return ProviderDurationBucket.UNDER_1_SECOND;
        }
        if (elapsedMs < 5_000) {
            return ProviderDurationBucket.ONE_TO_FIVE_SECONDS;
        }
        if (elapsedMs < 15_000) {
            return ProviderDurationBucket.FIVE_TO_FIFTEEN_SECONDS;
        }
        if (elapsedMs < 60_000) {
            return ProviderDurationBucket.FIFTEEN_TO_SIXTY_SECONDS;
        }
        return ProviderDurationBucket.OVER_SIXTY_SECONDS;
    }

    private static ProviderHttpStatusFamily statusFamily(int statusCode) {
        if (statusCode >= 400 && statusCode < 500) {
            return ProviderHttpStatusFamily.FOUR_XX;
        }
        if (statusCode >= 500 && statusCode < 600) {
            return ProviderHttpStatusFamily.FIVE_XX;
        }
        return ProviderHttpStatusFamily.UNKNOWN;
    }

    private static ProviderResponseSizeBucket sizeBucket(int length) {
        if (length == 0) {
            return ProviderResponseSizeBucket.ZERO;
        }
        if (length <= 1024) {
            return ProviderResponseSizeBucket.ONE_TO_1KB;
        }
        if (length <= 16 * 1024) {
            return ProviderResponseSizeBucket.ONE_TO_16KB;
        }
        return ProviderResponseSizeBucket.OVER_16KB;
    }

    private static ProviderContentTypeCategory contentTypeCategory(MediaType contentType) {
        if (contentType == null) {
            return ProviderContentTypeCategory.UNKNOWN;
        }
        if (MediaType.APPLICATION_JSON.isCompatibleWith(contentType) || contentType.getSubtype().endsWith("+json")) {
            return ProviderContentTypeCategory.JSON;
        }
        if (MediaType.TEXT_HTML.isCompatibleWith(contentType)) {
            return ProviderContentTypeCategory.HTML;
        }
        if ("text".equalsIgnoreCase(contentType.getType())) {
            return ProviderContentTypeCategory.TEXT;
        }
        return ProviderContentTypeCategory.OTHER;
    }

    private static boolean hasUpstreamRequestId(HttpHeaders headers) {
        return headers != null && (headers.containsKey("X-Request-Id")
                || headers.containsKey("Request-Id")
                || headers.containsKey("X-Correlation-Id"));
    }

    private static <T extends Throwable> T findCause(Throwable exception, Class<T> type) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
        }
        return null;
    }

    private static ProviderErrorType fromStatus(HttpStatusCode statusCode) {
        if (statusCode.value() == 401 || statusCode.value() == 403) {
            return ProviderErrorType.AUTHENTICATION_FAILED;
        }
        if (statusCode.value() == 429) {
            return ProviderErrorType.RATE_LIMITED;
        }
        if (statusCode.is5xxServerError()) {
            return ProviderErrorType.UPSTREAM_SERVER_ERROR;
        }
        return ProviderErrorType.UNKNOWN_PROVIDER_ERROR;
    }
}
