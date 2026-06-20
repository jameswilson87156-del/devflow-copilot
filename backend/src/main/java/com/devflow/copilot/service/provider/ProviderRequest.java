package com.devflow.copilot.service.provider;

public record ProviderRequest(
        String generationType,
        String renderedPrompt,
        String originalInput,
        String projectName,
        String techStack
) {
}
