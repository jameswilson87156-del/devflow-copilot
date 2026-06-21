package com.devflow.copilot.controller;

import com.devflow.copilot.common.ApiResponse;
import com.devflow.copilot.entity.AiTask;
import com.devflow.copilot.service.AiTaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class AiTaskController {

    private final AiTaskService aiTaskService;

    public AiTaskController(AiTaskService aiTaskService) {
        this.aiTaskService = aiTaskService;
    }

    @GetMapping
    public ApiResponse<List<AiTask>> list(@RequestParam(required = false) Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId 不能为空");
        }
        return ApiResponse.ok(aiTaskService.listByProjectId(projectId));
    }
}
