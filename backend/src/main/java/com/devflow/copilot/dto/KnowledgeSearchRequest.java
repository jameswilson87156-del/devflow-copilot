package com.devflow.copilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class KnowledgeSearchRequest {

    @NotBlank(message = "检索关键词不能为空")
    @Size(max = 1000, message = "检索关键词不能超过 1000 个字符")
    private String query;
    @Size(max = 20, message = "最多指定 20 个知识文档")
    private List<Long> documentIds;
    @Positive(message = "topK 必须为正整数")
    private Integer topK;
}
