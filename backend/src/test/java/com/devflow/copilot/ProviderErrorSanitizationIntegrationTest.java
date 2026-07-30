package com.devflow.copilot;

import com.devflow.copilot.common.LlmProviderException;
import com.devflow.copilot.common.ProviderContentTypeCategory;
import com.devflow.copilot.common.ProviderDurationBucket;
import com.devflow.copilot.common.ProviderErrorType;
import com.devflow.copilot.common.ProviderFailureMetadata;
import com.devflow.copilot.common.ProviderFailureStage;
import com.devflow.copilot.common.ProviderHttpStatusFamily;
import com.devflow.copilot.common.ProviderResponseSizeBucket;
import com.devflow.copilot.dto.AgentRunTraceResponse;
import com.devflow.copilot.dto.AiGenerateRequest;
import com.devflow.copilot.entity.GenerationRecord;
import com.devflow.copilot.entity.GenerationTrace;
import com.devflow.copilot.service.AgentWorkflowService;
import com.devflow.copilot.service.AiGenerateService;
import com.devflow.copilot.service.GenerationRecordService;
import com.devflow.copilot.service.GenerationTraceService;
import com.devflow.copilot.service.LlmGenerateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {"devflow.ai.provider=local-rule", "devflow.ai.api-key=", "devflow.ai.protocol=chat-completions-compatible", "devflow.ai.fallback-to-local=true"})
@ActiveProfiles("test")
@Transactional
class ProviderErrorSanitizationIntegrationTest {

    @Autowired AiGenerateService aiGenerateService;
    @Autowired GenerationRecordService recordService;
    @Autowired GenerationTraceService traceService;
    @Autowired AgentWorkflowService agentWorkflowService;

    @MockBean LlmGenerateService llmGenerateService;

    @Test
    void persistsOnlyClassifiedSafeErrorsForSyntheticUpstreamFailure() {
        String authorization = "Authorization" + ": " + "Bearer " + "synthetic-secret-token";
        String rawFailure = authorization + " " + "https" + "://synthetic-provider.invalid/v1 "
                + "<html>synthetic upstream failure</html>";
        when(llmGenerateService.generate(any()))
                .thenThrow(new LlmProviderException(ProviderErrorType.UPSTREAM_SERVER_ERROR,
                        metadata(502, ProviderHttpStatusFamily.FIVE_XX, true, true), new RuntimeException(rawFailure)));

        AiGenerateRequest request = new AiGenerateRequest();
        request.setProjectId(1L);
        request.setInput("offline provider safety test");

        assertThatThrownBy(() -> aiGenerateService.generate("requirement-split", request))
                .isInstanceOf(LlmProviderException.class)
                .hasMessageContaining(ProviderErrorType.UPSTREAM_SERVER_ERROR.name())
                .satisfies(error -> assertThat(error.getMessage()).doesNotContain("synthetic-secret-token"));

        GenerationRecord record = recordService.recent(1).get(0);
        List<GenerationTrace> traces = traceService.list(record.getId());
        AgentRunTraceResponse runTrace = agentWorkflowService.getTrace(
                agentWorkflowService.list(null, record.getId()).get(0).getId());

        assertSafe(record.getErrorMessage());
        assertThat(record.getRequestedProvider()).isNotBlank();
        assertThat(record.getRequestedModel()).isNotBlank();
        assertThat(record.getProviderName()).isNull();
        assertThat(record.getModelName()).isNull();
        assertThat(record.getFallbackUsed()).isFalse();
        assertThat(record.getFallbackReason()).isNull();
        assertThat(record.getProviderErrorType()).isEqualTo(ProviderErrorType.UPSTREAM_SERVER_ERROR.name());
        assertThat(record.getProviderFailureStage()).isEqualTo(ProviderFailureStage.HTTP_STATUS_RECEIVED.name());
        assertThat(record.getProviderHttpStatus()).isEqualTo(502);
        assertThat(record.getProviderHttpStatusFamily()).isEqualTo(ProviderHttpStatusFamily.FIVE_XX.value());
        assertThat(record.getProviderResponseBodyPresent()).isTrue();
        assertThat(record.getProviderResponseSizeBucket()).isEqualTo(ProviderResponseSizeBucket.ONE_TO_1KB.name());
        assertThat(record.getProviderContentTypeCategory()).isEqualTo(ProviderContentTypeCategory.HTML.name());
        assertThat(record.getProviderRetryAfterPresent()).isTrue();
        assertThat(record.getProviderRequestIdPresent()).isTrue();
        assertThat(record.getProviderClientRequestId()).matches("[0-9a-f-]{36}");
        assertSafe(traces.get(0).getErrorMessage());
        assertThat(traces.get(0).getProviderName()).isNull();
        assertThat(traces.get(0).getModelName()).isNull();
        assertThat(runTrace.getRun().getProviderName()).isNull();
        assertThat(runTrace.getRun().getModelName()).isNull();
        runTrace.getSteps().stream()
                .filter(step -> "FAILED".equals(step.getStatus()))
                .forEach(step -> assertSafe(step.getSummary()));
        runTrace.getToolCalls().stream()
                .filter(toolCall -> "FAILED".equals(toolCall.getStatus()))
                .forEach(toolCall -> assertSafe(toolCall.getOutputSummary()));
    }

