CREATE DATABASE IF NOT EXISTS devflow_copilot DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- Legacy MySQL bootstrap reference only.
-- The authoritative schema is managed by Flyway under src/main/resources/db/migration.
USE devflow_copilot;

DROP TABLE IF EXISTS ai_task;
DROP TABLE IF EXISTS log_analysis;
DROP TABLE IF EXISTS generation_record;
DROP TABLE IF EXISTS prompt_template;
DROP TABLE IF EXISTS project_context;

CREATE TABLE project_context (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    project_name VARCHAR(128) NOT NULL COMMENT '项目名称',
    tech_stack VARCHAR(512) COMMENT '技术栈',
    readme_content MEDIUMTEXT COMMENT 'README 内容',
    directory_structure MEDIUMTEXT COMMENT '目录结构',
    current_requirement MEDIUMTEXT COMMENT '当前需求',
    coding_rules MEDIUMTEXT COMMENT '开发约束',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='项目上下文表';

CREATE TABLE prompt_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    template_key VARCHAR(128) NOT NULL COMMENT '模板唯一标识',
    template_name VARCHAR(128) NOT NULL COMMENT '模板名称',
    template_type VARCHAR(64) NOT NULL COMMENT '模板类型',
    template_content MEDIUMTEXT NOT NULL COMMENT '模板内容',
    variables VARCHAR(512) COMMENT '变量列表',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_template_key (template_key)
) COMMENT='Prompt 模板表';

CREATE TABLE generation_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    project_id BIGINT COMMENT '项目 ID',
    generation_type VARCHAR(64) NOT NULL COMMENT '生成类型',
    input_summary VARCHAR(512) COMMENT '输入摘要',
    input_content MEDIUMTEXT COMMENT '输入内容',
    output_content MEDIUMTEXT COMMENT '输出内容',
    status VARCHAR(64) NOT NULL COMMENT '状态',
    confirmed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否人工确认',
    model_name VARCHAR(128) COMMENT '模型名称',
    cost_time_ms BIGINT COMMENT '耗时毫秒',
    error_message MEDIUMTEXT COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_generation_project (project_id),
    INDEX idx_generation_type (generation_type),
    INDEX idx_generation_created_at (created_at)
) COMMENT='生成历史记录表';

CREATE TABLE log_analysis (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    project_id BIGINT COMMENT '项目 ID',
    raw_log MEDIUMTEXT NOT NULL COMMENT '原始日志',
    exception_type VARCHAR(128) COMMENT '异常类型',
    possible_reason MEDIUMTEXT COMMENT '可能原因',
    diagnose_steps MEDIUMTEXT COMMENT '排查步骤',
    fix_prompt MEDIUMTEXT COMMENT '修复 Prompt',
    risk_tips MEDIUMTEXT COMMENT '风险提示',
    risk_level VARCHAR(32) COMMENT '风险等级',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_log_project (project_id),
    INDEX idx_log_created_at (created_at)
) COMMENT='日志分析表';

CREATE TABLE ai_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    project_id BIGINT COMMENT '项目 ID',
    task_title VARCHAR(256) NOT NULL COMMENT '任务标题',
    task_type VARCHAR(64) NOT NULL COMMENT '任务类型',
    priority VARCHAR(32) NOT NULL DEFAULT 'Medium' COMMENT '优先级',
    status VARCHAR(64) NOT NULL DEFAULT 'Draft' COMMENT '状态',
    risk_level VARCHAR(32) NOT NULL DEFAULT 'Medium' COMMENT '风险等级',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_task_project (project_id),
    INDEX idx_task_status (status)
) COMMENT='AI 任务状态表';

