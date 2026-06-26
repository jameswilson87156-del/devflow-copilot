package com.devflow.copilot.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.devflow.copilot.common.BusinessException;
import com.devflow.copilot.dto.AiGenerateRequest;
import com.devflow.copilot.entity.GenerationRecord;
import com.devflow.copilot.entity.GenerationTrace;
import com.devflow.copilot.mapper.GenerationTraceMapper;
import com.devflow.copilot.service.GenerationTraceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GenerationTraceServiceImpl implements GenerationTraceService {

    private final GenerationTraceMapper mapper;
    private final ObjectMapper objectMapper;

    public GenerationTraceServiceImpl(GenerationTraceMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public GenerationTrace record(GenerationRecord record, AiGenerateRequest request, String status) {
        GenerationTrace trace = new GenerationTrace();
        trace.setGenerationRecordId(record.getId());
        trace.setPromptVersion(record.getPromptTemplateVersion());
        trace.setInputVariables(inputVariables(record, request));
        trace.setRenderedPromptSummary(summary(record.getRenderedPrompt(), 900));
        trace.setProviderName(record.getProviderName());
        trace.setModelName(record.getModelName());
        trace.setStatus(status);
        trace.setLatencyMs(record.getCostTimeMs());
        trace.setErrorMessage(record.getErrorMessage());
        trace.setCreatedAt(LocalDateTime.now());
        mapper.insert(trace);
        return trace;
    }

    @Override
    public List<GenerationTrace> list(Long generationRecordId) {
        return mapper.selectList(Wrappers.<GenerationTrace>lambdaQuery()
                .eq(generationRecordId != null, GenerationTrace::getGenerationRecordId, generationRecordId)
                .orderByDesc(GenerationTrace::getCreatedAt));
    }

    @Override
    public GenerationTrace getById(Long id) {
        GenerationTrace trace = mapper.selectById(id);
        if (trace == null) {
            throw new BusinessException(4044, HttpStatus.NOT_FOUND, "Generation Trace 不存在：" + id);
        }
        return trace;
    }

    private String inputVariables(GenerationRecord record, AiGenerateRequest request) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("generationType", record.getGenerationType());
        values.put("projectId", record.getProjectId());
        values.put("inputSummary", record.getInputSummary());
        values.put("templateId", record.getPromptTemplateId());
        values.put("templateVersion", record.getPromptTemplateVersion());
        values.put("knowledgeQuery", request.getKnowledgeQuery());
        values.put("knowledgeDocumentIds", request.getKnowledgeDocumentIds());
        values.put("customVariables", request.getVariables());
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            return values.toString();
        }
    }

    private String summary(String text, int limit) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String clean = text.replaceAll("\\s+", " ").trim();
        return clean.length() > limit ? clean.substring(0, limit) + "..." : clean;
    }
}
