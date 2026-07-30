package com.devflow.copilot;

import com.devflow.copilot.dto.AiGenerateRequest;
import com.devflow.copilot.dto.AiGenerateResponse;
import com.devflow.copilot.entity.GenerationRecord;
import com.devflow.copilot.service.AiGenerateService;
import com.devflow.copilot.service.GenerationRecordService;
import com.devflow.copilot.service.LlmGenerateService;
import com.devflow.copilot.service.provider.ProviderResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {"devflow.ai.provider=openai-compatible", "devflow.ai.api-key=", "devflow.ai.protocol=chat-completions-compatible", "devflow.ai.fallback-to-local=false"})
@ActiveProfiles("test")
@Transactional
class ProviderExecutionExternalSuccessPersistenceIntegrationTest {

    @Autowired AiGenerateService aiGenerateService;
    @Autowired GenerationRecordService recordService;
    @MockBean LlmGenerateService llmGenerateService;

    @Test
    void successfulExternalResultPersistsItsOwnActualExecutionMetadata() {
        when(llmGenerateService.generate(any())).thenReturn(new ProviderResult(
                "synthetic external result", "openai-compatible", "local-rule-mvp", 1, 2, 3, false, null
        ));

        AiGenerateRequest request = new AiGenerateRequest();
        request.setProjectId(1L);
        request.setInput("offline external success persistence test");

        AiGenerateResponse response = aiGenerateService.generate("requirement-split", request);
        GenerationRecord record = recordService.getById(response.getRecordId());

        assertThat(record.getRequestedProvider()).isEqualTo("openai-compatible");
        assertThat(record.getProviderName()).isEqualTo("openai-compatible");
        assertThat(record.getRequestedModel()).isEqualTo(record.getModelName());
        assertThat(record.getFallbackUsed()).isFalse();
        assertThat(record.getFallbackReason()).isNull();
    }
}
