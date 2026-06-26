package com.devflow.copilot.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiGenerateResponse {

    private Long recordId;
    private String generationType;
    private String outputContent;
    private String status;
    private String providerName;
    private String modelName;
    private Long costTimeMs;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Long promptTemplateId;
    private String promptTemplateName;
    private Integer promptTemplateVersion;
    private String errorMessage;
    private Long agentRunId;
    private List<KnowledgeReferenceResponse> knowledgeReferences;
}
