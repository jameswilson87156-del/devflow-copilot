package com.devflow.copilot.service;

import com.devflow.copilot.dto.LogAnalyzeRequest;
import com.devflow.copilot.dto.LogAnalyzeResponse;
import com.devflow.copilot.entity.LogAnalysis;

import java.util.List;

public interface LogDiagnosisService {

    LogAnalyzeResponse analyze(LogAnalyzeRequest request);

    List<LogAnalysis> history(Long projectId);
}
