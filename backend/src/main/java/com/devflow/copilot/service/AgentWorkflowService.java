package com.devflow.copilot.service;

import com.devflow.copilot.common.GenerationStatus;
import com.devflow.copilot.dto.AgentRunTraceResponse;
import com.devflow.copilot.entity.AgentRun;
import com.devflow.copilot.entity.AgentStep;
import com.devflow.copilot.entity.GenerationRecord;
import com.devflow.copilot.entity.ProjectContext;
import com.devflow.copilot.entity.ToolCallRecord;

import java.util.List;

public interface AgentWorkflowService {

    AgentRun startRun(ProjectContext project, GenerationRecord record, String title, String goal);

    AgentStep addStep(Long runId, int order, String type, String name, String status, String summary, Long latencyMs);

    ToolCallRecord addToolCall(Long runId, Long stepId, String toolName, String inputSummary, String outputSummary,
                               String status, Long latencyMs);

    void createPendingReview(Long runId, Long generationRecordId);

    void syncGenerationTransition(Long generationRecordId, GenerationStatus target);

    List<AgentRun> list(Long projectId, Long generationRecordId);

    AgentRunTraceResponse getTrace(Long runId);
}
