package com.devflow.copilot;

import com.devflow.copilot.common.BusinessException;
import com.devflow.copilot.common.GenerationStatus;
import com.devflow.copilot.dto.AiGenerateRequest;
import com.devflow.copilot.dto.AiGenerateResponse;
import com.devflow.copilot.dto.RecordQuery;
import com.devflow.copilot.entity.GenerationRecord;
import com.devflow.copilot.service.AiGenerateService;
import com.devflow.copilot.service.GenerationRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GenerationWorkflowIntegrationTest {

    @Autowired AiGenerateService aiGenerateService;
    @Autowired GenerationRecordService recordService;

    @Test
    void localRuleGeneratesAndPersistsTraceMetadata() {
        AiGenerateResponse response = generate("需求拆解测试");
        GenerationRecord persisted = recordService.getById(response.getRecordId());

        assertThat(response.getProviderName()).isEqualTo("local-rule");
        assertThat(response.getTotalTokens()).isPositive();
        assertThat(persisted.getStatus()).isEqualTo(GenerationStatus.READY_FOR_REVIEW);
        assertThat(persisted.getRenderedPrompt()).contains("需求拆解测试");
    }

    @Test
    void generationHistoryCanBeQueriedAfterSave() {
        AiGenerateResponse response = generate("持久化查询测试");
        RecordQuery query = new RecordQuery();
        query.setProjectId(1L);

        assertThat(recordService.list(query)).extracting(GenerationRecord::getId)
                .contains(response.getRecordId());
    }

    @Test
    void readyRecordCanTransitionToSavedThenConfirmed() {
        AiGenerateResponse response = generate("状态流转测试");

        GenerationRecord saved = recordService.saveStatus(response.getRecordId());
        GenerationRecord confirmed = recordService.confirm(response.getRecordId());

        assertThat(saved.getStatus()).isEqualTo(GenerationStatus.SAVED);
        assertThat(confirmed.getStatus()).isEqualTo(GenerationStatus.CONFIRMED);
        assertThat(confirmed.getConfirmed()).isTrue();
    }

    @Test
    void readyRecordCannotConfirmDirectly() {
        AiGenerateResponse response = generate("非法确认测试");

        assertThatThrownBy(() -> recordService.confirm(response.getRecordId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非法状态流转");
    }

    @Test
    void confirmedRecordIsTerminal() {
        AiGenerateResponse response = generate("终态测试");
        recordService.saveStatus(response.getRecordId());
        recordService.confirm(response.getRecordId());

        assertThatThrownBy(() -> recordService.saveStatus(response.getRecordId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CONFIRMED");
    }

    private AiGenerateResponse generate(String input) {
        AiGenerateRequest request = new AiGenerateRequest();
        request.setProjectId(1L);
        request.setInput(input);
        request.setExtraContext("测试上下文");
        return aiGenerateService.generate("requirement-split", request);
    }
}
