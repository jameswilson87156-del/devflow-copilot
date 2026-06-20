package com.devflow.copilot.service.impl;

import com.devflow.copilot.common.GenerationStatus;
import com.devflow.copilot.common.LlmProviderException;
import com.devflow.copilot.config.AiProviderProperties;
import com.devflow.copilot.dto.AiGenerateRequest;
import com.devflow.copilot.dto.AiGenerateResponse;
import com.devflow.copilot.dto.RenderedPrompt;
import com.devflow.copilot.entity.GenerationRecord;
import com.devflow.copilot.entity.ProjectContext;
import com.devflow.copilot.service.AiGenerateService;
import com.devflow.copilot.service.GenerationRecordService;
import com.devflow.copilot.service.LlmGenerateService;
import com.devflow.copilot.service.ProjectContextService;
import com.devflow.copilot.service.PromptTemplateRenderService;
import com.devflow.copilot.service.provider.ProviderRequest;
import com.devflow.copilot.service.provider.ProviderResult;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LocalRuleGenerateService implements AiGenerateService {

    private final ProjectContextService projectService;
    private final GenerationRecordService recordService;
    private final PromptTemplateRenderService promptRenderService;
    private final LlmGenerateService llmGenerateService;
    private final AiProviderProperties providerProperties;

    public LocalRuleGenerateService(
            ProjectContextService projectService,
            GenerationRecordService recordService,
            PromptTemplateRenderService promptRenderService,
            LlmGenerateService llmGenerateService,
            AiProviderProperties providerProperties
    ) {
        this.projectService = projectService;
        this.recordService = recordService;
        this.promptRenderService = promptRenderService;
        this.llmGenerateService = llmGenerateService;
        this.providerProperties = providerProperties;
    }

    @Override
    public AiGenerateResponse generate(String generationType, AiGenerateRequest request) {
        ProjectContext project = resolveProject(request.getProjectId());
        RenderedPrompt rendered = promptRenderService.render(generationType, request, project);
        GenerationRecord record = createGeneratingRecord(generationType, request, rendered, project);
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
            GenerationRecord completed = recordService.transition(record.getId(), GenerationStatus.READY_FOR_REVIEW);
            return toResponse(completed);
        } catch (RuntimeException ex) {
            record.setProviderName(providerProperties.getProvider());
            record.setModelName(providerProperties.getModel());
            record.setCostTimeMs(System.currentTimeMillis() - start);
            record.setSuccess(false);
            record.setErrorMessage(safeMessage(ex));
            recordService.save(record);
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

    private AiGenerateResponse toResponse(GenerationRecord record) {
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

    private String safeMessage(RuntimeException ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? ex.getClass().getSimpleName() : ex.getMessage();
    }
}
