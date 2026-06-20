package com.devflow.copilot.controller;

import com.devflow.copilot.common.ApiResponse;
import com.devflow.copilot.dto.LogAnalyzeRequest;
import com.devflow.copilot.dto.LogAnalyzeResponse;
import com.devflow.copilot.entity.LogAnalysis;
import com.devflow.copilot.service.LogDiagnosisService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class LogAnalysisController {

    private final LogDiagnosisService logDiagnosisService;

    public LogAnalysisController(LogDiagnosisService logDiagnosisService) {
        this.logDiagnosisService = logDiagnosisService;
    }

    @PostMapping("/analyze")
    public ApiResponse<LogAnalyzeResponse> analyze(@Valid @RequestBody LogAnalyzeRequest request) {
        return ApiResponse.ok(logDiagnosisService.analyze(request));
    }

    @GetMapping("/history")
    public ApiResponse<List<LogAnalysis>> history(@RequestParam(required = false) Long projectId) {
        return ApiResponse.ok(logDiagnosisService.history(projectId));
    }
}
