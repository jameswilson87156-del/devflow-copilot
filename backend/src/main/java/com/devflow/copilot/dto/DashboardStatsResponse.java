package com.devflow.copilot.dto;

import com.devflow.copilot.entity.GenerationRecord;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardStatsResponse {

    private long projectCount;
    private long todayGenerationCount;
    private long logAnalysisCount;
    private long promptTemplateCount;
    private List<GenerationRecord> recentGenerations;
}
