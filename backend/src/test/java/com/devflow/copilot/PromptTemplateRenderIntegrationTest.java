package com.devflow.copilot;

import com.devflow.copilot.common.TemplateRenderException;
import com.devflow.copilot.dto.AiGenerateRequest;
import com.devflow.copilot.dto.AiGenerateResponse;
import com.devflow.copilot.dto.RenderedPrompt;
import com.devflow.copilot.entity.GenerationRecord;
import com.devflow.copilot.entity.ProjectContext;
import com.devflow.copilot.entity.PromptTemplate;
import com.devflow.copilot.service.AiGenerateService;
import com.devflow.copilot.service.GenerationRecordService;
import com.devflow.copilot.service.ProjectContextService;
import com.devflow.copilot.service.PromptTemplateRenderService;
import com.devflow.copilot.service.PromptTemplateService;
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
class PromptTemplateRenderIntegrationTest {

    @Autowired PromptTemplateRenderService renderService;
    @Autowired PromptTemplateService templateService;
    @Autowired ProjectContextService projectService;
    @Autowired AiGenerateService aiGenerateService;
    @Autowired GenerationRecordService recordService;

    @Test
    void rendersDefaultTemplateVariables() {
        ProjectContext project = projectService.getById(1L);
        AiGenerateRequest request = request("增加生成历史筛选");

        RenderedPrompt rendered = renderService.render("requirement-split", request, project);

        assertThat(rendered.content()).contains("DevFlow Copilot", "增加生成历史筛选");
        assertThat(rendered.templateName()).isEqualTo("需求拆解模板");
        assertThat(rendered.templateVersion()).isEqualTo(1);
    }

    @Test
    void rejectsMissingRequiredTemplateVariable() {
        PromptTemplate template = template("custom-missing", "自定义变量模板", "requirement-split",
                "项目 {{projectName}}，负责人 {{owner}}", "projectName,owner");
        template = templateService.create(template);
        AiGenerateRequest request = request("测试变量校验");
        request.setTemplateId(template.getId());

        PromptTemplate finalTemplate = template;
        assertThatThrownBy(() -> renderService.render("requirement-split", request, projectService.getById(1L)))
                .isInstanceOf(TemplateRenderException.class)
                .hasMessageContaining("owner");
        assertThat(finalTemplate.getVersion()).isEqualTo(1);
    }

    @Test
    void templateChangeAffectsLocalRuleOutputAndRecordsVersion() {
        PromptTemplate template = template("custom-plan", "自定义计划模板", "code-plan",
                "SPECIAL-MARKER {{requirement}}", "requirement");
        template = templateService.create(template);
        template.setTemplateContent("SPECIAL-MARKER-V2 {{requirement}}");
        PromptTemplate updated = templateService.update(template.getId(), template);
        AiGenerateRequest request = request("重构订单模块");
        request.setTemplateId(updated.getId());

        AiGenerateResponse response = aiGenerateService.generate("code-plan", request);
        GenerationRecord record = recordService.getById(response.getRecordId());

        assertThat(response.getOutputContent()).contains("SPECIAL-MARKER-V2");
        assertThat(record.getPromptTemplateName()).isEqualTo("自定义计划模板");
        assertThat(record.getPromptTemplateVersion()).isEqualTo(2);
    }

    private AiGenerateRequest request(String input) {
        AiGenerateRequest request = new AiGenerateRequest();
        request.setProjectId(1L);
        request.setInput(input);
        request.setExtraContext("backend/service");
        return request;
    }

    private PromptTemplate template(String key, String name, String type, String content, String variables) {
        PromptTemplate template = new PromptTemplate();
        template.setTemplateKey(key);
        template.setTemplateName(name);
        template.setTemplateType(type);
        template.setTemplateContent(content);
        template.setVariables(variables);
        template.setEnabled(true);
        template.setIsDefault(false);
        return template;
    }
}
