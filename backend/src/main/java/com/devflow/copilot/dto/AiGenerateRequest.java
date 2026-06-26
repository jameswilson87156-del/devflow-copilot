package com.devflow.copilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AiGenerateRequest {

    @Positive(message = "projectId 必须为正整数")
    private Long projectId;
    @NotBlank(message = "生成输入不能为空")
    @Size(max = 20000, message = "生成输入不能超过 20000 个字符")
    private String input;
    @Size(max = 20000, message = "补充上下文不能超过 20000 个字符")
    private String extraContext;
    @Positive(message = "templateId 必须为正整数")
    private Long templateId;
    @Size(max = 30, message = "模板变量不能超过 30 个")
    private Map<String, String> variables;
    @Size(max = 1000, message = "知识检索关键词不能超过 1000 个字符")
    private String knowledgeQuery;
    @Size(max = 20, message = "最多指定 20 个知识文档")
    private List<Long> knowledgeDocumentIds;
}
