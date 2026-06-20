package com.devflow.copilot;

import com.devflow.copilot.common.LlmProviderException;
import com.devflow.copilot.config.AiProviderProperties;
import com.devflow.copilot.dto.LogAnalyzeRequest;
import com.devflow.copilot.dto.LogAnalyzeResponse;
import com.devflow.copilot.service.LogDiagnosisService;
import com.devflow.copilot.service.provider.GenerationProviderRouter;
import com.devflow.copilot.service.provider.LocalRuleGenerationProvider;
import com.devflow.copilot.service.provider.OpenAiCompatibleGenerationProvider;
import com.devflow.copilot.service.provider.ProviderRequest;
import com.devflow.copilot.service.provider.ProviderResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProviderAndDiagnosisTest {

    @Autowired LogDiagnosisService diagnosisService;

    @Test
    void openAiProviderRejectsMissingApiKey() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.setApiKey("");
        OpenAiCompatibleGenerationProvider provider = new OpenAiCompatibleGenerationProvider(properties);

        assertThatThrownBy(() -> provider.generate(providerRequest()))
                .isInstanceOf(LlmProviderException.class)
                .hasMessageContaining("API_KEY");
    }

    @Test
    void routerFallsBackToLocalRuleWhenOpenAiConfigIsMissing() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.setProvider("openai-compatible");
        properties.setApiKey("");
        properties.setFallbackToLocal(true);
        LocalRuleGenerationProvider local = new LocalRuleGenerationProvider();
        GenerationProviderRouter router = new GenerationProviderRouter(
                properties, local, new OpenAiCompatibleGenerationProvider(properties));

        ProviderResult result = router.generate(providerRequest());

        assertThat(result.providerName()).isEqualTo("local-rule");
        assertThat(result.fallbackReason()).contains("API_KEY");
    }

    @Test
    void diagnosesSpringBootPortConflict() {
        LogAnalyzeRequest request = new LogAnalyzeRequest();
        request.setProjectId(3L);
        request.setRawLog("Web server failed to start. Port 8080 was already in use.");

        LogAnalyzeResponse result = diagnosisService.analyze(request);

        assertThat(result.getExceptionType()).isEqualTo("Port already in use");
        assertThat(result.getRiskLevel()).isEqualTo("Low");
    }

    private ProviderRequest providerRequest() {
        return new ProviderRequest("requirement-split", "请拆解订单需求", "订单需求",
                "DevFlow Copilot", "Java 17, Vue 3");
    }
}
