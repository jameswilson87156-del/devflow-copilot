package com.devflow.copilot.service;

import com.devflow.copilot.dto.AiGenerateRequest;
import com.devflow.copilot.dto.RenderedPrompt;
import com.devflow.copilot.entity.ProjectContext;

public interface PromptTemplateRenderService {

    RenderedPrompt render(String generationType, AiGenerateRequest request, ProjectContext project);
}
