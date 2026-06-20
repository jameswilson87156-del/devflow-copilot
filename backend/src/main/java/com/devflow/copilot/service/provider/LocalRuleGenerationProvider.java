package com.devflow.copilot.service.provider;

import com.devflow.copilot.common.GenerationType;
import org.springframework.stereotype.Component;

@Component
public class LocalRuleGenerationProvider implements GenerationProvider {

    @Override
    public String key() {
        return "local-rule";
    }

    @Override
    public ProviderResult generate(ProviderRequest request) {
        String content = switch (request.generationType()) {
            case GenerationType.REQUIREMENT_SPLIT -> requirementSplit(request);
            case GenerationType.CODE_PLAN -> codePlan(request);
            case GenerationType.README_GENERATE -> readme(request);
            case GenerationType.COMMIT_MESSAGE -> commitMessage(request);
            case GenerationType.FIX_PROMPT -> fixPrompt(request);
            default -> generic(request);
        };
        int promptTokens = estimateTokens(request.renderedPrompt());
        int completionTokens = estimateTokens(content);
        return new ProviderResult(content, key(), "local-rule-mvp", promptTokens, completionTokens,
                promptTokens + completionTokens, null);
    }

    private String requirementSplit(ProviderRequest request) {
        return """
                ## 模板渲染结果

                %s

                ## 后端任务

                1. 明确 Controller、Service、DTO 与持久化边界。
                2. 补齐参数校验、异常处理和可追踪状态字段。
                3. 为核心接口增加正常与异常路径测试。

                ## 前端任务

                1. 保留当前路由与 API，增加 loading、失败和人工确认状态。
                2. 长内容使用可滚动面板，关键字段支持复制和追溯。

                ## 数据库与测试

                1. 使用迁移脚本维护表结构，禁止依赖仅存在于进程内的数据。
                2. 覆盖创建、生成、保存、确认与非法状态流转。

                ## 风险与验收

                - 所有生成结果保持 review-only。
                - 后端测试与前端构建必须通过。
                """.formatted(request.renderedPrompt());
    }

    private String codePlan(ProviderRequest request) {
        return """
                ## 模板渲染结果

                %s

                ## 修改计划

                1. 阅读现有 Entity、Mapper、Service 和前端类型，保持 API 路径兼容。
                2. 先完成数据结构与状态约束，再接入页面展示。
                3. 每个大模块完成后运行定向测试，最后执行完整构建。

                ## 建议文件范围

                - `backend/src/main/java/.../service`
                - `backend/src/main/resources/db/migration`
                - `frontend/src/types/domain.ts`
                - `frontend/src/views`

                ## 验证命令

                ```bash
                cd backend && mvn test
                cd frontend && npm run build
                ```

                ## 回滚建议

                数据库变更通过新增 Flyway 版本向前修复；前端保持旧字段兼容，避免一次性切断调用链。
                """.formatted(request.renderedPrompt());
    }

    private String readme(ProviderRequest request) {
        return """
                ## 模板渲染结果

                %s

                ## 项目简介

                `%s` 是一个面向 Java 开发者的 AI Coding 工作流控制台，用于组织项目上下文、Prompt、生成记录与人工确认。

                ## 技术栈

                %s

                ## MVP 边界

                默认使用 local-rule 稳定演示；配置 OpenAI-compatible Provider 后可调用真实模型。生成结果不会自动修改代码。
                """.formatted(request.renderedPrompt(), request.projectName(), request.techStack());
    }

    private String commitMessage(ProviderRequest request) {
        return """
                ## 模板渲染结果

                %s

                ## 推荐 Commit Message

                ```text
                feat(devflow): improve AI generation workflow
                ```

                ## 中文说明

                完善 Prompt 渲染、生成记录追踪与人工确认流程，并补充工程化验证。
                """.formatted(request.renderedPrompt());
    }

    private String fixPrompt(ProviderRequest request) {
        return """
                ## 模板渲染结果

                %s

                ## 可复制修复 Prompt

                ```text
                请先定位与当前问题直接相关的 Controller、Service、Mapper 和 Vue 页面，只做最小必要修改。
                保持现有 API 路径兼容，补充参数校验、失败处理与状态流转测试。
                完成后运行 mvn test 和 npm run build，并列出修改文件与验证结果。
                ```

                ## 人工确认边界

                输出只作为 review-only Artifact，不自动执行 Git、数据库或代码修改操作。
                """.formatted(request.renderedPrompt());
    }

    private String generic(ProviderRequest request) {
        return "## 模板渲染结果\n\n" + request.renderedPrompt()
                + "\n\n## 建议\n\n请按最小修改、可验证、需人工确认的原则执行。";
    }

    private int estimateTokens(String text) {
        return text == null || text.isBlank() ? 0 : Math.max(1, (int) Math.ceil(text.length() / 3.5));
    }
}
