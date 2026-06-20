package com.devflow.copilot.controller;

import com.devflow.copilot.common.ApiResponse;
import com.devflow.copilot.dto.RecordQuery;
import com.devflow.copilot.entity.GenerationRecord;
import com.devflow.copilot.service.GenerationRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/generations")
public class GenerationHistoryController {

    private final GenerationRecordService generationRecordService;

    public GenerationHistoryController(GenerationRecordService generationRecordService) {
        this.generationRecordService = generationRecordService;
    }

    @GetMapping
    public ApiResponse<List<GenerationRecord>> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String generationType
    ) {
        RecordQuery query = new RecordQuery();
        query.setProjectId(projectId);
        query.setGenerationType(generationType);
        return ApiResponse.ok(generationRecordService.list(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<GenerationRecord> get(@PathVariable Long id) {
        return ApiResponse.ok(generationRecordService.getById(id));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<GenerationRecord> confirm(@PathVariable Long id) {
        return ApiResponse.ok(generationRecordService.confirm(id));
    }

    @PostMapping("/{id}/save")
    public ApiResponse<GenerationRecord> save(@PathVariable Long id) {
        return ApiResponse.ok(generationRecordService.saveStatus(id));
    }
}
