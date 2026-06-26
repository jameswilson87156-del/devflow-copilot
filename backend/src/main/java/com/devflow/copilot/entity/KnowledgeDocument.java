package com.devflow.copilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_document")
public class KnowledgeDocument {

    @TableId(type = IdType.AUTO)
    private Long id;
    @NotBlank(message = "知识文档标题不能为空")
    @Size(max = 256, message = "知识文档标题不能超过 256 个字符")
    private String title;
    private String sourceType;
    private String sourceUri;
    @NotBlank(message = "知识文档内容不能为空")
    @Size(max = 50000, message = "知识文档内容不能超过 50000 个字符")
    private String content;
    private Integer chunkCount;
    private String embeddingModel;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
