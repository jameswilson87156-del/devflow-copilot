package com.devflow.copilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeDocumentRequest {

    @NotBlank(message = "知识文档标题不能为空")
    @Size(max = 256, message = "知识文档标题不能超过 256 个字符")
    private String title;
    @Size(max = 64, message = "sourceType 不能超过 64 个字符")
    private String sourceType;
    @Size(max = 512, message = "sourceUri 不能超过 512 个字符")
    private String sourceUri;
    @NotBlank(message = "知识文档内容不能为空")
    @Size(max = 50000, message = "知识文档内容不能超过 50000 个字符")
    private String content;
    @Size(max = 128, message = "embeddingModel 不能超过 128 个字符")
    private String embeddingModel;
    @Size(max = 2000, message = "metadata 不能超过 2000 个字符")
    private String metadata;
}
