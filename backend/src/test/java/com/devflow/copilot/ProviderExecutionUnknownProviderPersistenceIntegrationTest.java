package com.devflow.copilot;

import com.devflow.copilot.common.LlmProviderException;
import com.devflow.copilot.dto.AiGenerateRequest;
import com.devflow.copilot.entity.GenerationRecord;
import com.devflow.copilot.service.AiGenerateService;
import com.devflow.copilot.service.GenerationRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"devflow.ai.provider=unsupported-test-provider", "devflow.ai.api-key=", "devflow.ai.protocol=chat-completions-compatible", "devflow.ai.fallback-to-local=true"})
@ActiveProfiles("test")
@Transactional
class ProviderExecutionUnknownProviderPersistenceIntegrationTest {

    @Autowired AiGenerateService aiGenerateService;
    @Autowired GenerationRecordService recordService;

    @Test
    void unknownProviderDoesNotCreateFallbackOrActualExecutionFacts() {
        AiGenerateRequest request = new AiGenerateRequest();
        request.setProjectId(1L);
        request.setInput("offline unknown provider persistence test");

        assertThatThrownBy(() -> aiGenerateService.generate("requirement-split", request))
                .isInstanceOf(LlmProviderException.class);

        GenerationRecord record = recordService.recent(1).get(0);
        assertThat(record.getStatus().name()).isEqualTo("FAILED");
        assertThat(record.getRequestedProvider()).isEqualTo("unsupported-test-provider");
        assertThat(record.getProviderName()).isNull();
        assertThat(record.getModelName()).isNull();
        assertThat(record.getFallbackUsed()).isFalse();
        assertThat(record.getFallbackReason()).isNull();
    }
}
