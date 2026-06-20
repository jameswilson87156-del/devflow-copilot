package com.devflow.copilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.devflow.copilot.common.GenerationStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("generation_record")
public class GenerationRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String generationType;
    private String inputSummary;
    private String inputContent;
    private String outputContent;
    private GenerationStatus status;
    private Boolean confirmed;
    private String providerName;
    private String modelName;
    private Long promptTemplateId;
    private String promptTemplateName;
    private Integer promptTemplateVersion;
    private String renderedPrompt;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Long costTimeMs;
    private Boolean success;
    private String errorMessage;
    @Version
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
