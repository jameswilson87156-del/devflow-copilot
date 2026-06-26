package com.devflow.copilot.service;

import com.devflow.copilot.dto.KnowledgeDocumentRequest;
import com.devflow.copilot.dto.KnowledgeReferenceResponse;
import com.devflow.copilot.dto.KnowledgeSearchRequest;
import com.devflow.copilot.entity.KnowledgeChunk;
import com.devflow.copilot.entity.KnowledgeDocument;

import java.util.List;

public interface KnowledgeBaseService {

    KnowledgeDocument createDocument(KnowledgeDocumentRequest request);

    List<KnowledgeDocument> listDocuments();

    List<KnowledgeChunk> listChunks(Long documentId);

    List<KnowledgeReferenceResponse> search(KnowledgeSearchRequest request);

    List<KnowledgeReferenceResponse> searchForGeneration(String query, List<Long> documentIds, int topK);

    List<KnowledgeReferenceResponse> attachReferences(Long generationRecordId, List<KnowledgeReferenceResponse> references);

    List<KnowledgeReferenceResponse> listReferences(Long generationRecordId);
}
