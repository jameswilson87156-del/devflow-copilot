package com.devflow.copilot.controller;

import com.devflow.copilot.common.ApiResponse;
import com.devflow.copilot.dto.KnowledgeDocumentRequest;
import com.devflow.copilot.dto.KnowledgeReferenceResponse;
import com.devflow.copilot.dto.KnowledgeSearchRequest;
import com.devflow.copilot.entity.KnowledgeChunk;
import com.devflow.copilot.entity.KnowledgeDocument;
import com.devflow.copilot.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @GetMapping("/documents")
    public ApiResponse<List<KnowledgeDocument>> listDocuments() {
        return ApiResponse.ok(knowledgeBaseService.listDocuments());
    }

    @PostMapping("/documents")
    public ApiResponse<KnowledgeDocument> createDocument(@Valid @RequestBody KnowledgeDocumentRequest request) {
        return ApiResponse.ok(knowledgeBaseService.createDocument(request));
    }

    @GetMapping("/documents/{id}/chunks")
    public ApiResponse<List<KnowledgeChunk>> listChunks(@PathVariable Long id) {
        return ApiResponse.ok(knowledgeBaseService.listChunks(id));
    }

    @PostMapping("/search")
    public ApiResponse<List<KnowledgeReferenceResponse>> search(@Valid @RequestBody KnowledgeSearchRequest request) {
        return ApiResponse.ok(knowledgeBaseService.search(request));
    }

    @GetMapping("/references")
    public ApiResponse<List<KnowledgeReferenceResponse>> references(@RequestParam Long generationRecordId) {
        return ApiResponse.ok(knowledgeBaseService.listReferences(generationRecordId));
    }
}
