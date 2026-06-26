package com.devflow.copilot.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.devflow.copilot.common.BusinessException;
import com.devflow.copilot.common.GenerationStatus;
import com.devflow.copilot.dto.AgentRunTraceResponse;
import com.devflow.copilot.entity.AgentRun;
import com.devflow.copilot.entity.AgentStep;
import com.devflow.copilot.entity.GenerationRecord;
import com.devflow.copilot.entity.HumanReview;
import com.devflow.copilot.entity.ProjectContext;
import com.devflow.copilot.entity.ToolCallRecord;
import com.devflow.copilot.mapper.AgentRunMapper;
import com.devflow.copilot.mapper.AgentStepMapper;
import com.devflow.copilot.mapper.HumanReviewMapper;
import com.devflow.copilot.mapper.ToolCallRecordMapper;
import com.devflow.copilot.service.AgentWorkflowService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgentWorkflowServiceImpl implements AgentWorkflowService {

    private final AgentRunMapper runMapper;
    private final AgentStepMapper stepMapper;
    private final ToolCallRecordMapper toolCallMapper;
    private final HumanReviewMapper humanReviewMapper;

    public AgentWorkflowServiceImpl(
            AgentRunMapper runMapper,
            AgentStepMapper stepMapper,
            ToolCallRecordMapper toolCallMapper,
            HumanReviewMapper humanReviewMapper
    ) {
        this.runMapper = runMapper;
        this.stepMapper = stepMapper;
        this.toolCallMapper = toolCallMapper;
        this.humanReviewMapper = humanReviewMapper;
    }

    @Override
    public AgentRun startRun(ProjectContext project, GenerationRecord record, String title, String goal) {
        LocalDateTime now = LocalDateTime.now();
        AgentRun run = new AgentRun();
        run.setProjectId(project.getId());
        run.setGenerationRecordId(record.getId());
        run.setTitle(summary(title, 240));
        run.setGoal(goal);
        run.setStatus("RUNNING");
        run.setProviderName(record.getProviderName());
        run.setModelName(record.getModelName());
        run.setStartedAt(now);
        run.setCreatedAt(now);
        run.setUpdatedAt(now);
        runMapper.insert(run);
        return run;
    }

    @Override
    public AgentStep addStep(Long runId, int order, String type, String name, String status, String summary, Long latencyMs) {
        LocalDateTime now = LocalDateTime.now();
        AgentStep step = new AgentStep();
        step.setRunId(runId);
        step.setStepOrder(order);
        step.setStepType(type);
        step.setStepName(name);
        step.setStatus(status);
        step.setSummary(summary(summary, 1800));
        step.setLatencyMs(latencyMs);
        step.setStartedAt(now.minusNanos(Math.max(0, latencyMs == null ? 0 : latencyMs) * 1_000_000));
        step.setCompletedAt(now);
        stepMapper.insert(step);
        return step;
    }

    @Override
    public ToolCallRecord addToolCall(Long runId, Long stepId, String toolName, String inputSummary,
                                      String outputSummary, String status, Long latencyMs) {
        ToolCallRecord toolCall = new ToolCallRecord();
        toolCall.setRunId(runId);
        toolCall.setStepId(stepId);
        toolCall.setToolName(toolName);
        toolCall.setInputSummary(summary(inputSummary, 900));
        toolCall.setOutputSummary(summary(outputSummary, 900));
        toolCall.setStatus(status);
        toolCall.setLatencyMs(latencyMs);
        toolCall.setCreatedAt(LocalDateTime.now());
        toolCallMapper.insert(toolCall);
        return toolCall;
    }

    @Override
    public void createPendingReview(Long runId, Long generationRecordId) {
        if (findReview(generationRecordId) != null) {
            return;
        }
        HumanReview review = new HumanReview();
        LocalDateTime now = LocalDateTime.now();
        review.setRunId(runId);
        review.setGenerationRecordId(generationRecordId);
        review.setReviewStatus("PENDING");
        review.setReviewer("manual");
        review.setComment("等待开发者保存或确认生成 Artifact。");
        review.setCreatedAt(now);
        review.setUpdatedAt(now);
        humanReviewMapper.insert(review);
    }

    @Override
    @Transactional
    public void syncGenerationTransition(Long generationRecordId, GenerationStatus target) {
        AgentRun run = findRun(generationRecordId);
        if (run != null) {
            LocalDateTime now = LocalDateTime.now();
            if (target == GenerationStatus.READY_FOR_REVIEW) {
                run.setStatus("WAITING_REVIEW");
            } else if (target == GenerationStatus.SAVED) {
                run.setStatus("SAVED");
            } else if (target == GenerationStatus.CONFIRMED) {
                run.setStatus("CONFIRMED");
                run.setCompletedAt(now);
            } else if (target == GenerationStatus.FAILED) {
                run.setStatus("FAILED");
                run.setCompletedAt(now);
            }
            run.setUpdatedAt(now);
            runMapper.updateById(run);
        }
        if (target == GenerationStatus.SAVED || target == GenerationStatus.CONFIRMED || target == GenerationStatus.FAILED) {
            updateReview(generationRecordId, target);
        }
    }

    @Override
    public List<AgentRun> list(Long projectId, Long generationRecordId) {
        return runMapper.selectList(Wrappers.<AgentRun>lambdaQuery()
                .eq(projectId != null, AgentRun::getProjectId, projectId)
                .eq(generationRecordId != null, AgentRun::getGenerationRecordId, generationRecordId)
                .orderByDesc(AgentRun::getCreatedAt));
    }

    @Override
    public AgentRunTraceResponse getTrace(Long runId) {
        AgentRun run = runMapper.selectById(runId);
        if (run == null) {
            throw new BusinessException(4045, HttpStatus.NOT_FOUND, "Agent Run 不存在：" + runId);
        }
        return AgentRunTraceResponse.builder()
                .run(run)
                .steps(stepMapper.selectList(Wrappers.<AgentStep>lambdaQuery()
                        .eq(AgentStep::getRunId, runId)
                        .orderByAsc(AgentStep::getStepOrder)))
                .toolCalls(toolCallMapper.selectList(Wrappers.<ToolCallRecord>lambdaQuery()
                        .eq(ToolCallRecord::getRunId, runId)
                        .orderByAsc(ToolCallRecord::getCreatedAt)))
                .humanReviews(humanReviewMapper.selectList(Wrappers.<HumanReview>lambdaQuery()
                        .eq(HumanReview::getRunId, runId)
                        .orderByAsc(HumanReview::getCreatedAt)))
                .build();
    }

    private void updateReview(Long generationRecordId, GenerationStatus target) {
        HumanReview review = findReview(generationRecordId);
        if (review == null) {
            AgentRun run = findRun(generationRecordId);
            if (run == null) {
                return;
            }
            createPendingReview(run.getId(), generationRecordId);
            review = findReview(generationRecordId);
        }
        review.setReviewStatus(switch (target) {
            case SAVED -> "SAVED";
            case CONFIRMED -> "CONFIRMED";
            case FAILED -> "REJECTED";
            default -> review.getReviewStatus();
        });
        review.setComment(switch (target) {
            case SAVED -> "开发者已保存生成记录，等待最终确认。";
            case CONFIRMED -> "开发者已完成人工确认。";
            case FAILED -> "生成失败，进入人工复核或重试。";
            default -> review.getComment();
        });
        review.setReviewer("manual");
        review.setUpdatedAt(LocalDateTime.now());
        humanReviewMapper.updateById(review);
    }

    private AgentRun findRun(Long generationRecordId) {
        return runMapper.selectOne(Wrappers.<AgentRun>lambdaQuery()
                .eq(AgentRun::getGenerationRecordId, generationRecordId)
                .last("LIMIT 1"));
    }

    private HumanReview findReview(Long generationRecordId) {
        return humanReviewMapper.selectOne(Wrappers.<HumanReview>lambdaQuery()
                .eq(HumanReview::getGenerationRecordId, generationRecordId)
                .last("LIMIT 1"));
    }

    private String summary(String text, int limit) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String clean = text.replaceAll("\\s+", " ").trim();
        return clean.length() > limit ? clean.substring(0, limit) + "..." : clean;
    }
}
