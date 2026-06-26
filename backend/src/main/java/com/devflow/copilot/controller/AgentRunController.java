package com.devflow.copilot.controller;

import com.devflow.copilot.common.ApiResponse;
import com.devflow.copilot.dto.AgentRunTraceResponse;
import com.devflow.copilot.entity.AgentRun;
import com.devflow.copilot.service.AgentWorkflowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent-runs")
public class AgentRunController {

    private final AgentWorkflowService agentWorkflowService;

    public AgentRunController(AgentWorkflowService agentWorkflowService) {
        this.agentWorkflowService = agentWorkflowService;
    }

    @GetMapping
    public ApiResponse<List<AgentRun>> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long generationRecordId
    ) {
        return ApiResponse.ok(agentWorkflowService.list(projectId, generationRecordId));
    }

    @GetMapping("/{id}/trace")
    public ApiResponse<AgentRunTraceResponse> trace(@PathVariable Long id) {
        return ApiResponse.ok(agentWorkflowService.getTrace(id));
    }
}
