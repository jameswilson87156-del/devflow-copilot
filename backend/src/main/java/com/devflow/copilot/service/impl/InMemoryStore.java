package com.devflow.copilot.service.impl;

import com.devflow.copilot.common.GenerationStatus;
import com.devflow.copilot.common.GenerationType;
import com.devflow.copilot.entity.GenerationRecord;
import com.devflow.copilot.entity.LogAnalysis;
import com.devflow.copilot.entity.ProjectContext;
import com.devflow.copilot.entity.PromptTemplate;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Demo-only in-memory backing store for the {@code memory-demo} profile.
 *
 * <p>This component is kept only for no-database or lightweight local demo scenarios. It is not
 * the default dev/test/prod persistence path, and it must not be described in resumes or README
 * material as production-grade storage. The default application flow should use the
 * MyBatis-Plus/database persistence implementation backed by H2 or MySQL.</p>
 *
 * <p>This note documents the profile boundary only; it intentionally does not change runtime
 * behavior, {@link Profile} configuration, fields, or methods.</p>
 */
@Component
@Profile("memory-demo")
public class InMemoryStore {

    private final AtomicLong projectId = new AtomicLong(0);
    private final AtomicLong promptId = new AtomicLong(0);
    private final AtomicLong recordId = new AtomicLong(0);
    private final AtomicLong logId = new AtomicLong(0);

    private final Map<Long, ProjectContext> projects = new ConcurrentHashMap<>();
    private final Map<Long, PromptTemplate> prompts = new ConcurrentHashMap<>();
    private final Map<Long, GenerationRecord> records = new ConcurrentHashMap<>();
    private final Map<Long, LogAnalysis> logs = new ConcurrentHashMap<>();

