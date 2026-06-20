package com.devflow.copilot.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogAnalyzeResponse {

    private Long id;
    private Long recordId;
    private String exceptionType;
    private String possibleReason;
    private String diagnoseSteps;
    private String fixPrompt;
    private String riskTips;
    private String riskLevel;
}
