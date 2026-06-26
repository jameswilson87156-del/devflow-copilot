package com.devflow.copilot.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeReferenceResponse {

    private Long documentId;
    private String documentTitle;
    private Long chunkId;
    private Integer chunkIndex;
    private Double score;
    private String citationLabel;
    private String snippet;
}
