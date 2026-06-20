package com.devflow.copilot.dto;

public record RenderedPrompt(
        String content,
        Long templateId,
        String templateName,
        Integer templateVersion
) {
}
