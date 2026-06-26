package com.devflow.copilot.service.impl;

import com.devflow.copilot.dto.DashboardStatsResponse;
import com.devflow.copilot.entity.GenerationRecord;
import com.devflow.copilot.mapper.AgentRunMapper;
import com.devflow.copilot.mapper.GenerationRecordMapper;
import com.devflow.copilot.mapper.HumanReviewMapper;
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
    private final GenerationRecordMapper generationRecordMapper;
    private final AgentRunMapper agentRunMapper;
    private final HumanReviewMapper humanReviewMapper;
    private final GenerationRecordService recordService;

    public DashboardServiceImpl(
            ProjectContextMapper projectMapper,
            PromptTemplateMapper promptMapper,
            LogAnalysisMapper logMapper,
            GenerationRecordMapper generationRecordMapper,
            AgentRunMapper agentRunMapper,
            HumanReviewMapper humanReviewMapper,
            GenerationRecordService recordService
    ) {
        this.projectMapper = projectMapper;
        this.promptMapper = promptMapper;
        this.logMapper = logMapper;
        this.generationRecordMapper = generationRecordMapper;
        this.agentRunMapper = agentRunMapper;
        this.humanReviewMapper = humanReviewMapper;
        this.recordService = recordService;
    }

    @Override
    public DashboardStatsResponse stats() {
        long totalGenerations = generationRecordMapper.selectCount(null);
        long successCount = generationRecordMapper.selectList(null).stream()
                .filter(record -> Boolean.TRUE.equals(record.getSuccess()))
                .count();
        long averageLatency = Math.round(generationRecordMapper.selectList(null).stream()
                .map(GenerationRecord::getCostTimeMs)
                .filter(value -> value != null && value >= 0)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0));
        return DashboardStatsResponse.builder()
                .projectCount(projectMapper.selectCount(null))
                .todayGenerationCount(recordService.todayCount())
                .logAnalysisCount(logMapper.selectCount(null))
                .promptTemplateCount(promptMapper.selectCount(null))
                .agentRunCount(agentRunMapper.selectCount(null))
                .humanReviewCount(humanReviewMapper.selectCount(null))
                .successCount(successCount)
                .successRate(totalGenerations == 0 ? 0 : Math.round((successCount * 10000.0) / totalGenerations) / 100.0)
                .averageLatencyMs(averageLatency)
                .recentGenerations(recordService.recent(6))
                .build();
    }
}