INSERT INTO project_context (project_name, tech_stack, readme_content, directory_structure, current_requirement, coding_rules) VALUES
('DevFlow Copilot', 'Java 17, Spring Boot 3, MyBatis-Plus, MySQL, Vue 3, Vite, TypeScript', 'AI Coding workflow console MVP. Local rule generation first, LLM integration later.', 'backend/src/main/java/com/devflow/copilot\nfrontend/src/views\nfrontend/src/components', '优化 Workbench 前端三栏布局，增强 AI 输出区的 Markdown 展示和复制体验。', 'Every artifact is review-only until the developer explicitly confirms it.'),
('ai-jd-analyzer', 'Java 17, Spring Boot 3, MyBatis-Plus, MySQL, Vue 3, Vite, Axios', 'AI 求职 Agent，用于 JD 匹配、简历优化、投递记录和面试复盘。', 'backend/src/main/java/com/jd/agent\nfrontend/src/views/apply\nfrontend/src/views/review\nsql/init.sql', '给 AI 求职 Agent 增加投递复盘模块，支持记录面试问题和复盘建议。', '先做 MVP；输出修改计划和 Prompt；所有 AI 建议需要人工确认后使用。'),
('power-plant-system', 'Java 17, Spring Boot 3, MyBatis-Plus, MySQL, Redis, Vue 3', '电厂设备巡检与告警管理演示系统。', 'src/main/java/com/power/plant\nsrc/main/resources/application.yml\nfrontend/src/views/alarm', '排查 Spring Boot 启动失败，端口 8080 被占用或 BeanCreationException。', '先定位日志根因，再给最小修复；不修改生产配置；不提交真实数据库密码。');

INSERT INTO prompt_template (template_key, template_name, template_type, template_content, variables, enabled) VALUES
('requirement-split', '需求拆解模板', 'requirement', '将用户需求拆解为后端任务、前端任务、数据库任务、测试任务、风险点和验收标准。', 'projectName,techStack,input,currentRequirement,codingRules', 1),
('log-analysis', '日志分析模板', 'log', '识别 Java/Spring Boot 异常类型，输出可能原因、排查步骤、修复 Prompt 和风险提示。', 'projectName,techStack,rawLog', 1),
('code-plan', '代码修改计划模板', 'plan', '根据需求输出建议修改文件、修改原因、修改步骤、风险点、验证方式和回滚建议。', 'projectName,techStack,input', 1),
('readme-generate', 'README 生成模板', 'document', '生成项目简介、技术栈、启动方式、功能模块、接口说明和 MVP 边界。', 'projectName,techStack,input', 1),
('commit-message', 'Commit Message 模板', 'document', '生成 feat/fix/docs/refactor 格式提交说明、中文说明和变更摘要。', 'input', 1),
('fix-prompt', 'Codex/Claude 修复 Prompt 模板', 'prompt', '生成角色设定、项目背景、报错信息、修改范围、任务要求、验收标准和构建命令。', 'projectName,techStack,input,codingRules', 1);

