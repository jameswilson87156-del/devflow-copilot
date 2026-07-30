package com.devflow.copilot;

import com.devflow.copilot.common.ProviderErrorType;
import com.devflow.copilot.dto.AiGenerateRequest;
import com.devflow.copilot.dto.AiGenerateResponse;
import com.devflow.copilot.entity.GenerationRecord;
import com.devflow.copilot.entity.GenerationTrace;
import com.devflow.copilot.service.AgentWorkflowService;
import com.devflow.copilot.service.AiGenerateService;
import com.devflow.copilot.service.GenerationRecordService;
import com.devflow.copilot.service.GenerationTraceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"devflow.ai.provider=openai-compatible", "devflow.ai.api-key=", "devflow.ai.protocol=chat-completions-compatible", "devflow.ai.fallback-to-local=true"})
@ActiveProfiles("test")
@Transactional
class ProviderExecutionFallbackPersistenceIntegrationTest {

    @Autowired AiGenerateService aiGenerateService;
    @Autowired GenerationRecordService recordService;
    @Autowired GenerationTraceService traceService;
    @Autowired AgentWorkflowService agentWorkflowService;

    @Test
    void classifiedExternalFailureFallsBackOnceAndPersistsRequestedAndActualFacts() {
        AiGenerateResponse response = aiGenerateService.generate("requirement-split", request());
        GenerationRecord record = recordService.getById(response.getRecordId());
        GenerationTrace trace = traceService.list(record.getId()).get(0);

        assertThat(record.getRequestedProvider()).isEqualTo("openai-compatible");
        assertThat(record.getRequestedModel()).isNotBlank();
        assertThat(record.getProviderName()).isEqualTo("local-rule");
        assertThat(record.getModelName()).isEqualTo("local-rule-mvp");
        assertThat(record.getFallbackUsed()).isTrue();
        assertThat(record.getFallbackReason()).isEqualTo(ProviderErrorType.API_KEY_MISSING.name());
        assertThat(record.getErrorMessage()).isNull();
        assertThat(trace.getProviderName()).isEqualTo(record.getProviderName());
        assertThat(trace.getModelName()).isEqualTo(record.getModelName());
        assertThat(agentWorkflowService.getTrace(response.getAgentRunId()).getRun().getProviderName())
                .isEqualTo(record.getProviderName());
    }

    private AiGenerateRequest request() {
        AiGenerateRequest request = new AiGenerateRequest();
        request.setProjectId(1L);
        request.setInput("offline fallback persistence test");
        return request;
    }
}
