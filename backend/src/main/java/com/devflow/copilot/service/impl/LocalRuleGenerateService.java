package com.devflow.copilot.service.impl;

import com.devflow.copilot.common.GenerationStatus;
import com.devflow.copilot.common.LlmProviderException;
import com.devflow.copilot.config.AiProviderProperties;
import com.devflow.copilot.dto.AiGenerateRequest;
import com.devflow.copilot.dto.AiGenerateResponse;
import com.devflow.copilot.dto.KnowledgeReferenceResponse;
import com.devflow.copilot.dto.RenderedPrompt;
import com.devflow.copilot.entity.AgentRun;
import com.devflow.copilot.entity.AgentStep;
import com.devflow.copilot.entity.GenerationRecord;
import com.devflow.copilot.entity.ProjectContext;
import com.devflow.copilot.service.AgentWorkflowService;
import com.devflow.copilot.service.AiGenerateService;
import com.devflow.copilot.service.GenerationRecordService;
import com.devflow.copilot.service.GenerationTraceService;
import com.devflow.copilot.service.KnowledgeBaseService;
import com.devflow.copilot.service.LlmGenerateService;
import com.devflow.copilot.service.ProjectContextService;
import com.devflow.copilot.service.PromptTemplateRenderService;
import com.devflow.copilot.service.provider.ProviderRequest;
import com.devflow.copilot.service.provider.ProviderResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LocalRuleGenerateService implements AiGenerateService {

    private final ProjectContextService projectService;
    private final GenerationRecordService recordService;
    private final PromptTemplateRenderService promptRenderService;
    private final LlmGenerateService llmGenerateService;
    private final AiProviderProperties providerProperties;
    private final GenerationTraceService traceService;
    private final AgentWorkflowService agentWorkflowService;
    private final KnowledgeBaseService knowledgeBaseService;

    public LocalRuleGenerateService(
            ProjectContextService projectService,
            GenerationRecordService recordService,
            PromptTemplateRenderService promptRenderService,
            LlmGenerateService llmGenerateService,
            AiProviderProperties providerProperties,
            GenerationTraceService traceService,
            AgentWorkflowService agentWorkflowService,
            KnowledgeBaseService knowledgeBaseService
    ) {
        this.projectService = projectService;
        this.recordService = recordService;
        this.promptRenderService = promptRenderService;
        this.llmGenerateService = llmGenerateService;
        this.providerProperties = providerProperties;
        this.traceService = traceService;
        this.agentWorkflowService = agentWorkflowService;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @Override
    public AiGenerateResponse generate(String generationType, AiGenerateRequest request) {
        ProjectContext project = resolveProject(request.getProjectId());
        List<KnowledgeReferenceResponse> references = knowledgeBaseService.searchForGeneration(
                knowledgeQuery(request),
                request.getKnowledgeDocumentIds(),
                3
        );
        String originalExtraContext = request.getExtraContext();
        request.setExtraContext(mergeKnowledgeContext(originalExtraContext, references));
        long renderStart = System.currentTimeMillis();
        RenderedPrompt rendered = promptRenderService.render(generationType, request, project);
        long renderMs = System.currentTimeMillis() - renderStart;
        GenerationRecord record = createGeneratingRecord(generationType, request, rendered, project);
        AgentRun run = createRun(project, record, request);
        agentWorkflowService.addStep(run.getId(), 1, "TASK_DECOMPOSITION", "任务接收", "SUCCESS",
                "接收生成请求并绑定项目上下文：" + project.getProjectName(), 0L);
        AgentStep renderStep = agentWorkflowService.addStep(run.getId(), 2, "PROMPT_RENDER", "Prompt 模板渲染", "SUCCESS",
                "使用模板 " + rendered.templateName() + " v" + rendered.templateVersion(), renderMs);
        agentWorkflowService.addToolCall(run.getId(), renderStep.getId(), "prompt-template-render",
                generationType + " / templateId=" + rendered.templateId(),
                summary(rendered.content()), "SUCCESS", renderMs);
        if (!references.isEmpty()) {
            AgentStep retrievalStep = agentWorkflowService.addStep(run.getId(), 3, "KNOWLEDGE_RETRIEVAL", "Knowledge Base 检索",
                    "SUCCESS", "命中 " + references.size() + " 个知识 chunk，并注入 Prompt 上下文。", 0L);
            agentWorkflowService.addToolCall(run.getId(), retrievalStep.getId(), "keyword-knowledge-search",
                    knowledgeQuery(request), knowledgeSummary(references), "SUCCESS", 0L);
        }
        long start = System.currentTimeMillis();
        try {
            ProviderResult result = llmGenerateService.generate(new ProviderRequest(
                    generationType,
                    rendered.content(),
                    request.getInput(),
                    project.getProjectName(),
                    project.getTechStack()
            ));
            record.setOutputContent(result.content());
            record.setProviderName(result.providerName());
            record.setModelName(result.modelName());
            record.setPromptTokens(result.promptTokens());
            record.setCompletionTokens(result.completionTokens());
            record.setTotalTokens(result.totalTokens());
            record.setCostTimeMs(System.currentTimeMillis() - start);
            record.setSuccess(true);
            record.setErrorMessage(result.fallbackReason());
            recordService.save(record);
            List<KnowledgeReferenceResponse> attachedReferences = knowledgeBaseService.attachReferences(record.getId(), references);
            AgentStep generationStep = agentWorkflowService.addStep(run.getId(), 4, "LLM_GENERATION", "Provider 生成", "SUCCESS",
                    "Provider 返回 Artifact，状态进入待人工确认。", record.getCostTimeMs());
            agentWorkflowService.addToolCall(run.getId(), generationStep.getId(), "generation-provider",
                    result.providerName() + " / " + result.modelName(),
                    result.fallbackReason() == null ? summary(result.content()) : "fallback: " + result.fallbackReason(),
                    "SUCCESS", record.getCostTimeMs());
            agentWorkflowService.addStep(run.getId(), 5, "HUMAN_REVIEW", "人工确认", "WAITING",
                    "生成结果等待开发者保存和确认。", 0L);
            agentWorkflowService.createPendingReview(run.getId(), record.getId());
            traceService.record(record, request, GenerationStatus.READY_FOR_REVIEW.name());
            GenerationRecord completed = recordService.transition(record.getId(), GenerationStatus.READY_FOR_REVIEW);
            return toResponse(completed, run.getId(), attachedReferences);
        } catch (RuntimeException ex) {
            record.setProviderName(providerProperties.getProvider());
            record.setModelName(providerProperties.getModel());
            record.setCostTimeMs(System.currentTimeMillis() - start);
            record.setSuccess(false);
            record.setErrorMessage(safeMessage(ex));
            recordService.save(record);
            AgentStep generationStep = agentWorkflowService.addStep(run.getId(), 4, "LLM_GENERATION", "Provider 生成", "FAILED",
                    "Provider 调用失败：" + safeMessage(ex), record.getCostTimeMs());
            agentWorkflowService.addToolCall(run.getId(), generationStep.getId(), "generation-provider",
                    providerProperties.getProvider() + " / " + providerProperties.getModel(),
                    safeMessage(ex), "FAILED", record.getCostTimeMs());
            traceService.record(record, request, GenerationStatus.FAILED.name());
            recordService.transition(record.getId(), GenerationStatus.FAILED);
            if (ex instanceof LlmProviderException llmProviderException) {
                throw llmProviderException;
            }
            throw ex;
        }
    }

    private GenerationRecord createGeneratingRecord(
            String generationType,
            AiGenerateRequest request,
            RenderedPrompt rendered,
            ProjectContext project
    ) {
        GenerationRecord record = new GenerationRecord();
        record.setProjectId(project.getId());
        record.setGenerationType(generationType);
        record.setInputSummary(summary(request.getInput()));
        record.setInputContent(request.getInput());
        record.setStatus(GenerationStatus.GENERATING);
        record.setConfirmed(false);
        record.setSuccess(false);
        record.setProviderName(providerProperties.getProvider());
        record.setModelName(providerProperties.getModel());
        record.setPromptTemplateId(rendered.templateId());
        record.setPromptTemplateName(rendered.templateName());
        record.setPromptTemplateVersion(rendered.templateVersion());
        record.setRenderedPrompt(rendered.content());
        return recordService.save(record);
    }

    private AgentRun createRun(ProjectContext project, GenerationRecord record, AiGenerateRequest request) {
        return agentWorkflowService.startRun(
                project,
                record,
                summary(request.getInput()),
                request.getInput()
        );
    }

    private AiGenerateResponse toResponse(GenerationRecord record, Long agentRunId, List<KnowledgeReferenceResponse> references) {
        return AiGenerateResponse.builder()
                .recordId(record.getId())
                .generationType(record.getGenerationType())
                .outputContent(record.getOutputContent())
                .status(record.getStatus().name())
                .providerName(record.getProviderName())
                .modelName(record.getModelName())
                .costTimeMs(record.getCostTimeMs())
                .promptTokens(record.getPromptTokens())
                .completionTokens(record.getCompletionTokens())
                .totalTokens(record.getTotalTokens())
                .promptTemplateId(record.getPromptTemplateId())
                .promptTemplateName(record.getPromptTemplateName())
                .promptTemplateVersion(record.getPromptTemplateVersion())
                .errorMessage(record.getErrorMessage())
                .agentRunId(agentRunId)
                .knowledgeReferences(references)
                .build();
    }

    private ProjectContext resolveProject(Long projectId) {
        if (projectId != null) {
            return projectService.getById(projectId);
        }
        return projectService.list().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("请先创建项目上下文"));
    }

    private String summary(String input) {
        String clean = Optional.ofNullable(input).orElse("").replaceAll("\\s+", " ").trim();
        return clean.length() > 60 ? clean.substring(0, 60) + "..." : clean;
    }

    private String knowledgeQuery(AiGenerateRequest request) {
        return Optional.ofNullable(request.getKnowledgeQuery())
                .filter(value -> !value.isBlank())
                .orElse(request.getInput());
    }

    private String mergeKnowledgeContext(String existingContext, List<KnowledgeReferenceResponse> references) {
        if (references.isEmpty()) {
            return existingContext;
        }
        StringBuilder builder = new StringBuilder(Optional.ofNullable(existingContext).orElse("").trim());
        if (!builder.isEmpty()) {
            builder.append("\n\n");
        }
        builder.append("Knowledge Base 引用：\n");
        for (KnowledgeReferenceResponse reference : references) {
            builder.append("- [")
                    .append(reference.getCitationLabel())
                    .append("] ")
                    .append(reference.getSnippet())
                    .append("\n");
        }
        return builder.toString();
    }

    private String knowledgeSummary(List<KnowledgeReferenceResponse> references) {
        return references.stream()
                .map(item -> item.getCitationLabel() + " score=" + item.getScore())
                .reduce((left, right) -> left + "; " + right)
                .orElse("未命中知识引用");
    }

    private String safeMessage(RuntimeException ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? ex.getClass().getSimpleName() : ex.getMessage();
    }
}