    @PostConstruct
    public void initDemoData() {
        ProjectContext devflow = new ProjectContext();
        devflow.setProjectName("DevFlow Copilot");
        devflow.setTechStack("Java 17, Spring Boot 3, MyBatis-Plus, MySQL, Vue 3, Vite, TypeScript");
        devflow.setReadmeContent("AI Coding workflow console MVP. Local rule generation first, LLM integration later.");
        devflow.setDirectoryStructure("backend/src/main/java/com/devflow/copilot\nfrontend/src/views\nfrontend/src/components");
        devflow.setCurrentRequirement("优化 Workbench 前端三栏布局，增强 AI 输出区的 Markdown 展示和复制体验。");
        devflow.setCodingRules("Every artifact is review-only until the developer explicitly confirms it.");
        createProject(devflow);

        ProjectContext jdAnalyzer = new ProjectContext();
        jdAnalyzer.setProjectName("ai-jd-analyzer");
        jdAnalyzer.setTechStack("Java 17, Spring Boot 3, MyBatis-Plus, MySQL, Vue 3, Vite, Axios");
        jdAnalyzer.setReadmeContent("AI 求职 Agent，用于 JD 匹配、简历优化、投递记录和面试复盘。");
        jdAnalyzer.setDirectoryStructure("backend/src/main/java/com/jd/agent\nfrontend/src/views/apply\nfrontend/src/views/review\nsql/init.sql");
        jdAnalyzer.setCurrentRequirement("给 AI 求职 Agent 增加投递复盘模块，支持记录面试问题和复盘建议。");
        jdAnalyzer.setCodingRules("先做 MVP；输出修改计划和 Prompt；所有 AI 建议需要人工确认后使用。");
        createProject(jdAnalyzer);

        ProjectContext powerPlant = new ProjectContext();
        powerPlant.setProjectName("power-plant-system");
        powerPlant.setTechStack("Java 17, Spring Boot 3, MyBatis-Plus, MySQL, Redis, Vue 3");
        powerPlant.setReadmeContent("电厂设备巡检与告警管理演示系统。");
        powerPlant.setDirectoryStructure("src/main/java/com/power/plant\nsrc/main/resources/application.yml\nfrontend/src/views/alarm");
        powerPlant.setCurrentRequirement("排查 Spring Boot 启动失败，端口 8080 被占用或 BeanCreationException。");
        powerPlant.setCodingRules("先定位日志根因，再给最小修复；不修改生产配置；不暴露真实数据库密码。");
        createProject(powerPlant);

        List.of(
                seedPrompt("requirement-split", "需求拆解模板", "requirement", "将用户需求拆解为后端、前端、数据库、测试、风险和验收标准。"),
                seedPrompt("log-analysis", "日志分析模板", "log", "识别 Java/Spring Boot 异常类型，输出原因、排查步骤、修复 Prompt 和风险提示。"),
                seedPrompt("code-plan", "代码修改计划模板", "plan", "根据需求输出建议修改文件、修改步骤、验证方式和回滚建议。"),
                seedPrompt("readme-generate", "README 生成模板", "document", "生成项目简介、启动方式、功能模块、接口说明和 MVP 边界。"),
                seedPrompt("commit-message", "Commit Message 模板", "document", "生成 Conventional Commits 风格提交说明、中文说明和变更摘要。"),
                seedPrompt("fix-prompt", "修复 Prompt 模板", "prompt", "生成可复制给 Codex 或 Claude Code 的结构化修复提示词。")
        ).forEach(this::createPrompt);

        seedRecord(2L, GenerationType.REQUIREMENT_SPLIT, "ai-jd-analyzer 投递复盘模块",
                "给 AI 求职 Agent 增加投递复盘模块，支持记录面试问题和复盘建议。",
                "## 需求摘要\n\n为 `ai-jd-analyzer` 增加投递复盘模块，沉淀面试问题、复盘建议和后续跟进行动。\n\n## 后端任务\n\n1. 新增面试复盘实体、DTO、Controller 和 Service。\n2. 支持按岗位、公司、投递记录查询复盘。\n3. 保存问题、回答情况、改进建议和下一步动作。\n\n## 前端任务\n\n1. 新增复盘列表和复盘详情面板。\n2. 支持快速录入面试问题和 AI 复盘建议。\n3. 增加复制给 Codex/Claude 的改进 Prompt。\n\n## 验收标准\n\n- 可以创建、查看、编辑复盘记录。\n- 可以从投递记录进入复盘。\n- 所有 AI 建议必须人工确认后保存。", GenerationStatus.READY_FOR_REVIEW, false, 188L);

        seedRecord(2L, GenerationType.CODE_PLAN, "ai-jd-analyzer 修改计划",
                "为投递复盘模块输出代码修改计划。",
                "## 目标\n\n在 `ai-jd-analyzer` 中加入投递复盘能力，保持现有 JD 分析和投递记录流程不被破坏。\n\n## 涉及文件\n\n- `InterviewReviewController.java`\n- `InterviewReviewService.java`\n- `InterviewReviewMapper.java`\n- `frontend/src/views/review/InterviewReviewView.vue`\n- `sql/interview_review.sql`\n\n## 修改步骤\n\n1. 设计 `interview_review` 表。\n2. 增加 CRUD 和按投递记录查询接口。\n3. 前端增加复盘录入区和 AI 建议区。\n4. 生成 Commit Message 和 README 片段。\n\n## 验证方式\n\n```bash\nmvn clean package\nnpm run build\n```\n", GenerationStatus.SAVED, false, 236L);

        seedRecord(2L, GenerationType.README_GENERATE, "ai-jd-analyzer README 片段",
                "生成投递复盘模块 README 片段。",
                "## 投递复盘模块\n\n投递复盘模块用于记录面试问题、回答表现、复盘建议和下一步行动。该模块不自动生成求职结论，只提供结构化记录和可复制的 AI 改进 Prompt。\n\n## MVP 边界\n\n- 支持复盘记录 CRUD。\n- 支持按公司、岗位、投递记录筛选。\n- AI 输出需要人工确认后保存。", GenerationStatus.CONFIRMED, true, 112L);

        seedRecord(2L, GenerationType.COMMIT_MESSAGE, "ai-jd-analyzer Commit Message",
                "生成投递复盘模块提交说明。",
                "## 推荐 Commit Message\n\n```text\nfeat(review): add interview review workflow\n```\n\n## 中文说明\n\n新增投递复盘模块，支持记录面试问题、复盘建议和下一步行动。\n\n## 变更摘要\n\n- 新增复盘数据结构和接口。\n- 新增复盘页面和 AI 建议展示。\n- 保留人工确认流程。", GenerationStatus.CONFIRMED, true, 76L);

        seedRecord(3L, GenerationType.LOG_ANALYSIS, "power-plant-system 端口占用",
                "Spring Boot 启动失败，8080 端口被占用。",
                "## 异常类型\n\nPort already in use\n\n## 可能原因\n\n`8080` 端口已被另一个 Java 进程占用，导致 Tomcat 无法启动。\n\n## 排查步骤\n\n1. 使用 `netstat -ano | findstr :8080` 查找进程。\n2. 使用任务管理器或 `taskkill` 结束无关进程。\n3. 或临时修改 `server.port`。\n\n## 修复 Prompt\n\n```text\n请定位 Spring Boot 端口占用问题，给出最小修复方案，不要改动无关配置。\n```", GenerationStatus.READY_FOR_REVIEW, false, 91L);

        seedRecord(1L, GenerationType.FIX_PROMPT, "DevFlow Workbench 修复 Prompt",
                "优化 Workbench 前端三栏布局，增强 AI 输出区的 Markdown 展示和复制体验。",
                "## 可复制给 Codex / Claude Code 的修复 Prompt\n\n```text\n你是资深 Vue3 + AI Coding 工具前端工程师。请优化 DevFlow Copilot 的 Workbench 三栏布局：左侧像 IDE 上下文面板，中间像任务编排器，右侧像 Artifact 输出区。要求 Markdown、代码块、复制、保存记录、人工确认状态都清晰可见。所有输出保持 review-only，由开发者确认后再使用。完成后运行 npm run build。\n```\n\n## 验收标准\n\n- 三栏布局稳定。\n- 输出区 Markdown 可读。\n- Copy Result、Save Record、Mark Confirmed 可用。", GenerationStatus.READY_FOR_REVIEW, false, 134L);

        seedLog(3L,
                "***************************\nAPPLICATION FAILED TO START\n***************************\n\nWeb server failed to start. Port 8080 was already in use.",
                "Port already in use",
                "Spring Boot 内置 Tomcat 绑定 8080 失败，通常是已有 Java 服务占用端口。",
                "1. 查询 8080 占用进程。\n2. 结束无关进程或改用临时端口。\n3. 同步前端代理和 README 启动说明。",
                "请分析 Spring Boot 端口占用问题，给出不影响生产配置的最小修复方案。",
                "修改端口后要同步前端代理配置。",
                "Low");

        seedLog(3L,
                "org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'alarmService'\nCaused by: org.springframework.beans.factory.NoSuchBeanDefinitionException: No qualifying bean of type 'AlarmMapper' available",
                "BeanCreationException",
                "Spring Bean 创建失败，可能是 Mapper 扫描范围不包含 `AlarmMapper` 或接口未被注册。",
                "1. 查看最底层 caused by。\n2. 检查 @MapperScan 包路径。\n3. 检查 Mapper 接口和 XML 命名空间。\n4. 重新运行 mvn clean package。",
                "请定位 BeanCreationException 根因，优先检查 Mapper 扫描与 Bean 注册，不要大范围重构。",
                "不要只处理最外层 BeanCreationException，根因在 caused by。",
                "High");
    }

