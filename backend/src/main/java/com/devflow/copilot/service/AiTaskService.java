package com.devflow.copilot.service;

import com.devflow.copilot.entity.AiTask;

import java.util.List;

public interface AiTaskService {

    List<AiTask> listByProjectId(Long projectId);
}
