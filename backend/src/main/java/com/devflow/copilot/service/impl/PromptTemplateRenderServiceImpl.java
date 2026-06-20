package com.devflow.copilot.service.impl;

import com.devflow.copilot.common.TemplateRenderException;
import com.devflow.copilot.dto.AiGenerateRequest;
import com.devflow.copilot.dto.RenderedPrompt;
import com.devflow.copilot.entity.ProjectContext;
import com.devflow.copilot.entity.PromptTemplate;
import com.devflow.copilot.service.PromptTemplateRenderService;
import com.devflow.copilot.service.PromptTemplateService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PromptTemplateRenderServiceImpl implements PromptTemplateRenderService {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*}}", Pattern.MULTILINE);

    private final PromptTemplateService templateService;

    public PromptTemplateRenderServiceImpl(PromptTemplateService templateService) {
        this.templateService = templateService;
    }

    @Override
    public RenderedPrompt render(String generationType, AiGenerateRequest request, ProjectContext project) {
        PromptTemplate template = request.getTemplateId() == null
                ? templateService.findEnabledDefault(generationType)
                : templateService.getById(request.getTemplateId());
        Map<String, String> variables = buildVariables(request, project);
        if (template == null) {
            String fallback = "项目：" + project.getProjectName() + "\n任务类型：" + generationType
                    + "\n需求：" + request.getInput() + "\n上下文：" + value(request.getExtraContext(), "未提供");
            return new RenderedPrompt(fallback, null, "内置默认 Prompt", 1);
        }
        if (!Boolean.TRUE.equals(template.getEnabled())) {
            throw new TemplateRenderException("Prompt 模板已停用：" + template.getTemplateName());
        }
        validateRequired(template, variables);
        String rendered = replace(template.getTemplateContent(), variables);
        Matcher unresolved = VARIABLE_PATTERN.matcher(rendered);
        if (unresolved.find()) {
            throw new TemplateRenderException("Prompt 模板变量缺失：" + unresolved.group(1));
        }
        return new RenderedPrompt(rendered, template.getId(), template.getTemplateName(), template.getVersion());
    }

    private Map<String, String> buildVariables(AiGenerateRequest request, ProjectContext project) {
        Map<String, String> values = new HashMap<>();
        values.put("projectName", value(project.getProjectName(), "未命名项目"));
        values.put("techStack", value(project.getTechStack(), "Java / Vue"));
        values.put("language", "Java");
        values.put("requirement", value(request.getInput(), ""));
        values.put("input", value(request.getInput(), ""));
        values.put("errorLog", value(request.getInput(), ""));
        values.put("rawLog", value(request.getInput(), ""));
        values.put("context", value(request.getExtraContext(), value(project.getDirectoryStructure(), "未提供")));
        values.put("currentRequirement", value(project.getCurrentRequirement(), "未提供"));
        values.put("codingRules", value(project.getCodingRules(), "遵循现有代码规范"));
        if (request.getVariables() != null) {
            request.getVariables().forEach((key, val) -> values.put(key, value(val, "")));
        }
        return values;
    }

    private void validateRequired(PromptTemplate template, Map<String, String> values) {
        if (template.getVariables() == null || template.getVariables().isBlank()) {
            return;
        }
        Arrays.stream(template.getVariables().split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .filter(name -> values.get(name) == null || values.get(name).isBlank())
                .findFirst()
                .ifPresent(name -> {
                    throw new TemplateRenderException("Prompt 模板缺少必填变量：" + name);
                });
    }

    private String replace(String template, Map<String, String> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String replacement = variables.get(matcher.group(1));
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement == null ? matcher.group() : replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
