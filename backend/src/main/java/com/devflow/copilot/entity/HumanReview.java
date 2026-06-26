package com.devflow.copilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("human_review")
public class HumanReview {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long runId;
    private Long generationRecordId;
    private String reviewStatus;
    private String reviewer;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