    private void seedRecord(
            Long projectId,
            String generationType,
            String inputSummary,
            String inputContent,
            String outputContent,
            GenerationStatus status,
            Boolean confirmed,
            Long costTimeMs
    ) {
        GenerationRecord record = new GenerationRecord();
        record.setProjectId(projectId);
        record.setGenerationType(generationType);
        record.setInputSummary(inputSummary);
        record.setInputContent(inputContent);
        record.setOutputContent(outputContent);
        record.setStatus(status);
        record.setConfirmed(confirmed);
        record.setModelName("local-rule-mvp");
        record.setCostTimeMs(costTimeMs);
        createRecord(record);
    }

    private void seedLog(
            Long projectId,
            String rawLog,
            String exceptionType,
            String possibleReason,
            String diagnoseSteps,
            String fixPrompt,
            String riskTips,
            String riskLevel
    ) {
        LogAnalysis logAnalysis = new LogAnalysis();
        logAnalysis.setProjectId(projectId);
        logAnalysis.setRawLog(rawLog);
        logAnalysis.setExceptionType(exceptionType);
        logAnalysis.setPossibleReason(possibleReason);
        logAnalysis.setDiagnoseSteps(diagnoseSteps);
        logAnalysis.setFixPrompt(fixPrompt);
        logAnalysis.setRiskTips(riskTips);
        logAnalysis.setRiskLevel(riskLevel);
        createLog(logAnalysis);
    }

    public ProjectContext createProject(ProjectContext project) {
        long id = projectId.incrementAndGet();
        LocalDateTime now = LocalDateTime.now();
        project.setId(id);
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        projects.put(id, project);
        return project;
    }

    public PromptTemplate createPrompt(PromptTemplate prompt) {
        long id = promptId.incrementAndGet();
        LocalDateTime now = LocalDateTime.now();
        prompt.setId(id);
        prompt.setCreatedAt(now);
        prompt.setUpdatedAt(now);
        prompts.put(id, prompt);
        return prompt;
    }

    public GenerationRecord createRecord(GenerationRecord record) {
        long id = recordId.incrementAndGet();
        record.setId(id);
        record.setCreatedAt(LocalDateTime.now());
        records.put(id, record);
        return record;
    }

    public LogAnalysis createLog(LogAnalysis logAnalysis) {
        long id = logId.incrementAndGet();
        logAnalysis.setId(id);
        logAnalysis.setCreatedAt(LocalDateTime.now());
        logs.put(id, logAnalysis);
        return logAnalysis;
    }

    private PromptTemplate seedPrompt(String key, String name, String type, String content) {
        PromptTemplate prompt = new PromptTemplate();
        prompt.setTemplateKey(key);
        prompt.setTemplateName(name);
        prompt.setTemplateType(type);
        prompt.setTemplateContent(content);
        prompt.setVariables("projectName,techStack,input,currentRequirement,codingRules");
        prompt.setEnabled(true);
        return prompt;
    }

    public Map<Long, ProjectContext> projects() {
        return projects;
    }

    public Map<Long, PromptTemplate> prompts() {
        return prompts;
    }

    public Map<Long, GenerationRecord> records() {
        return records;
    }

    public Map<Long, LogAnalysis> logs() {
        return logs;
    }
}
