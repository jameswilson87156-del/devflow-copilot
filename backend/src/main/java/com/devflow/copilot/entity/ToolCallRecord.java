package com.devflow.copilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tool_call_record")
public class ToolCallRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long runId;
    private Long stepId;
    private String toolName;
    private String inputSummary;
    private String outputSummary;
    private String status;
    private Long latencyMs;
    private LocalDateTime createdAt;
}