INSERT INTO generation_record (project_id, generation_type, input_summary, input_content, output_content, status, confirmed, model_name, cost_time_ms) VALUES
(2, 'requirement-split', 'ai-jd-analyzer 投递复盘模块', '给 AI 求职 Agent 增加投递复盘模块，支持记录面试问题和复盘建议。', '## 需求摘要\n\n为 `ai-jd-analyzer` 增加投递复盘模块，沉淀面试问题、复盘建议和后续跟进行动。\n\n## 后端任务\n\n1. 新增面试复盘实体、DTO、Controller 和 Service。\n2. 支持按岗位、公司、投递记录查询复盘。\n3. 保存问题、回答情况、改进建议和下一步动作。\n\n## 前端任务\n\n1. 新增复盘列表和复盘详情面板。\n2. 支持快速录入面试问题和 AI 复盘建议。\n3. 增加复制给 Codex/Claude 的改进 Prompt。\n\n## 验收标准\n\n- 可以创建、查看、编辑复盘记录。\n- 可以从投递记录进入复盘。\n- 所有 AI 建议必须人工确认后保存。', 'Ready for Review', 0, 'local-rule-mvp', 188),
(2, 'code-plan', 'ai-jd-analyzer 修改计划', '为投递复盘模块输出代码修改计划。', '## 目标\n\n在 `ai-jd-analyzer` 中加入投递复盘能力，保持现有 JD 分析和投递记录流程不被破坏。\n\n## 涉及文件\n\n- `InterviewReviewController.java`\n- `InterviewReviewService.java`\n- `InterviewReviewMapper.java`\n- `frontend/src/views/review/InterviewReviewView.vue`\n- `sql/interview_review.sql`\n\n## 验证方式\n\n```bash\nmvn clean package\nnpm run build\n```', 'Saved', 0, 'local-rule-mvp', 236),
(2, 'readme-generate', 'ai-jd-analyzer README 片段', '生成投递复盘模块 README 片段。', '## 投递复盘模块\n\n投递复盘模块用于记录面试问题、回答表现、复盘建议和下一步行动。该模块不自动生成求职结论，只提供结构化记录和可复制的 AI 改进 Prompt。', 'Confirmed', 1, 'local-rule-mvp', 112),
(2, 'commit-message', 'ai-jd-analyzer Commit Message', '生成投递复盘模块提交说明。', '## 推荐 Commit Message\n\n```text\nfeat(review): add interview review workflow\n```\n\n## 中文说明\n\n新增投递复盘模块，支持记录面试问题、复盘建议和下一步行动。', 'Confirmed', 1, 'local-rule-mvp', 76),
(3, 'log-analysis', 'power-plant-system 端口占用', 'Spring Boot 启动失败，8080 端口被占用。', '## 异常类型\n\nPort already in use\n\n## 可能原因\n\n`8080` 端口已被另一个 Java 进程占用，导致 Tomcat 无法启动。\n\n## 排查步骤\n\n1. 使用 `netstat -ano | findstr :8080` 查找进程。\n2. 使用任务管理器或 `taskkill` 结束无关进程。\n3. 或临时修改 `server.port`。', 'Ready for Review', 0, 'local-rule-mvp', 91),
(1, 'fix-prompt', 'DevFlow Workbench 修复 Prompt', '优化 Workbench 前端三栏布局，增强 AI 输出区的 Markdown 展示和复制体验。', '## 可复制给 Codex / Claude Code 的修复 Prompt\n\n```text\n你是资深 Vue3 + AI Coding 工具前端工程师。请优化 DevFlow Copilot 的 Workbench 三栏布局：左侧像 IDE 上下文面板，中间像任务编排器，右侧像 Artifact 输出区。要求 Markdown、代码块、复制、保存记录、人工确认状态都清晰可见。所有输出保持 review-only，由开发者确认后再使用。完成后运行 npm run build。\n```\n\n## 验收标准\n\n- 三栏布局稳定。\n- 输出区 Markdown 可读。\n- Copy Result、Save Record、Mark Confirmed 可用。', 'Ready for Review', 0, 'local-rule-mvp', 134);

INSERT INTO log_analysis (project_id, raw_log, exception_type, possible_reason, diagnose_steps, fix_prompt, risk_tips, risk_level) VALUES
(3, '***************************\nAPPLICATION FAILED TO START\n***************************\n\nWeb server failed to start. Port 8080 was already in use.', 'Port already in use', 'Spring Boot 内置 Tomcat 绑定 8080 失败，通常是已有 Java 服务占用端口。', '1. 查询 8080 占用进程。\n2. 结束无关进程或改用临时端口。\n3. 同步前端代理和 README 启动说明。', '请分析 Spring Boot 端口占用问题，给出不影响生产配置的最小修复方案。', '修改端口后要同步前端代理配置。', 'Low'),
(3, 'org.springframework.beans.factory.BeanCreationException: Error creating bean with name alarmService\nCaused by: org.springframework.beans.factory.NoSuchBeanDefinitionException: No qualifying bean of type AlarmMapper available', 'BeanCreationException', 'Spring Bean 创建失败，可能是 Mapper 扫描范围不包含 AlarmMapper 或接口未被注册。', '1. 查看最底层 caused by。\n2. 检查 @MapperScan 包路径。\n3. 检查 Mapper 接口和 XML 命名空间。\n4. 重新运行 mvn clean package。', '请定位 BeanCreationException 根因，优先检查 Mapper 扫描与 Bean 注册，不要大范围重构。', '不要只处理最外层 BeanCreationException，根因在 caused by。', 'High');

INSERT INTO ai_task (project_id, task_title, task_type, priority, status, risk_level) VALUES
(1, '优化 Workbench 三栏布局', 'frontend', 'High', 'Ready for Review', 'Medium'),
(2, '设计投递复盘数据结构', 'backend', 'High', 'Saved', 'Low'),
(3, '排查 Spring Boot 启动失败', 'log-analysis', 'High', 'Ready for Review', 'High');
