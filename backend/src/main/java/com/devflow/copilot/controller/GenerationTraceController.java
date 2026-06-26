package com.devflow.copilot.controller;

import com.devflow.copilot.common.ApiResponse;
import com.devflow.copilot.entity.GenerationTrace;
import com.devflow.copilot.service.GenerationTraceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/generation-traces")
public class GenerationTraceController {

    private final GenerationTraceService generationTraceService;

    public GenerationTraceController(GenerationTraceService generationTraceService) {
        this.generationTraceService = generationTraceService;
    }

    @GetMapping
    public ApiResponse<List<GenerationTrace>> list(@RequestParam(required = false) Long generationRecordId) {
        return ApiResponse.ok(generationTraceService.list(generationRecordId));
    }

    @GetMapping("/{id}")
    public ApiResponse<GenerationTrace> get(@PathVariable Long id) {
        return ApiResponse.ok(generationTraceService.getById(id));
    }
}
