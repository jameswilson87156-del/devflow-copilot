package com.devflow.copilot.service;

import com.devflow.copilot.dto.RecordQuery;
import com.devflow.copilot.entity.GenerationRecord;
import com.devflow.copilot.common.GenerationStatus;

import java.util.List;

public interface GenerationRecordService {

    GenerationRecord save(GenerationRecord record);

    List<GenerationRecord> list(RecordQuery query);

    GenerationRecord getById(Long id);

    GenerationRecord confirm(Long id);

    GenerationRecord saveStatus(Long id);

    GenerationRecord transition(Long id, GenerationStatus target);

    List<GenerationRecord> recent(int limit);

    long todayCount();
}
