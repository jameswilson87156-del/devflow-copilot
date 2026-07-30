package com.devflow.copilot.service.provider;

public record ProviderResult(
        String content,
        String providerName,
        String modelName,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        boolean fallbackUsed,
        String fallbackReason
) {
    public ProviderResult withFallbackReason(String reason) {
        return new ProviderResult(content, providerName, modelName, promptTokens, completionTokens, totalTokens, true, reason);
    }
}
