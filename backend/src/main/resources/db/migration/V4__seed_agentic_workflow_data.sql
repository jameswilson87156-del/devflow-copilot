INSERT INTO knowledge_document (title, source_type, source_uri, content, chunk_count, metadata) VALUES
('DevFlow Copilot 项目边界', 'manual', 'docs/mvp-scope.md', 'DevFlow Copilot 默认使用 local-rule 本地规则生成，OpenAI-compatible Provider 只在配置 API Key 后调用真实模型。项目强调 Prompt 模板、Generation Trace、Agent Run Trace 和 Human Review，不自动修改代码、不自动提交 Git。', 2, 'seed=true'),
('Spring Boot 日志诊断规则', 'manual', 'docs/interview-guide.md', '日志分析当前基于关键词规则引擎识别 NullPointerException、SQLSyntaxErrorException、BeanCreationException、Port already in use 等常见异常。诊断结果应作为人工复核材料，不应声称为真实 LLM 根因推理。', 2, 'seed=true');

INSERT INTO knowledge_chunk (document_id, chunk_index, content, content_summary, keywords, embedding_model, embedding_vector) VALUES
(1, 0, 'DevFlow Copilot 默认使用 local-rule 本地规则生成，OpenAI-compatible Provider 只在配置 API Key 后调用真实模型。', 'local-rule 与 real provider 边界', 'local-rule,openai-compatible,api key,provider', NULL, NULL),
(1, 1, '项目强调 Prompt 模板、Generation Trace、Agent Run Trace 和 Human Review，不自动修改代码、不自动提交 Git。', 'AI Coding 工作流边界', 'generation trace,agent run,human review,git', NULL, NULL),
(2, 0, '日志分析当前基于关键词规则引擎识别 NullPointerException、SQLSyntaxErrorException、BeanCreationException、Port already in use 等常见异常。', '日志诊断规则范围', 'log,java,spring boot,exception,rule', NULL, NULL),
(2, 1, '诊断结果应作为人工复核材料，不应声称为真实 LLM 根因推理。', '日志诊断简历边界', 'human review,llm,boundary,diagnosis', NULL, NULL);
