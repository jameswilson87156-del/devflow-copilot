package com.devflow.copilot.service.provider;

import com.devflow.copilot.common.LlmProviderException;
import com.devflow.copilot.config.AiProviderProperties;
import org.springframework.stereotype.Component;

@Component
public class GenerationProviderRouter {

    private final AiProviderProperties properties;
    private final LocalRuleGenerationProvider localRule;
    private final OpenAiCompatibleGenerationProvider openAiCompatible;

    public GenerationProviderRouter(
            AiProviderProperties properties,
            LocalRuleGenerationProvider localRule,
            OpenAiCompatibleGenerationProvider openAiCompatible
    ) {
        this.properties = properties;
        this.localRule = localRule;
        this.openAiCompatible = openAiCompatible;
    }

    public ProviderResult generate(ProviderRequest request) {
        String selected = properties.getProvider() == null ? "local-rule" : properties.getProvider().trim();
        if (localRule.key().equals(selected)) {
            return localRule.generate(request);
        }
        if (!openAiCompatible.key().equals(selected)) {
            throw new LlmProviderException("不支持的 AI Provider：" + selected);
        }
        try {
            return openAiCompatible.generate(request);
        } catch (RuntimeException ex) {
            if (!properties.isFallbackToLocal()) {
                throw ex;
            }
            return localRule.generate(request).withFallbackReason(ex.getMessage());
        }
    }
}
