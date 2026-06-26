package com.devflow.copilot.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.devflow.copilot.common.BusinessException;
import com.devflow.copilot.common.GenerationStatus;
import com.devflow.copilot.dto.RecordQuery;
import com.devflow.copilot.entity.GenerationRecord;
import com.devflow.copilot.mapper.GenerationRecordMapper;
import com.devflow.copilot.service.AgentWorkflowService;
import com.devflow.copilot.service.GenerationRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class GenerationRecordServiceImpl implements GenerationRecordService {

    private final GenerationRecordMapper mapper;
    private final AgentWorkflowService agentWorkflowService;

    public GenerationRecordServiceImpl(GenerationRecordMapper mapper, AgentWorkflowService agentWorkflowService) {
        this.mapper = mapper;
        this.agentWorkflowService = agentWorkflowService;
    }

    @Override
    public GenerationRecord save(GenerationRecord record) {
        LocalDateTime now = LocalDateTime.now();
        if (record.getId() == null) {
            record.setVersion(0);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            mapper.insert(record);
        } else {
            record.setUpdatedAt(now);
            if (mapper.updateById(record) != 1) {
                throw new BusinessException(4093, HttpStatus.CONFLICT, "生成记录已被更新，请刷新后重试");
            }
        }
        return record;
    }

    @Override
    public List<GenerationRecord> list(RecordQuery query) {
        return mapper.selectList(Wrappers.<GenerationRecord>lambdaQuery()
                .eq(query != null && query.getProjectId() != null, GenerationRecord::getProjectId,
                        query == null ? null : query.getProjectId())
                .eq(query != null && query.getGenerationType() != null && !query.getGenerationType().isBlank(),
                        GenerationRecord::getGenerationType, query == null ? null : query.getGenerationType())
                .orderByDesc(GenerationRecord::getCreatedAt));
    }

    @Override
    public GenerationRecord getById(Long id) {
        GenerationRecord record = mapper.selectById(id);
        if (record == null) {
            throw new BusinessException(4043, HttpStatus.NOT_FOUND, "生成记录不存在：" + id);
        }
        return record;
    }

    @Override
    public GenerationRecord confirm(Long id) {
        return transition(id, GenerationStatus.CONFIRMED);
    }

    @Override
    public GenerationRecord saveStatus(Long id) {
        return transition(id, GenerationStatus.SAVED);
    }

    @Override
    @Transactional
    public GenerationRecord transition(Long id, GenerationStatus target) {
        GenerationRecord record = getById(id);
        GenerationStatus current = record.getStatus();
        if (current == null || !current.canTransitionTo(target)) {
            String allowed = current == null ? "无" : current.allowedTargets().toString();
            throw new BusinessException(4094, HttpStatus.CONFLICT,
                    "非法状态流转：" + current + " -> " + target + "，允许流转为 " + allowed);
        }
        record.setStatus(target);
        record.setConfirmed(target == GenerationStatus.CONFIRMED);
        record.setSuccess(target != GenerationStatus.FAILED);
        GenerationRecord saved = save(record);
        agentWorkflowService.syncGenerationTransition(saved.getId(), target);
        return saved;
    }

    @Override
    public List<GenerationRecord> recent(int limit) {
        return mapper.selectList(Wrappers.<GenerationRecord>lambdaQuery()
                .orderByDesc(GenerationRecord::getCreatedAt)
                .last("LIMIT " + Math.max(1, Math.min(limit, 100))));
    }

    @Override
    public long todayCount() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return mapper.selectCount(Wrappers.<GenerationRecord>lambdaQuery()
                .ge(GenerationRecord::getCreatedAt, start)
                .lt(GenerationRecord::getCreatedAt, end));
    }

    public long successCount() {
        return mapper.selectCount(Wrappers.<GenerationRecord>lambdaQuery()
                .eq(GenerationRecord::getSuccess, true));
    }
}
