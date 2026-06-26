package com.devflow.copilot.dto;

import com.devflow.copilot.entity.AgentRun;
import com.devflow.copilot.entity.AgentStep;
import com.devflow.copilot.entity.HumanReview;
import com.devflow.copilot.entity.ToolCallRecord;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AgentRunTraceResponse {

    private AgentRun run;
    private List<AgentStep> steps;
    private List<ToolCallRecord> toolCalls;
    private List<HumanReview> humanReviews;
}
