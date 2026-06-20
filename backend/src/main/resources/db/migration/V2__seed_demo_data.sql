INSERT INTO project_context (project_name, tech_stack, readme_content, directory_structure, current_requirement, coding_rules) VALUES
('DevFlow Copilot', 'Java 17, Spring Boot 3, MyBatis-Plus, H2/MySQL, Vue 3, Vite, TypeScript', '面向 Java 开发者的 AI Coding 工作流控制台。', 'backend/src/main/java/com/devflow/copilot\nfrontend/src/views\nfrontend/src/components', '优化 AI Coding 生成、审查与历史追踪流程。', '所有生成结果必须经过人工确认后使用。'),
('ai-jd-analyzer', 'Java 17, Spring Boot 3, MyBatis-Plus, MySQL, Vue 3, Vite', 'AI 求职辅助项目。', 'backend/src/main/java/com/jd/agent\nfrontend/src/views', '增加投递复盘模块。', '先做 MVP，所有 AI 建议需要人工确认。'),
('power-plant-system', 'Java 17, Spring Boot 3, MyBatis-Plus, MySQL, Vue 3', '电厂设备巡检与告警管理系统。', 'src/main/java/com/power/plant\nfrontend/src/views/alarm', '排查 Spring Boot 启动失败。', '先定位日志根因，再给最小修复。');

INSERT INTO prompt_template (template_key, template_name, template_type, template_content, variables, enabled, is_default, version) VALUES
('requirement-split', '需求拆解模板', 'requirement-split', '你是 Java 全栈项目负责人。项目：{{projectName}}；技术栈：{{techStack}}。请将需求“{{requirement}}”拆解为后端、前端、数据库、测试、风险与验收标准。补充上下文：{{context}}。', 'projectName,techStack,requirement,context', TRUE, TRUE, 1),
('code-plan', '代码修改计划模板', 'code-plan', '请为项目 {{projectName}} 制定可执行的代码修改计划。需求：{{requirement}}；技术栈：{{techStack}}；开发约束：{{codingRules}}；上下文：{{context}}。输出涉及文件、步骤、验证命令与回滚建议。', 'projectName,requirement,techStack,codingRules,context', TRUE, TRUE, 1),
('readme-generate', 'README 生成模板', 'readme-generate', '请为项目 {{projectName}} 生成 README 片段。技术栈：{{techStack}}；需求：{{requirement}}；上下文：{{context}}。输出项目简介、启动方式、核心功能、API 与 MVP 边界。', 'projectName,techStack,requirement,context', TRUE, TRUE, 1),
('commit-message', 'Commit Message 模板', 'commit-message', '请根据变更“{{requirement}}”生成 Conventional Commits 风格的 Commit Message、中文说明和变更摘要。项目：{{projectName}}；上下文：{{context}}。', 'requirement,projectName,context', TRUE, TRUE, 1),
('fix-prompt', '修复 Prompt 模板', 'fix-prompt', '请为项目 {{projectName}} 生成可交给 Codex 或 Claude Code 的修复 Prompt。问题：{{requirement}}；技术栈：{{techStack}}；约束：{{codingRules}}；上下文：{{context}}。必须包含最小修改范围和验收命令。', 'projectName,requirement,techStack,codingRules,context', TRUE, TRUE, 1),
('log-analysis', '日志分析模板', 'log-analysis', '你是 Java/Spring Boot 日志诊断工程师。项目：{{projectName}}；技术栈：{{techStack}}。分析日志：{{errorLog}}。输出异常类型、可能原因、排查步骤、修复 Prompt 和风险提示。', 'projectName,techStack,errorLog', TRUE, TRUE, 1);

INSERT INTO generation_record (project_id, generation_type, input_summary, input_content, output_content, status, confirmed, provider_name, model_name, prompt_template_id, prompt_template_name, prompt_template_version, rendered_prompt, prompt_tokens, completion_tokens, total_tokens, cost_time_ms, success) VALUES
(2, 'requirement-split', 'ai-jd-analyzer 投递复盘模块', '给 AI 求职 Agent 增加投递复盘模块。', '## 需求摘要\n\n为 ai-jd-analyzer 增加投递复盘模块。\n\n## 验收标准\n\n所有 AI 建议必须人工确认后保存。', 'READY_FOR_REVIEW', FALSE, 'local-rule', 'local-rule-mvp', 1, '需求拆解模板', 1, '本地规则演示 Prompt', 68, 126, 194, 188, TRUE),
(2, 'code-plan', 'ai-jd-analyzer 修改计划', '为投递复盘模块输出代码修改计划。', '## 修改计划\n\n增加实体、Service、Controller、前端录入区和测试。', 'SAVED', FALSE, 'local-rule', 'local-rule-mvp', 2, '代码修改计划模板', 1, '本地规则演示 Prompt', 55, 102, 157, 236, TRUE),
(2, 'readme-generate', 'ai-jd-analyzer README 片段', '生成投递复盘模块 README 片段。', '## 投递复盘模块\n\n用于记录面试问题、复盘建议和下一步行动。', 'CONFIRMED', TRUE, 'local-rule', 'local-rule-mvp', 3, 'README 生成模板', 1, '本地规则演示 Prompt', 42, 73, 115, 112, TRUE),
(2, 'commit-message', 'ai-jd-analyzer Commit Message', '生成投递复盘模块提交说明。', '## Commit Message\n\nfeat(review): add interview review workflow', 'CONFIRMED', TRUE, 'local-rule', 'local-rule-mvp', 4, 'Commit Message 模板', 1, '本地规则演示 Prompt', 31, 44, 75, 76, TRUE),
(3, 'log-analysis', 'power-plant-system 端口占用', 'Spring Boot 启动失败，8080 端口被占用。', '## 异常类型\n\nPort already in use\n\n## 排查步骤\n\n查询占用进程并同步前端代理配置。', 'READY_FOR_REVIEW', FALSE, 'local-rule', 'local-rule-mvp', 6, '日志分析模板', 1, '本地规则演示 Prompt', 64, 98, 162, 91, TRUE),
(1, 'fix-prompt', 'DevFlow Workbench 修复 Prompt', '优化 Workbench 三栏布局。', '## 修复 Prompt\n\n请优化 Workbench 布局并保持人工确认流程。', 'READY_FOR_REVIEW', FALSE, 'local-rule', 'local-rule-mvp', 5, '修复 Prompt 模板', 1, '本地规则演示 Prompt', 51, 82, 133, 134, TRUE);

INSERT INTO log_analysis (project_id, raw_log, exception_type, possible_reason, diagnose_steps, fix_prompt, risk_tips, risk_level) VALUES
(3, 'Web server failed to start. Port 8080 was already in use.', 'Port already in use', '8080 端口已被其他进程占用。', '查询占用进程；结束无关进程或修改端口；同步代理配置。', '请定位端口占用并给出最小修复方案。', '修改端口后要同步前端代理。', 'Low'),
(3, 'BeanCreationException caused by NoSuchBeanDefinitionException: AlarmMapper', 'BeanCreationException', 'Mapper 扫描范围不正确或接口未注册。', '检查 caused by、MapperScan 和 XML namespace。', '请优先检查 Mapper 扫描与 Bean 注册。', '不要只处理最外层异常。', 'High');
