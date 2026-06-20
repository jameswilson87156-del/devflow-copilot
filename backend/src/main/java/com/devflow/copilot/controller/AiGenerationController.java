package com.devflow.copilot.controller;

import com.devflow.copilot.common.ApiResponse;
import com.devflow.copilot.common.GenerationType;
import com.devflow.copilot.dto.AiGenerateRequest;
import com.devflow.copilot.dto.AiGenerateResponse;
import com.devflow.copilot.service.AiGenerateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiGenerationController {

    private final AiGenerateService aiGenerateService;

    public AiGenerationController(AiGenerateService aiGenerateService) {
        this.aiGenerateService = aiGenerateService;
    }

    @PostMapping("/requirement-split")
    public ApiResponse<AiGenerateResponse> requirementSplit(@Valid @RequestBody AiGenerateRequest request) {
        return ApiResponse.ok(aiGenerateService.generate(GenerationType.REQUIREMENT_SPLIT, request));
    }

    @PostMapping("/code-plan")
    public ApiResponse<AiGenerateResponse> codePlan(@Valid @RequestBody AiGenerateRequest request) {
        return ApiResponse.ok(aiGenerateService.generate(GenerationType.CODE_PLAN, request));
    }

    @PostMapping("/readme-generate")
    public ApiResponse<AiGenerateResponse> readmeGenerate(@Valid @RequestBody AiGenerateRequest request) {
        return ApiResponse.ok(aiGenerateService.generate(GenerationType.README_GENERATE, request));
    }

    @PostMapping("/commit-message")
    public ApiResponse<AiGenerateResponse> commitMessage(@Valid @RequestBody AiGenerateRequest request) {
        return ApiResponse.ok(aiGenerateService.generate(GenerationType.COMMIT_MESSAGE, request));
    }

    @PostMapping("/fix-prompt")
    public ApiResponse<AiGenerateResponse> fixPrompt(@Valid @RequestBody AiGenerateRequest request) {
        return ApiResponse.ok(aiGenerateService.generate(GenerationType.FIX_PROMPT, request));
    }
}
