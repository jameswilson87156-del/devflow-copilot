package com.devflow.copilot.service.impl;

import com.devflow.copilot.dto.DashboardStatsResponse;
import com.devflow.copilot.mapper.LogAnalysisMapper;
import com.devflow.copilot.mapper.ProjectContextMapper;
import com.devflow.copilot.mapper.PromptTemplateMapper;
import com.devflow.copilot.service.DashboardService;
import com.devflow.copilot.service.GenerationRecordService;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final ProjectContextMapper projectMapper;
    private final PromptTemplateMapper promptMapper;
    private final LogAnalysisMapper logMapper;
    private final GenerationRecordService recordService;

    public DashboardServiceImpl(
            ProjectContextMapper projectMapper,
            PromptTemplateMapper promptMapper,
            LogAnalysisMapper logMapper,
            GenerationRecordService recordService
    ) {
        this.projectMapper = projectMapper;
        this.promptMapper = promptMapper;
        this.logMapper = logMapper;
        this.recordService = recordService;
    }

    @Override
    public DashboardStatsResponse stats() {
        return DashboardStatsResponse.builder()
                .projectCount(projectMapper.selectCount(null))
                .todayGenerationCount(recordService.todayCount())
                .logAnalysisCount(logMapper.selectCount(null))
                .promptTemplateCount(promptMapper.selectCount(null))
                .recentGenerations(recordService.recent(6))
                .build();
    }
}