    @Test
    void persists503AndKeeps429SeparateFromUpstreamServerErrors() {
        when(llmGenerateService.generate(any()))
                .thenThrow(new LlmProviderException(ProviderErrorType.UPSTREAM_SERVER_ERROR,
                        metadata(503, ProviderHttpStatusFamily.FIVE_XX, false, false)));

        assertThatThrownBy(() -> aiGenerateService.generate("requirement-split", request()))
                .isInstanceOf(LlmProviderException.class);
        GenerationRecord upstream = recordService.recent(1).get(0);
        assertThat(upstream.getProviderHttpStatus()).isEqualTo(503);
        assertThat(upstream.getProviderErrorType()).isEqualTo(ProviderErrorType.UPSTREAM_SERVER_ERROR.name());

        doThrow(new LlmProviderException(ProviderErrorType.RATE_LIMITED,
                metadata(429, ProviderHttpStatusFamily.FOUR_XX, false, false)))
                .when(llmGenerateService).generate(any());

        assertThatThrownBy(() -> aiGenerateService.generate("requirement-split", request()))
                .isInstanceOf(LlmProviderException.class);
        GenerationRecord rateLimited = recordService.recent(1).get(0);
        assertThat(rateLimited.getProviderHttpStatus()).isEqualTo(429);
        assertThat(rateLimited.getProviderErrorType()).isEqualTo(ProviderErrorType.RATE_LIMITED.name());
        assertThat(rateLimited.getProviderErrorType()).isNotEqualTo(ProviderErrorType.UPSTREAM_SERVER_ERROR.name());
    }

    private AiGenerateRequest request() {
        AiGenerateRequest request = new AiGenerateRequest();
        request.setProjectId(1L);
        request.setInput("offline metadata persistence test");
        return request;
    }

    private ProviderFailureMetadata metadata(int status, ProviderHttpStatusFamily family,
                                             boolean retryAfterPresent, boolean upstreamRequestIdPresent) {
        return new ProviderFailureMetadata(
                ProviderFailureStage.HTTP_STATUS_RECEIVED,
                status,
                family,
                true,
                ProviderResponseSizeBucket.ONE_TO_1KB,
                true,
                ProviderContentTypeCategory.HTML,
                retryAfterPresent,
                upstreamRequestIdPresent,
                "00000000-0000-4000-8000-000000000001",
                ProviderDurationBucket.UNDER_1_SECOND
        );
    }

    private void assertSafe(String value) {
        assertThat(value)
                .contains(ProviderErrorType.UPSTREAM_SERVER_ERROR.name())
                .doesNotContain("synthetic-secret-token", "synthetic-provider.invalid", "synthetic upstream failure");
    }
}
