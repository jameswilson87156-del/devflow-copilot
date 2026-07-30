package com.devflow.copilot.common;

import java.util.UUID;

/**
 * Deliberately content-free data captured for an external provider failure.
 * It must never contain response bodies, URLs, credentials, or header values.
 */
public record ProviderFailureMetadata(
        ProviderFailureStage failureStage,
        Integer httpStatusCode,
        ProviderHttpStatusFamily httpStatusFamily,
        Boolean responseBodyPresent,
        ProviderResponseSizeBucket responseBodySizeBucket,
        Boolean contentTypePresent,
        ProviderContentTypeCategory contentTypeCategory,
        Boolean retryAfterHeaderPresent,
        Boolean upstreamRequestIdHeaderPresent,
        String clientRequestId,
        ProviderDurationBucket durationBucket
) {

    public ProviderFailureMetadata {
        failureStage = failureStage == null ? ProviderFailureStage.UNKNOWN : failureStage;
        httpStatusFamily = httpStatusFamily == null ? ProviderHttpStatusFamily.UNKNOWN : httpStatusFamily;
        responseBodySizeBucket = responseBodySizeBucket == null ? ProviderResponseSizeBucket.UNKNOWN : responseBodySizeBucket;
        contentTypeCategory = contentTypeCategory == null ? ProviderContentTypeCategory.UNKNOWN : contentTypeCategory;
        durationBucket = durationBucket == null ? ProviderDurationBucket.UNKNOWN : durationBucket;
    }

    public static ProviderFailureMetadata unknown() {
        return new ProviderFailureMetadata(
                ProviderFailureStage.UNKNOWN, null, ProviderHttpStatusFamily.UNKNOWN,
                null, ProviderResponseSizeBucket.UNKNOWN, null, ProviderContentTypeCategory.UNKNOWN,
                null, null, null, ProviderDurationBucket.UNKNOWN
        );
    }

    public static ProviderFailureMetadata atStage(ProviderFailureStage stage) {
        return new ProviderFailureMetadata(
                stage, null, ProviderHttpStatusFamily.UNKNOWN,
                null, ProviderResponseSizeBucket.UNKNOWN, null, ProviderContentTypeCategory.UNKNOWN,
                null, null, null, ProviderDurationBucket.UNKNOWN
        );
    }

    public static String newClientRequestId() {
        return UUID.randomUUID().toString();
    }
}
