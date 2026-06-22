package com.devflow.copilot.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.devflow.copilot.entity.AiTask;
import com.devflow.copilot.mapper.AiTaskMapper;
import com.devflow.copilot.service.AiTaskService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiTaskServiceImpl implements AiTaskService {

    private final AiTaskMapper mapper;

    public AiTaskServiceImpl(AiTaskMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<AiTask> listByProjectId(Long projectId) {
        return mapper.selectList(Wrappers.<AiTask>lambdaQuery()
                .eq(AiTask::getProjectId, projectId)
                .orderByDesc(AiTask::getId));
    }
}
