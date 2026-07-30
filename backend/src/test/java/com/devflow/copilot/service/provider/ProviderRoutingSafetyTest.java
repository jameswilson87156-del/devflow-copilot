package com.devflow.copilot.service.provider;

import com.devflow.copilot.common.LlmProviderException;
import com.devflow.copilot.common.ProviderErrorType;
import com.devflow.copilot.config.AiProviderProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class ProviderRoutingSafetyTest {

    @Test
    void supportedProtocolReachesCredentialGateWithoutMakingANetworkRequest() {
        AiProviderProperties properties = externalProperties();
        OpenAiCompatibleGenerationProvider provider = new OpenAiCompatibleGenerationProvider(properties);

        assertThatThrownBy(() -> provider.generate(request()))
                .isInstanceOf(LlmProviderException.class)
                .extracting(error -> ((LlmProviderException) error).getErrorType())
                .isEqualTo(ProviderErrorType.API_KEY_MISSING);
    }

    @Test
    void unsupportedProtocolFallsBackOnlyWhenEnabled() {
        AiProviderProperties enabled = externalProperties();
        enabled.setProtocol("unsupported");
        enabled.setFallbackToLocal(true);
        LocalRuleGenerationProvider local = spy(new LocalRuleGenerationProvider());
        GenerationProviderRouter enabledRouter = new GenerationProviderRouter(
                enabled, local, new OpenAiCompatibleGenerationProvider(enabled));

        ProviderResult fallback = enabledRouter.generate(request());

        assertThat(fallback.providerName()).isEqualTo(local.key());
        assertThat(fallback.fallbackReason()).contains(ProviderErrorType.PROTOCOL_UNSUPPORTED.name());
        verify(local).generate(any());

        AiProviderProperties disabled = externalProperties();
        disabled.setProtocol("unsupported");
        disabled.setFallbackToLocal(false);
        LocalRuleGenerationProvider noFallbackLocal = spy(new LocalRuleGenerationProvider());
        GenerationProviderRouter disabledRouter = new GenerationProviderRouter(
                disabled, noFallbackLocal, new OpenAiCompatibleGenerationProvider(disabled));

        assertThatThrownBy(() -> disabledRouter.generate(request()))
                .isInstanceOf(LlmProviderException.class)
                .extracting(error -> ((LlmProviderException) error).getErrorType())
                .isEqualTo(ProviderErrorType.PROTOCOL_UNSUPPORTED);
        verify(noFallbackLocal, never()).generate(any());
    }

    @Test
    void unknownProviderFailsWithoutFallbackOrNetworkRequest() {
        AiProviderProperties properties = externalProperties();
        properties.setProvider("unknown");
        LocalRuleGenerationProvider local = spy(new LocalRuleGenerationProvider());
        GenerationProviderRouter router = new GenerationProviderRouter(
                properties, local, new OpenAiCompatibleGenerationProvider(properties));

        assertThatThrownBy(() -> router.generate(request()))
                .isInstanceOf(LlmProviderException.class)
                .extracting(error -> ((LlmProviderException) error).getErrorType())
                .isEqualTo(ProviderErrorType.UNKNOWN_PROVIDER_ERROR);
        verify(local, never()).generate(any());
    }

    private AiProviderProperties externalProperties() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.setProvider("openai-compatible");
        properties.setProtocol("chat-completions-compatible");
        properties.setApiKey("");
        return properties;
    }

    private ProviderRequest request() {
        return new ProviderRequest("requirement-split", "offline test", "offline test", "project", "Java");
    }
}
