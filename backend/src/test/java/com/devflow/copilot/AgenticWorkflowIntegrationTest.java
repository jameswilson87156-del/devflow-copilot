package com.devflow.copilot;

import com.devflow.copilot.dto.AgentRunTraceResponse;
import com.devflow.copilot.dto.AiGenerateRequest;
import com.devflow.copilot.dto.AiGenerateResponse;
import com.devflow.copilot.dto.KnowledgeDocumentRequest;
import com.devflow.copilot.dto.KnowledgeReferenceResponse;
import com.devflow.copilot.dto.KnowledgeSearchRequest;
import com.devflow.copilot.entity.AgentStep;
import com.devflow.copilot.entity.GenerationRecord;
import com.devflow.copilot.entity.GenerationTrace;
import com.devflow.copilot.entity.HumanReview;
import com.devflow.copilot.entity.KnowledgeDocument;
import com.devflow.copilot.service.AgentWorkflowService;
import com.devflow.copilot.service.AiGenerateService;
import com.devflow.copilot.service.GenerationRecordService;
import com.devflow.copilot.service.GenerationTraceService;
import com.devflow.copilot.service.KnowledgeBaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AgenticWorkflowIntegrationTest {

    @Autowired AiGenerateService aiGenerateService;
    @Autowired GenerationRecordService recordService;
    @Autowired GenerationTraceService traceService;
    @Autowired AgentWorkflowService agentWorkflowService;
    @Autowired KnowledgeBaseService knowledgeBaseService;

    @Test
    void generationCreatesTraceAgentRunToolCallHumanReviewAndKnowledgeReferences() {
        AiGenerateRequest request = new AiGenerateRequest();
        request.setProjectId(1L);
        request.setInput("请解释 OpenAI-compatible Provider 没有 API Key 时如何降级到 local-rule。");
        request.setExtraContext("保持 mock/local-rule 演示能力。");
        request.setKnowledgeQuery("OpenAI-compatible API Key local-rule Provider");

        AiGenerateResponse response = aiGenerateService.generate("requirement-split", request);

        assertThat(response.getAgentRunId()).isNotNull();
        assertThat(response.getKnowledgeReferences()).isNotEmpty();

        GenerationRecord record = recordService.getById(response.getRecordId());
        assertThat(record.getStatus().name()).isEqualTo("READY_FOR_REVIEW");
        assertThat(record.getRenderedPrompt()).contains("Knowledge Base 引用");

        List<GenerationTrace> traces = traceService.list(response.getRecordId());
        assertThat(traces).hasSize(1);
        assertThat(traces.get(0).getProviderName()).isEqualTo("local-rule");
        assertThat(traces.get(0).getStatus()).isEqualTo("READY_FOR_REVIEW");
        assertThat(traces.get(0).getInputVariables()).doesNotContain("DEVFLOW_AI_API_KEY");

        AgentRunTraceResponse runTrace = agentWorkflowService.getTrace(response.getAgentRunId());
        assertThat(runTrace.getRun().getStatus()).isEqualTo("WAITING_REVIEW");
        assertThat(runTrace.getSteps()).extracting(AgentStep::getStepType)
                .contains("TASK_DECOMPOSITION", "PROMPT_RENDER", "KNOWLEDGE_RETRIEVAL", "LLM_GENERATION", "HUMAN_REVIEW");
        assertThat(runTrace.getToolCalls()).extracting("toolName")
                .contains("prompt-template-render", "keyword-knowledge-search", "generation-provider");
        assertThat(runTrace.getHumanReviews()).extracting(HumanReview::getReviewStatus)
                .contains("PENDING");

        recordService.saveStatus(response.getRecordId());
        recordService.confirm(response.getRecordId());

        AgentRunTraceResponse confirmedTrace = agentWorkflowService.getTrace(response.getAgentRunId());
        assertThat(confirmedTrace.getRun().getStatus()).isEqualTo("CONFIRMED");
        assertThat(confirmedTrace.getHumanReviews()).extracting(HumanReview::getReviewStatus)
                .contains("CONFIRMED");
        assertThat(knowledgeBaseService.listReferences(response.getRecordId())).isNotEmpty();
    }

    @Test
    void knowledgeDocumentCanBeCreatedChunkedAndSearched() {
        KnowledgeDocumentRequest request = new KnowledgeDocumentRequest();
        request.setTitle("退款幂等设计说明");
        request.setSourceType("manual");
        request.setSourceUri("docs/refund-idempotency.md");
        request.setContent("""
                退款回调可能因为上游重试重复到达，系统需要使用 refundRequestId 做幂等键。

                后端实现时应在数据库增加唯一约束，并在 Service 层捕获唯一键冲突后返回已存在结果。
                测试需要覆盖并发回调和重复请求两种场景。
                """);

        KnowledgeDocument document = knowledgeBaseService.createDocument(request);
        assertThat(document.getChunkCount()).isPositive();
        assertThat(knowledgeBaseService.listChunks(document.getId())).isNotEmpty();

        KnowledgeSearchRequest searchRequest = new KnowledgeSearchRequest();
        searchRequest.setQuery("退款 幂等 唯一约束");
        searchRequest.setTopK(3);

        List<KnowledgeReferenceResponse> results = knowledgeBaseService.search(searchRequest);
        assertThat(results).extracting(KnowledgeReferenceResponse::getDocumentId)
                .contains(document.getId());
        assertThat(results.get(0).getSnippet()).contains("退款");
    }
}
