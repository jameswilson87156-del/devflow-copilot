# DevFlow Copilot Resume Evidence

DevFlow Copilot 是一个面向 AI Coding / VibeCoding 场景的 Agentic Coding Workflow 控制台 MVP。它适合作为 Java 后端 / AI 应用开发实习项目展示，重点不是声称“做了完整智能体平台”，而是把 AI 生成链路做成可追踪、可降级、可复核、可测试的后端工程闭环。

## 可以写进简历的 5 条 bullet

- 基于 Spring Boot 3、MyBatis-Plus、Flyway 和 H2/MySQL Profile 设计 AI Coding Workflow 后端，按 Controller / Service / Mapper / Entity / DTO 分层实现生成记录、状态流转和审计查询接口。
- 设计 OpenAI-compatible Provider 抽象，默认使用无 Key 可演示的 `local-rule` Provider，真实 Provider 仅从环境变量读取 API Key，并在失败或未配置时降级到本地规则链路。
- 落地 Generation Trace 和 Agent Run Trace，记录 Prompt 版本、输入变量、渲染摘要、Provider、模型、延迟、步骤、工具调用和 Human Review 状态，支撑 AI 生成过程可解释。
- 实现轻量 Knowledge Base/RAG 闭环，支持知识文档创建、文本切片、关键词/简单相似度检索和生成引用返回，并在表结构中预留 `embedding_model`、`embedding_vector` 扩展字段。
- 补充 20 个 Spring Boot 集成测试覆盖 Prompt 渲染、Provider 降级、状态机、Trace、Agent Workflow、Knowledge 检索和 RAG 引用，前端通过 Vue 3 + TypeScript + Vite 构建验证并生成作品集截图证据。

## 不能写或需要谨慎写的 5 条边界

- 不能写“完整多 Agent Runtime”：当前是可解释的单次 workflow 审计记录，不包含多 Agent 调度、规划器、异步 worker 或自动工具执行。
- 不能写“已稳定接入真实大模型”：默认演示链路是 `local-rule`，OpenAI-compatible 代码路径已准备，真实调用需要用户通过环境变量配置 API Key 后手动验证。
- 不能写“生产级向量 RAG”：当前 Knowledge Base 是关键词/简单相似度检索，不是向量数据库；embedding 字段只是后续扩展点。
- 不能写“AI 自动改代码、自动提交 Git”：项目只生成 review-only Artifact，保存和确认必须由人工触发。
- 不能写“已生产上线或商业化”：当前是本地可运行、可截图、可面试讲解的作品集 MVP，部署方案是可执行计划，不代表已经线上运营。

## 面试官可能追问的 10 个问题和回答

### 1. 这个项目和普通 ChatGPT 套壳有什么区别？

普通聊天页主要是输入和输出。这个项目把一次 AI Coding 行为拆成 Prompt 模板、Provider 调用、Generation Trace、Agent Run Trace、Knowledge 引用和 Human Review，并把这些信息落库，方便回放和解释。我的重点是后端工作流和审计模型，而不是聊天 UI。

### 2. 没有 API Key 时怎么演示？

默认走 `local-rule` Provider。它不是 LLM 推理，而是本地规则/模板生成，但会完整经过 Prompt 渲染、生成记录、Trace、Agent Run、Knowledge 引用、保存和人工确认流程，所以没有 Key 也能稳定演示工程闭环。

### 3. 真实 Provider 是怎么接的？

后端有 `GenerationProvider` 抽象，`OpenAiCompatibleGenerationProvider` 调用兼容 `/v1/chat/completions` 的接口。`DEVFLOW_AI_API_KEY`、`DEVFLOW_AI_BASE_URL`、`DEVFLOW_AI_MODEL` 等配置只从环境变量读取，不写进代码和文档示例的真实值。

### 4. Provider 失败后系统会怎样？

如果开启 `DEVFLOW_AI_FALLBACK_TO_LOCAL=true`，真实 Provider 抛错或缺 Key 时会降级到 `local-rule`，并把 provider、model、status、latency 和错误摘要写入记录。这样演示不会因为外部模型不可用而中断。

### 5. Generation Trace 记录了什么？

它记录 Prompt 版本、输入变量、渲染后的 Prompt 摘要、Provider、模型、状态、耗时和错误信息。它不保存 API Key，也不保存 Authorization Header，目的是让每次生成结果可以被排查和复盘。

### 6. Agent Run Trace 是真正的多 Agent 吗？

不是。它是一次任务运行的可解释记录：包括任务接收、Prompt 渲染、Knowledge 检索、Provider 生成和人工确认等步骤。它的价值是审计和展示 workflow，不是自动多 Agent 协作调度。

### 7. Knowledge Base 为什么先做关键词检索？

对作品集 MVP 来说，先证明文档、chunk、检索、引用返回和生成链路整合更重要。关键词/简单相似度实现成本低、可测试、稳定；表结构预留 embedding 字段，后续可以接向量数据库。

### 8. 后端哪些地方体现工程化？

主要是分层结构、DTO 校验、统一响应、全局异常处理、状态机约束、Flyway migration、H2/MySQL profile、MyBatis-Plus Mapper、Provider 降级和集成测试。这些都是 Java 后端实习面试能展开讲的点。

### 9. 如何避免 AI 输出直接造成风险？

后端状态机要求生成结果先进入 `READY_FOR_REVIEW`，之后才能 `SAVED` 和 `CONFIRMED`。项目没有自动改代码、没有自动 Git commit、没有部署动作，所有 Artifact 都需要人工复核。

### 10. 下一步你会怎么优化？

我会优先做三件事：用真实 OpenAI-compatible Key 跑一次最小端到端验证；给前端补 1 到 2 个 Vitest 测试；对 Vite 构建的大 chunk 做路由懒加载和依赖拆包优化。这些都能继续增强可信度，而不是堆不必要的大功能。

## 当前证据材料

- 核心截图：`docs/images/dashboard-agentic.png`、`docs/images/workbench-running.png`、`docs/images/agent-run-trace.png`、`docs/images/knowledge-base-rag.png`
- 额外截图：`docs/images/prompt-studio.png`、`docs/images/human-review-trace-detail.png`
- Provider 验证手册：`docs/real-provider-verification.md`
- 部署计划：`docs/deployment-plan.md`
- 架构说明：`docs/architecture.md`
