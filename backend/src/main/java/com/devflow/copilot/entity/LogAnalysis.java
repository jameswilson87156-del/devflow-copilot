package com.devflow.copilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("log_analysis")
public class LogAnalysis {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String rawLog;
    private String exceptionType;
    private String possibleReason;
    private String diagnoseSteps;
    private String fixPrompt;
    private String riskTips;
    private String riskLevel;
    @Version
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
