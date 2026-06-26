package com.devflow.copilot.service;

import com.devflow.copilot.dto.AiGenerateRequest;
import com.devflow.copilot.entity.GenerationRecord;
import com.devflow.copilot.entity.GenerationTrace;

import java.util.List;

public interface GenerationTraceService {

    GenerationTrace record(GenerationRecord record, AiGenerateRequest request, String status);

    List<GenerationTrace> list(Long generationRecordId);

    GenerationTrace getById(Long id);
}
