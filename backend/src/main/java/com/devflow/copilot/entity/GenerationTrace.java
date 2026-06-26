package com.devflow.copilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("generation_trace")
public class GenerationTrace {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long generationRecordId;
    private Integer promptVersion;
    private String inputVariables;
    private String renderedPromptSummary;
    private String providerName;
    private String modelName;
    private String status;
    private Long latencyMs;
    private String errorMessage;
    private LocalDateTime createdAt;
}
