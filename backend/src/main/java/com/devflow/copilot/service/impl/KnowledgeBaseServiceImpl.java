package com.devflow.copilot.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.devflow.copilot.common.BusinessException;
import com.devflow.copilot.dto.KnowledgeDocumentRequest;
import com.devflow.copilot.dto.KnowledgeReferenceResponse;
import com.devflow.copilot.dto.KnowledgeSearchRequest;
import com.devflow.copilot.entity.GenerationKnowledgeReference;
import com.devflow.copilot.entity.KnowledgeChunk;
import com.devflow.copilot.entity.KnowledgeDocument;
import com.devflow.copilot.mapper.GenerationKnowledgeReferenceMapper;
import com.devflow.copilot.mapper.KnowledgeChunkMapper;
import com.devflow.copilot.mapper.KnowledgeDocumentMapper;
import com.devflow.copilot.service.KnowledgeBaseService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final int CHUNK_SIZE = 700;
    private static final int MAX_TOP_K = 10;

    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeChunkMapper chunkMapper;
    private final GenerationKnowledgeReferenceMapper referenceMapper;

    public KnowledgeBaseServiceImpl(
            KnowledgeDocumentMapper documentMapper,
            KnowledgeChunkMapper chunkMapper,
            GenerationKnowledgeReferenceMapper referenceMapper
    ) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.referenceMapper = referenceMapper;
    }

    @Override
    @Transactional
    public KnowledgeDocument createDocument(KnowledgeDocumentRequest request) {
        LocalDateTime now = LocalDateTime.now();
        KnowledgeDocument document = new KnowledgeDocument();
        document.setTitle(request.getTitle().trim());
        document.setSourceType(blankToDefault(request.getSourceType(), "manual"));
        document.setSourceUri(request.getSourceUri());
        document.setContent(request.getContent().trim());
        document.setEmbeddingModel(request.getEmbeddingModel());
        document.setMetadata(request.getMetadata());
        document.setChunkCount(0);
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        documentMapper.insert(document);

        List<String> chunks = chunkText(document.getContent());
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocumentId(document.getId());
            chunk.setChunkIndex(i);
            chunk.setContent(chunks.get(i));
            chunk.setContentSummary(summary(chunks.get(i), 220));
            chunk.setKeywords(extractKeywords(chunks.get(i)));
            chunk.setEmbeddingModel(document.getEmbeddingModel());
            chunk.setEmbeddingVector(null);
            chunk.setCreatedAt(now);
            chunk.setUpdatedAt(now);
            chunkMapper.insert(chunk);
        }
        document.setChunkCount(chunks.size());
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        return document;
    }

    @Override
    public List<KnowledgeDocument> listDocuments() {
        return documentMapper.selectList(Wrappers.<KnowledgeDocument>lambdaQuery()
                .orderByDesc(KnowledgeDocument::getUpdatedAt));
    }

    @Override
    public List<KnowledgeChunk> listChunks(Long documentId) {
        ensureDocumentExists(documentId);
        return chunkMapper.selectList(Wrappers.<KnowledgeChunk>lambdaQuery()
                .eq(KnowledgeChunk::getDocumentId, documentId)
                .orderByAsc(KnowledgeChunk::getChunkIndex));
    }

    @Override
    public List<KnowledgeReferenceResponse> search(KnowledgeSearchRequest request) {
        int topK = normalizeTopK(request.getTopK());
        return searchForGeneration(request.getQuery(), request.getDocumentIds(), topK);
    }

    @Override
    public List<KnowledgeReferenceResponse> searchForGeneration(String query, List<Long> documentIds, int topK) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<KnowledgeChunk> chunks = chunkMapper.selectList(Wrappers.<KnowledgeChunk>lambdaQuery()
                .in(documentIds != null && !documentIds.isEmpty(), KnowledgeChunk::getDocumentId, documentIds)
                .orderByAsc(KnowledgeChunk::getDocumentId)
                .orderByAsc(KnowledgeChunk::getChunkIndex));
        Set<String> terms = tokenize(query);
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        return chunks.stream()
                .map(chunk -> toScoredReference(chunk, normalizedQuery, terms))
                .filter(item -> item.getScore() != null && item.getScore() > 0)
                .sorted(Comparator.comparing(KnowledgeReferenceResponse::getScore).reversed()
                        .thenComparing(KnowledgeReferenceResponse::getDocumentId)
                        .thenComparing(KnowledgeReferenceResponse::getChunkIndex))
                .limit(Math.max(1, Math.min(topK, MAX_TOP_K)))
                .toList();
    }

    @Override
    @Transactional
    public List<KnowledgeReferenceResponse> attachReferences(Long generationRecordId, List<KnowledgeReferenceResponse> references) {
        if (generationRecordId == null || references == null || references.isEmpty()) {
            return List.of();
        }
        referenceMapper.delete(Wrappers.<GenerationKnowledgeReference>lambdaQuery()
                .eq(GenerationKnowledgeReference::getGenerationRecordId, generationRecordId));
        LocalDateTime now = LocalDateTime.now();
        for (KnowledgeReferenceResponse item : references) {
            GenerationKnowledgeReference reference = new GenerationKnowledgeReference();
            reference.setGenerationRecordId(generationRecordId);
            reference.setChunkId(item.getChunkId());
            reference.setDocumentId(item.getDocumentId());
            reference.setScore(item.getScore());
            reference.setCitationLabel(item.getCitationLabel());
            reference.setSnippet(summary(item.getSnippet(), 500));
            reference.setCreatedAt(now);
            referenceMapper.insert(reference);
        }
        return listReferences(generationRecordId);
    }

    @Override
    public List<KnowledgeReferenceResponse> listReferences(Long generationRecordId) {
        List<GenerationKnowledgeReference> records = referenceMapper.selectList(
                Wrappers.<GenerationKnowledgeReference>lambdaQuery()
                        .eq(GenerationKnowledgeReference::getGenerationRecordId, generationRecordId)
                        .orderByDesc(GenerationKnowledgeReference::getScore));
        return records.stream().map(record -> {
            KnowledgeDocument document = documentMapper.selectById(record.getDocumentId());
            KnowledgeChunk chunk = chunkMapper.selectById(record.getChunkId());
            return KnowledgeReferenceResponse.builder()
                    .documentId(record.getDocumentId())
                    .documentTitle(document == null ? "知识文档 #" + record.getDocumentId() : document.getTitle())
                    .chunkId(record.getChunkId())
                    .chunkIndex(chunk == null ? null : chunk.getChunkIndex())
                    .score(record.getScore())
                    .citationLabel(record.getCitationLabel())
                    .snippet(record.getSnippet())
                    .build();
        }).toList();
    }

    private KnowledgeReferenceResponse toScoredReference(KnowledgeChunk chunk, String normalizedQuery, Set<String> terms) {
        KnowledgeDocument document = documentMapper.selectById(chunk.getDocumentId());
        String content = chunk.getContent() == null ? "" : chunk.getContent();
        String haystack = (content + "\n" + blankToDefault(chunk.getKeywords(), "")).toLowerCase(Locale.ROOT);
        double score = haystack.contains(normalizedQuery) ? 5.0 : 0.0;
        for (String term : terms) {
            if (haystack.contains(term)) {
                score += 1.0;
            }
        }
        return KnowledgeReferenceResponse.builder()
                .documentId(chunk.getDocumentId())
                .documentTitle(document == null ? "知识文档 #" + chunk.getDocumentId() : document.getTitle())
                .chunkId(chunk.getId())
                .chunkIndex(chunk.getChunkIndex())
                .score(score)
                .citationLabel((document == null ? "doc-" + chunk.getDocumentId() : document.getTitle()) + " #chunk-" + chunk.getChunkIndex())
                .snippet(summary(content, 500))
                .build();
    }

    private List<String> chunkText(String content) {
        String normalized = content.replace("\r\n", "\n").trim();
        String[] paragraphs = normalized.split("\\n\\s*\\n");
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            String clean = paragraph.trim();
            if (clean.isEmpty()) {
                continue;
            }
            if (current.length() + clean.length() + 2 > CHUNK_SIZE && !current.isEmpty()) {
                chunks.add(current.toString().trim());
                current.setLength(0);
            }
            if (clean.length() > CHUNK_SIZE) {
                flushLongText(clean, chunks);
            } else {
                current.append(clean).append("\n\n");
            }
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }
        return chunks.isEmpty() ? List.of(normalized) : chunks;
    }

    private void flushLongText(String text, List<String> chunks) {
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            chunks.add(text.substring(start, end).trim());
            start = end;
        }
    }

    private String extractKeywords(String content) {
        return String.join(",", tokenize(content).stream().limit(16).toList());
    }

    private Set<String> tokenize(String text) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        for (String token : text.toLowerCase(Locale.ROOT).split("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4e00-\\u9fa5]+")) {
            String clean = token.trim();
            if (clean.length() >= 2) {
                terms.add(clean);
            }
        }
        if (terms.isEmpty() && !text.isBlank()) {
            terms.add(text.toLowerCase(Locale.ROOT).trim());
        }
        return terms;
    }

    private void ensureDocumentExists(Long documentId) {
        if (documentMapper.selectById(documentId) == null) {
            throw new BusinessException(4046, HttpStatus.NOT_FOUND, "知识文档不存在：" + documentId);
        }
    }

    private int normalizeTopK(Integer topK) {
        return Math.max(1, Math.min(topK == null ? 5 : topK, MAX_TOP_K));
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String summary(String text, int limit) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String clean = text.replaceAll("\\s+", " ").trim();
        return clean.length() > limit ? clean.substring(0, limit) + "..." : clean;
    }
}
