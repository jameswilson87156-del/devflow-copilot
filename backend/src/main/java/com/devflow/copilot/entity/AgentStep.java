package com.devflow.copilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_step")
public class AgentStep {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long runId;
    private Integer stepOrder;
    private String stepType;
    private String stepName;
    private String status;
    private String summary;
    private Long latencyMs;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
