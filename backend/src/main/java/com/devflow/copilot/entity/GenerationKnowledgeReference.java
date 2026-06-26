package com.devflow.copilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("generation_knowledge_reference")
public class GenerationKnowledgeReference {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long generationRecordId;
    private Long chunkId;
    private Long documentId;
    private Double score;
    private String citationLabel;
    private String snippet;
    private LocalDateTime createdAt;
}
