# DevFlow Copilot — 项目 TODO

> 阅读规则（AGENTS.md 要求）：每轮开始前先读本文件，了解当前已完成情况和待处理任务，再读 HANDOFF.md 了解最近一轮审查或修复记录。

## 项目当前定位

面向大三实习简历的 **Agentic Coding Workflow 控制台 / Java 后端工程化项目**。

核心价值在于把 AI Coding 行为工程化：Prompt 模板渲染、Provider 抽象与降级、Generation Trace、Agent Run Trace、Knowledge Base 引用、生成历史持久化、状态机约束、人工确认闭环，而不是聊天套壳或完整生产级大模型平台。

目标投递方向：Java 后端实习、AI 工具开发、Java + AI 应用。

---

## 当前已完成能力

以下内容均有代码或文档证据，面试时可直接展示：

- **Spring Boot 3 + Java 17** 后端，`controller / service / service.impl / provider / mapper / entity / dto` 分层清晰
- **MyBatis-Plus 持久化 + Flyway 数据库迁移**（V1 建表5张，FK 约束完整）
- **H2（dev/test）/ MySQL（prod）Profile** 双数据库支持，`mvn test` 使用内存 H2 隔离
- **Generation 状态机**：`GENERATING → READY_FOR_REVIEW → SAVED → CONFIRMED`，非法流转返回 HTTP 409
- **Prompt 模板变量渲染**：正则替换 `{{variable}}`，缺填报 `TemplateRenderException`，版本写入 `generation_record`
- **Generation Provider 抽象**：`LocalRuleGenerationProvider`（无 Key 演示）+ `OpenAiCompatibleGenerationProvider`（真实 HTTP 调用 `/v1/chat/completions`）+ `GenerationProviderRouter`（含 fallback 逻辑）
- **Generation Trace**：`generation_trace` 记录 promptVersion、inputVariables、renderedPrompt 摘要、provider、model、status、latencyMs 和 errorMessage，不保存 API Key
- **Agent Run Trace**：`agent_run`、`agent_step`、`tool_call_record`、`human_review` 记录一次生成的任务拆解、Prompt 渲染、Knowledge 检索、Provider 工具调用和人工确认状态
- **轻量 Knowledge Base / RAG 引用**：支持创建文档、自动切片、关键词/简单相似度检索，并在生成响应中返回命中的 chunk 引用；embedding 字段仅为后续扩展预留
- **日志诊断规则引擎**：识别8种 Java/Spring Boot 常见异常关键词，输出排查步骤和修复 Prompt（规则引擎，不是 LLM 推理）
- **DTO 校验 + GlobalExceptionHandler**，统一 `ApiResponse` 响应结构
- **20 个 JUnit 5 / MockMvc / `@SpringBootTest` 集成测试**（`@Transactional` 隔离，`@ActiveProfiles("test")`），最近一次 `mvn test`：`Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`
- **ai_task 最小只读查询入口**：`GET /api/tasks?projectId={projectId}`，复用 `AiTaskMapper`，按 ID 倒序返回指定项目任务列表
- **Vue 3 + TypeScript + Element Plus** 前端，8 个真实页面路由，Dashboard / Workbench / Trace Evidence / Human Review 已升级为中文 AI Coding Workbench / Agentic Workflow Console 展示页，`npm run build` 通过
- **截图**：`docs/images/` 下 13 张本地真实浏览器截图均存在；canonical 核心截图为 `dashboard.png`、`workbench.png`、`trace-evidence.png`、`human-review.png`，另保留现有 README 兼容命名和 5 张 1920px 历史大图
- **Docker Compose** 配置存在，`docker compose config` 通过（三服务：mysql / backend / frontend）
- **后端宿主端口避让**：已从 `8080` 改为 `${BACKEND_HOST_PORT:-18080}:8080`，避免与现有服务冲突
- **GitHub Actions CI** 配置存在，包含后端 `mvn -B verify` 和前端 `npm ci && npm run build`
- **环境审计文档**：`docs/ENVIRONMENT_CHECK.md` 已记录本机工具版本和 Docker Compose 验证记录
- **Git 仓库**：当前作品集升级分支为 `feat/portfolio-showcase-v1`，协作规则已提交
- **README 简历版项目介绍**：已新增“简历版项目介绍”短小节，面向大三 Java 后端实习 / AI 工具开发 / Java + AI 应用投递场景，并保持项目真实边界

---

## 当前不能夸大的能力

以下内容**不能写进简历，也不能在面试中声称为已实现**：

| 不能声称 | 真实情况 |
|---|---|
| 接入了真实大模型 | 默认 `local-rule` 生成的是结构化模板 boilerplate，不是 LLM 推理结果；OpenAI-compatible 代码已实现但未用真实 Key 端到端验证 |
| 完整多 Agent Runtime | 当前是可解释的单次 Agent Workflow 记录闭环，不做多 Agent 调度、自动工具执行或异步 Worker |
| 生产级 RAG / 向量数据库 | 当前 Knowledge Base 是关键词/简单相似度检索，`embedding_model`、`embedding_vector` 是扩展预留字段 |
| AI 智能日志分析 | 日志诊断是关键词规则引擎（8种异常类型硬编码匹配），不是 LLM 推理 |
| 毫秒级 latency 追踪 | local-rule 的 `costTimeMs` 实测约为 0–1ms，不代表 LLM 网络延迟 |
| token 精确统计 | `LocalRuleGenerationProvider.estimateTokens()` = `text.length() / 3.5`，是字符数估算，不是真实 tokenizer |
| SSE 流式输出 | 当前是同步请求，无 SSE |
| Docker Compose 部署成功 | `docker compose up --build` 因 Docker Hub 镜像拉取网络超时未完成，后端/前端容器未创建；不能写成"已容器化部署" |
| InMemoryStore 是默认生产持久化 | `InMemoryStore` 仅用于 `memory-demo` 或无数据库轻量演示场景，不是默认 dev/test/prod 主流程持久化方案；默认主流程应使用 MyBatis-Plus + H2/MySQL |

---

## P0 — 必须处理（当前已闭环）

### P0-1：创建 TODO.md ✅ 本轮完成

- 状态：**done**（本文件即为 P0-1 的产出）
- 完成时间：2026-06-21

### P0-2：处理 ai_task 表 / Mapper 空壳 ✅ 本轮完成

- 状态：**done**
- 完成提交：`bce0368 feat: add ai task query endpoint`
- 完成内容：
  - 新增 `GET /api/tasks?projectId={projectId}`
  - 新增 `AiTaskController`
  - 新增 `AiTaskService`
  - 新增 `AiTaskServiceImpl`
  - 复用 `AiTaskMapper`
  - 增加 `ControllerAndMapperIntegrationTest` 覆盖
- 接口行为：
  - `projectId` 必填
  - 缺失 `projectId` 返回 HTTP 400，统一错误结构 `code=4000`
  - 返回 `ApiResponse<List<AiTask>>`
  - 按 `id` 倒序返回指定项目的 `ai_task` 列表
  - 不存在项目或无任务时返回空数组
- 未做内容：
  - 没有新增、更新、删除接口
  - 没有任务调度
  - 没有异步执行
  - 没有接入真实 LLM
  - 没有改生成、日志分析、模板管理、历史记录主流程
  - 没有改 `InMemoryStore`
- 验证结果：`mvn test` 通过，`Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`

#### P0-2 只读复核结论

- P0-2 已解决。
- 实现符合 Controller -> Service -> Mapper 分层风格。
- 返回结构沿用 `ApiResponse<List<AiTask>>`。
- 参数校验与现有 `GlobalExceptionHandler` 风格一致。
- 测试覆盖正常查询、空结果、缺失 `projectId` 三类场景。
- 未发现高风险。
- 低风险：当前排序使用 `orderByDesc(AiTask::getId)`，与部分历史查询使用 createdAt 倒序略有风格差异，但功能正确，可解释为 ID 自增近似创建顺序。
- 低风险：测试中 `projectId=2L` 依赖 seed 数据，当前与项目测试惯例一致。

---

## P1 — 建议优化（提升工程质量和面试表现）

### P1-1：InMemoryStore demo-only 说明 ✅ 已完成

- 状态：**done**
- 完成提交：`9babb4f docs: clarify InMemoryStore demo profile boundary`
- 完成内容：仅修改 `InMemoryStore.java` 类头 Javadoc，明确它只用于 `memory-demo` profile 或无数据库轻量演示场景。
- 持久化边界：`InMemoryStore` 不是默认 dev/test/prod 主流程持久化方案；默认主流程应使用 MyBatis-Plus + H2/MySQL。
- 未做内容：没有修改任何业务逻辑；没有修改字段、方法、注解或 profile 配置；没有再次扩展内存存储能力。
- 验证说明：未运行 `mvn test`，原因是本次只修改 Javadoc 注释，不改变运行行为。
- 验收：类头 Javadoc 清晰，阅读代码不再产生“内存存储是默认生产持久化”的歧义。

### P1-2：README 简历版项目介绍 ✅ 已完成

- 状态：**done**
- 完成提交：`0abd047 docs: add resume-oriented project summary`
- 完成内容：`README.md` 已在“项目定位”之后、“技术栈”之前新增“简历版项目介绍”小节。
- 完成边界：仅新增 4 句话的短小节，没有重写 README，没有修改 Java / Vue 业务代码。
- 表达重点：突出 Java 后端工程化能力，包括 Spring Boot 3、REST API、Controller -> Service -> Mapper 分层、MyBatis-Plus、Flyway、H2/MySQL Profile、统一响应、状态流转和后端测试；也突出 Prompt 模板、生成记录、人工确认、local-rule 演示生成链路与 OpenAI-compatible Provider 代码层适配。
- 边界保持：没有把项目夸大为生产级大模型平台；继续说明 `local-rule` 不是真实 LLM 推理，日志分析不是 AI 自动推理，tokenUsage 是估算，Docker runtime 未完整成功，`ai_task` 不是完整任务系统，`InMemoryStore` 不是默认主流程持久化。

### P1-3：只读审查 README.md、HANDOFF.md、TODO.md 和最近提交历史

- 状态：**待处理**
- 背景：P1-1 和 P1-2 已完成后，需要确认文档状态、提交历史和边界表达是否一致。
- 涉及文件：只读审查 `README.md`、`HANDOFF.md`、`TODO.md` 和最近提交历史，下一轮先不直接修改文件。
- 审查重点：文档是否一致、是否还有夸大表述、是否适合进入最终验收报告阶段。
- 不可扩大：不要继续扩功能；不要运行 Docker；不要把 Docker runtime 写成完整部署成功。
- 验收：输出只读审查结论；如果建议进入最终验收报告阶段，应作为后续单独任务处理。

### 已完成：README 补充 local-rule 和 token 估算说明（原 P1-2）

- 状态：**done**
- 背景：README 描述了 token 记录和 local-rule 模式，但未说明 token 是估算值、local-rule 生成内容是模板 boilerplate
- 涉及文件：`README.md`（仅相关段落）
- 方案：在 LLM Provider 配置或核心功能段落补充两句话：① local-rule token 为字符数估算；② local-rule 生成结构化模板内容，非 LLM 推理
- 不可破坏：README 整体结构和简历描述部分不变
- 验收：描述与实现一致；不引入任何虚构功能
- 完成提交：`5908a7b docs: clarify resume README boundaries`

### 协作记录维护：HANDOFF.md 持续记录 Claude 审查和 Codex 修复结果

- 状态：**进行中**（Claude 简历版只读审查已追加，Codex 修复结果待填写）
- 背景：每轮修复后，Codex 应在 HANDOFF.md 的"Codex 修复结果（待填写）"区填写实际结果
- 涉及文件：`HANDOFF.md`
- 验收：每个 Codex 任务完成后"待填写"区有真实记录

### P1-4：补充面试 Q&A 覆盖当前遗留问题

- 状态：**待处理**
- 背景：`docs/interview-guide.md` 已有基础 Q&A，但缺少对 ai_task 空壳、token 估算、Docker 超时的准确答法
- 涉及文件：`docs/interview-guide.md`
- 方案：追加以下 Q&A：
  - Q：ai_task 这张表是干嘛的？
  - Q：token 是怎么算的？
  - Q：Docker 部署跑过吗？
- 建议 commit message：`docs: add Q&A for ai_task, token estimation, Docker status`

### P1-5：Agentic Coding Workflow 控制台最小闭环 ✅ 本轮完成

- 状态：**done**
- 背景：用户要求把 DevFlow Copilot 从本地规则演示升级为可运行、可截图、可解释、可面试的 Agentic Coding Workflow 控制台。
- 涉及文件：后端 schema/entity/mapper/service/controller/test，前端 types/api/router/views，README、docs、TODO、HANDOFF。
- 完成内容：
  - 新增 `generation_trace`
  - 新增 `agent_run`、`agent_step`、`tool_call_record`、`human_review`
  - 新增 `knowledge_document`、`knowledge_chunk`、`generation_knowledge_reference`
  - 生成链路自动写入 Trace、Agent Run、Step、Tool Call、Human Review 和 Knowledge 引用
  - 新增 `GET /api/generation-traces`、`GET /api/agent-runs/{id}/trace`、`POST /api/knowledge/documents`、`POST /api/knowledge/search` 等接口
  - 新增前端 Agent Run Trace 页面与 Knowledge Base 页面，Prompt 模板页升级为 Prompt Studio 试运行，Dashboard 增加运行数、成功率、人工确认数、平均耗时等指标
  - 新增 `docs/resume-evidence.md` 和 `docs/deployment-plan.md`
- 未做内容：
  - 没有接入真实 API Key
  - 没有实现向量数据库
  - 没有实现复杂多 Agent 调度
  - 没有自动修改代码或自动 Git 提交
- 验证结果：
  - `cd backend && mvn test`：20 tests passed
  - `cd frontend && npm run build`：通过；仍有既有大 chunk 警告

---

## P2 — 有时间再做（加分项）

| 编号 | 任务 | 说明 |
|---|---|---|
| P2-1 | 真实 LLM 端到端验证 - 已完成一次最小验证 | 2026-06-26 在本机 `8080` 后端跑通 `openai-compatible` 最小生成请求，记录见 `docs/real-provider-verification.md`；未记录或泄露 API Key |
| P2-2 | 前端 Vitest 最小测试 | 为 StatusTag 或 WorkbenchView 计算属性添加 1–2 个 Vitest 测试 |
| P2-3 | Docker compose up --build 网络条件好时重跑 | 换镜像源或等网络恢复后重新验证，成功后补充进 HANDOFF.md 和 ENVIRONMENT_CHECK.md |
| P2-4 | 30 秒演示 GIF | 录制 Workbench 生成 → 保存 → 确认流程，放入 README |
| P2-5 | 前端 chunk 体积优化 | 做路由级 dynamic import，解决 `npm run build` 的 500 kB 体积警告 |

---

## 下一轮建议任务

**建议下一轮：为新增 Agent Run Trace / Knowledge Base 页面补截图或录制 30 秒演示 GIF。**

下一轮仍然只处理一个明确任务。建议先补演示证据，不继续扩大后端能力。

审查重点是截图路径真实存在、README 只引用真实文件、不把 keyword RAG 写成向量数据库、不把 Agent Run Trace 写成完整多 Agent Runtime。

边界要求：不要提交真实 API Key；不要运行产生费用的真实模型调用；不要把 Docker runtime 写成完整部署成功；继续保持 `local-rule`、日志规则、tokenUsage、`ai_task`、`InMemoryStore`、Knowledge Base 和 Agent Run Trace 的真实边界。

---

## 任务格式规范

后续每个任务按以下格式记录：

```
### 任务名称
- 优先级：P0 / P1 / P2
- 背景：为什么需要做这个
- 涉及文件：列出会修改的文件
- 不可破坏：本轮不能破坏哪些行为（通常包含：mvn test 15个通过）
- 验收方式：可操作的检查步骤（可包含 checkbox）
- 建议 commit message：feat/fix/docs: 简短说明
```

---

## P1-6：作品集截图证据与真实 Provider 验证准备 ✅ 本轮完成

- 状态：**done**
- 背景：完成 Agentic Coding Workflow 最小闭环后，需要把项目整理成可截图、可验证、可写简历的作品集证据。
- 涉及文件：`scripts/capture-portfolio-screenshots.mjs`、`frontend/package.json`、`README.md`、`docs/real-provider-verification.md`、`docs/resume-evidence.md`、`docs/images/*.png`、`HANDOFF.md`。
- 完成内容：
  - 新增作品集截图脚本，通过真实后端 API 预热安全 demo 数据，再从真实浏览器页面截图。
  - 生成 6 张截图：Dashboard、Workbench、Agent Run Trace、Knowledge Base、Prompt Studio、Human Review / Trace 详情。
  - README 顶部加入项目定位和 4 张核心截图，截图章节改为当前真实文件路径。
  - 新增真实 Provider 验证手册，说明环境变量、启动方式、请求示例、fallback 和 API Key 泄露检查。
  - 重写简历证据文档，补充 5 条后端架构感 bullet、5 条边界和 10 个面试追问回答。
- 边界：
  - 未提交真实 API Key。
  - 未执行真实模型付费调用。
  - 未实现自动改代码、自动 Git 提交、登录权限、SSE、向量数据库或完整多 Agent Runtime。
  - Knowledge Base 仍是关键词/简单相似度检索。
- 验收：本轮运行 `mvn test`、`npm run build`、`git diff --check`、截图存在性检查和敏感文件/密钥检查。

## P1-7：前端 UI 中文版重构前审查与分阶段计划 ✅ 本轮完成

- 状态：**done**
- 背景：用户提供 5 张 AI-generated 中文 UI concept images，要求先审查现有真实前端、接口、截图脚本和风险，不要立刻大改。
- 涉及文件：审查 `frontend/src` 页面、路由、API、类型、布局和样式，审查 `scripts/capture-portfolio-screenshots.mjs`、README 和相关 docs；本轮仅写回 `TODO.md`、`HANDOFF.md`。
- 审查结论：
  - 当前前端已具备真实接口闭环，不是静态概念图。
  - `docs/design/references/` 是 AI 视觉参考，不是运行截图；真实截图仍应来自 `docs/images/`。
  - 工作区已有未提交的 `AgentRunTraceView.vue` 视觉改动和未跟踪 `docs/design/`，后续修改必须保留并基于现状继续。
  - 参考图目标比现有页面更完整，不能一次性把五页重构混成一个不可验收大任务。
- 最小后续任务建议：
  1. 先统一导航中文结构、全局设计 token 和公共组件骨架。
  2. 单独改造 Dashboard，并重新生成真实截图。
  3. 单独改造 Workbench，并重新生成真实截图。
  4. 单独补强 Agent Run Trace 的底部 Tool Call / JSON / 状态历史 / 日志区域。
  5. 单独补强 Knowledge Base 和 Prompt Studio 的详情、历史和右侧预览区域。
- 验证结果：`cd frontend && npm run build` 通过；仍有既有 VueUse PURE 注释提示和 Element Plus/Markdown 大 chunk 警告。后端本轮未修改，未运行 `mvn test`。
- 建议 commit message：`docs: record frontend ui redesign audit`

## P1-8：前端设计系统第一阶段地基 ✅ 本轮完成

- 状态：**done**
- 背景：在不一次重构 5 个页面的前提下，为 DevFlow Copilot 中文版 AI SaaS 控制台建立低风险前端设计系统地基。
- 涉及文件：`frontend/src/router/index.ts`、`frontend/src/layouts/DevFlowLayout.vue`、`frontend/src/styles/theme.css`、`frontend/src/views/DashboardView.vue`、`frontend/src/components/*.vue`、`TODO.md`、`HANDOFF.md`。
- 完成内容：
  - 左侧导航统一为中文分组结构，包含工作流、可观测性、知识与引用、治理与审核、配置。
  - 保留已有真实路由；未实现独立页面的 `工具调用`、`人工审核`、`Provider`、`设置` 以 disabled 导航项展示，不新增假页面。
  - 全局 design tokens 统一放在 `frontend/src/styles/theme.css`，包含深色背景、卡片背景、蓝紫主色、状态色、文本色、间距、字号、圆角、阴影、sidebar 宽度和 topbar 高度。
  - 新增公共组件骨架：`SidebarNav`、`TopBar`、`MetricCard`、`SectionCard`、`StatusBadge`、`ProviderBadge`、`CodeBlock`。
  - `DevFlowLayout` 接入 `SidebarNav` 和 `TopBar`；`DashboardView` 轻量接入 `MetricCard` 和 `SectionCard`。
- 未做内容：
  - 未改后端。
  - 未新增接口或假接口。
  - 未重构 Workbench、Agent Run Trace、Knowledge Base、Prompt Studio。
  - 未把 `docs/design/references/` 的 AI 概念图当作真实截图。
- 验证结果：`cd frontend && npm run build` 通过；仍有既有 VueUse PURE 注释提示和 Element Plus/Markdown 大 chunk 警告。
- 建议 commit message：`feat: add devflow frontend design system foundation`

## P1-9：Dashboard 高保真中文版重构 ✅ 本轮完成

- 状态：**done**
- 背景：基于 `docs/design/references/01-dashboard-ai-concept-cn.png`，只重构 Dashboard，不改 Workbench、Agent Run Trace、Knowledge Base、Prompt Studio 或后端。
- 涉及文件：`frontend/src/views/DashboardView.vue`、`TODO.md`、`HANDOFF.md`。
- 完成内容：
  - Dashboard 首屏改为深色 DevFlow Copilot Hero，包含标题、Agentic Coding 工作流控制台副标题、Prompt / Provider / Trace / Knowledge / Human Review 说明和小型 Agent Workflow Overview。
  - KPI 区使用 `MetricCard` 展示今日运行数、成功率、平均耗时、人工审核、工具调用、知识库命中。
  - 中部使用 `SectionCard` 组织最近智能体运行、启用中的 Prompt 模板、Provider 健康状态、最近人工审核、最新知识引用。
  - 右侧模块补充 Agent Workflow Overview、最近活动时间线和高频工具。
  - 复用 `MetricCard`、`SectionCard`、`StatusBadge`、`ProviderBadge`；未新增假接口。
- 真实接口：
  - `GET /api/dashboard/stats`
  - `GET /api/prompts`
  - `GET /api/logs/history`
  - `GET /api/agent-runs`
  - `GET /api/agent-runs/{id}/trace`
  - `GET /api/knowledge/documents`
  - `GET /api/knowledge/references?generationRecordId={id}`
- fallback / 派生说明：
  - `dashboard/stats` 不直接提供工具调用总数、知识命中总数、Provider 健康明细和 Human Review 列表；页面集中从现有 Trace / Knowledge 引用接口派生。
  - 缺失 provider/model/reviewer 时仅使用 `local-rule`、`local-rule`、`未分配` 等安全展示 fallback。
  - 缺失 Tool Call 或 Knowledge 引用时显示 0 或空状态，不伪造成后端真实统计。
- 未做内容：
  - 未改后端。
  - 未新增接口或假接口。
  - 未重构 Workbench、Agent Run Trace、Knowledge Base、Prompt Studio。
  - 未重新生成 `docs/images/` 真实截图；建议下一轮或统一截图任务执行。
- 验证结果：`cd frontend && npm run build` 通过；仍有既有 VueUse PURE 注释提示和 Element Plus/Markdown 大 chunk 警告。
- 建议 commit message：`feat: redesign dashboard with devflow design system`

## P1-10：Workbench 高保真中文版重构 ✅ 本轮完成

- 状态：**done**
- 背景：基于 `docs/design/references/02-workbench-ai-concept-cn.png`，只重构 Workbench，不改后端、不改 Dashboard、Agent Run Trace、Knowledge Base、Prompt Studio。
- 涉及文件：`frontend/src/views/WorkbenchView.vue`、`scripts/capture-portfolio-screenshots.mjs`、`TODO.md`、`HANDOFF.md`。
- 完成内容：
  - Workbench 改为左侧任务配置区、中间生成结果区、右侧执行详情区、底部追踪与日志区。
  - 左侧保留任务类型、项目、Prompt 模板、任务输入、补充上下文、知识检索等真实生成字段，并补充本页 UI-only 的任务标题、优先级、分支、上下文文件、关注区域、语言和约束展示。
  - 中间保留生成结果展示，新增预览 / 差异 / 原始 / JSON 视图；无真实 diff 时显示空状态，不伪造 diff。
  - 中间新增结构化响应和建议文件区；仅从响应 JSON 或响应文本安全派生，缺失字段显示 0 或空状态。
  - 右侧新增执行详情、知识引用、人工审核、工具调用摘要。
  - 底部新增生成追踪、工具调用、状态历史和日志，优先展示真实 Agent Run Trace / Generation Trace / Tool Call 数据。
  - 截图脚本 Workbench 等待选择器更新为兼容新结果区。
- 保留真实功能：
  - `运行工作流`
  - `保存记录`
  - `标记已审核 / 确认`
  - 项目选择、Prompt 模板选择、知识检索 query、补充上下文输入和生成历史刷新
- 真实接口：
  - `GET /api/projects`
  - `GET /api/generations`
  - `GET /api/prompts`
  - `POST /api/ai/*`
  - `POST /api/generations/{id}/save`
  - `POST /api/generations/{id}/confirm`
  - `GET /api/generation-traces?generationRecordId={id}`
  - `GET /api/agent-runs?generationRecordId={id}`
  - `GET /api/agent-runs/{id}/trace`
  - `GET /api/knowledge/references?generationRecordId={id}`
- fallback / 派生说明：
  - 后端未提供任务标题、优先级、分支、上下文文件、关注区域、语言等持久字段；本轮仅作为 Workbench 本页配置展示，不写入后端。
  - 后端不保证返回 diff、files_created、files_changed、tests_added、dependencies；页面只从响应 JSON 或文本安全派生，缺失时显示空状态。
  - Provider 降级策略文案来自既有 Provider Router 行为说明，不伪造成独立健康接口。
  - 日志区只展示真实 `errorMessage`；无错误时显示空状态。
- 未做内容：
  - 未改后端。
  - 未新增接口或假接口。
  - 未新增自动改代码、自动 Git 提交、SSE、登录权限。
  - 未重新生成 `docs/images/` 真实截图；建议下一轮做统一截图任务，或单独重构 Knowledge Base。
- 验证结果：`cd frontend && npm run build` 通过；仍有既有 VueUse PURE 注释提示和 Element Plus/Markdown 大 chunk 警告。
- 建议 commit message：`feat: redesign workbench with devflow design system`

## P1-11：Knowledge Base 高保真中文版重构 ✅ 本轮完成

- 状态：**done**
- 背景：基于 `docs/design/references/04-knowledge-base-ai-concept-cn.png`，只重构 Knowledge Base，不改后端、不改 Dashboard、Workbench、Agent Run Trace、Prompt Studio。
- 涉及文件：`frontend/src/views/KnowledgeBaseView.vue`、`TODO.md`、`HANDOFF.md`。
- 完成内容：
  - Knowledge Base 改为左侧文档列表、中间文档详情与 Chunk 切片、右侧检索结果详情与审核状态、底部检索历史 / 引用历史 / 生成知识引用 / 索引日志的高密度知识引用工作台。
  - 顶部明确展示 `文档 -> Chunk -> 检索 -> 引用 -> 生成 -> Trace` 链路，不做普通文档列表页。
  - 左侧保留搜索框、新增文档按钮、来源筛选、状态筛选和真实文档列表；文档选择后会重新加载真实 Chunk 并触发当前关键词检索。
  - 中间保留创建知识文档、加载文档 Chunk、关键词检索等真实功能；新增 Chunk 切片、命中结果、检索时间线、文档信息、访问控制分区。
  - 右侧展示当前命中 Chunk 的查询、来源、score、引用预览、生成使用情况和关联 Agent Run；没有 Trace / 审核记录时显示 `未关联 Trace`、`暂无知识发布审核记录`。
  - 底部可观测性区域展示本页会话检索历史、真实 `generation_knowledge_reference` 引用历史、按 Generation Record 聚合的知识引用和由文档 / Chunk 状态派生的索引记录。
- 保留真实功能：
  - `GET /api/knowledge/documents` 文档列表。
  - `POST /api/knowledge/documents` 新增文档并由后端切片。
  - `GET /api/knowledge/documents/{id}/chunks` 真实 Chunk 列表。
  - `POST /api/knowledge/search` 关键词 / 简单相似度检索。
  - `GET /api/knowledge/references?generationRecordId={id}` 生成知识引用记录。
- 额外使用真实接口：
  - `GET /api/generations` 用于采样最近 Generation Record。
  - `GET /api/agent-runs?generationRecordId={id}` 用于在引用历史中显示可关联的 Agent Run。
- fallback / 派生说明：
  - 后端未提供文档发布状态字段；状态优先从 `metadata status` 读取，否则按 `chunkCount > 0` 安全派生为 `已索引`，缺失时显示 `草稿 / 待审核`。
  - 后端未提供 Chunk Token 数、最近使用、检索日志持久化、索引耗时、知识发布审核人或审核意见；页面显示 `未记录` 或空状态，不伪造。
  - `embeddingModel / embeddingVector` 仅展示为预留扩展点；页面文案保持 `关键词检索 / RAG 引用`，没有写成向量数据库已启用。
  - 引用使用次数、首次 / 最近使用时间只从已读取的真实 generation knowledge references 派生，未命中时显示未记录。
- 截图脚本：
  - `scripts/capture-portfolio-screenshots.mjs` 的 Knowledge Base 路径仍是 `/knowledge`，等待选择器 `.knowledge-page` 仍存在，本轮不需要调整脚本。
  - 本轮未重新生成 `docs/images/` 作品集截图；建议后续统一截图任务执行。
- 验证结果：`cd frontend && npm run build` 通过；仍有既有 VueUse PURE 注释提示和 Element Plus/Markdown 大 chunk 警告。使用本机 Chrome 对 `/knowledge` 做过 DOM 烟测，左/中/右/底部四区渲染存在；临时验证截图已删除，未纳入本轮变更。
- 建议 commit message：`feat: redesign knowledge base with devflow design system`

## P1-12：Prompt Studio 高保真中文版重构 ✅ 本轮完成

- 状态：**done**
- 背景：基于 `docs/design/references/05-prompt-studio-ai-concept-cn.png`，只重构 Prompt Studio，不改后端、不新增假接口、不改 Dashboard、Workbench、Agent Run Trace、Knowledge Base。
- 涉及文件：`frontend/src/views/PromptTemplatesView.vue`、`scripts/capture-portfolio-screenshots.mjs`、`TODO.md`、`HANDOFF.md`。
- 完成内容：
  - Prompt Studio 改为左侧 Prompt 模板列表、中间 Prompt 编辑器与渲染预览、右侧测试运行预览与执行详情、底部版本历史 / 测试结果 / Prompt 校验 / 最近运行 / 变更记录。
  - 顶部明确展示 `Prompt 模板 -> 变量配置 -> 渲染预览 -> 测试运行 -> Trace / Human Review` 链路，不做普通文本编辑器或 CRUD 列表。
  - 左侧保留搜索、新建模板、类型筛选、状态筛选和真实模板列表；使用次数和最近运行从真实 generation records 派生，缺失时显示未记录。
  - 中间保留模板名称、Key、类型、版本、启用 / 默认状态、模板内容、variables 字段、保存模板、保存版本和运行测试；使用 `CodeBlock` 展示 System/User Prompt 派生预览、渲染后 Prompt 和 JSON 预览。
  - 右侧展示真实试运行状态、耗时、Trace ID、Provider、Model、Token、原始响应、查看 Generation Trace 和 Human Review 状态；缺失时显示空状态。
  - 底部 Prompt 校验只做前端静态校验：内容非空、变量占位符、必填变量覆盖、长度阈值、敏感占位符；不包装成 PromptOps、LLM-as-Judge 或自动评测后端能力。
- 保留真实功能：
  - `GET /api/prompts` 模板列表。
  - `POST /api/prompts` 新建模板。
  - `PUT /api/prompts/{id}` 保存模板；后端 update 会递增当前 `version`。
  - `POST /api/ai/{type}` 试运行支持已有 `requirement-split`、`code-plan`、`readme-generate`、`commit-message`、`fix-prompt` 类型。
- 额外使用真实接口：
  - `GET /api/projects` 用于测试项目和变量示例。
  - `GET /api/generations` 用于模板使用次数、最近运行和测试结果历史。
  - `GET /api/generation-traces?generationRecordId={id}` 用于 Trace ID、Provider、Model、耗时等可观测字段。
  - `GET /api/agent-runs?generationRecordId={id}` 与 `GET /api/agent-runs/{id}/trace` 用于关联 Agent Run 与 Human Review。
- fallback / 派生说明：
  - 后端没有独立版本历史表、PromptOps 评测表、单独 Prompt 校验记录、模板级使用次数或最近运行字段；页面只展示当前版本记录、静态校验、真实 generation 派生统计或空状态。
  - 后端没有独立渲染预览 API；页面本地按 `{{variable}}` 做预览，真实渲染仍由生成请求中的后端 `PromptTemplateRenderService` 完成。
  - `System Prompt` / `User Prompt` 没有独立后端字段；页面仅从 `templateContent` 中的标题片段安全推导，缺失时显示单字段说明。
  - `log-analysis` 可编辑保存，但当前 `generateAi` 没有对应生成端点，页面禁用真实试运行，不新增假接口。
- 截图脚本：
  - `scripts/capture-portfolio-screenshots.mjs` 的 Prompt Studio 路径仍是 `/prompts`，等待选择器 `.templates-page` 仍存在。
  - 本轮只把 Prompt Studio 试运行按钮匹配扩展为 `运行测试|试运行|运行模板|Test`；未重新生成 `docs/images/` 作品集截图。
- 验证结果：`cd frontend && npm run build` 通过；仍有既有 VueUse PURE 注释提示和 Element Plus/Markdown 大 chunk 警告。
- 建议 commit message：`feat: redesign prompt studio with devflow design system`

## P1-13：统一生成真实浏览器截图并更新 README ✅ 本轮完成

- 状态：**done**
- 背景：前端设计系统、Dashboard、Workbench、Knowledge Base、Prompt Studio 已完成高保真中文版重构；本轮只做作品集截图和 README 收口，不继续大改页面、不改后端核心逻辑、不新增接口。
- 涉及文件：`README.md`、`TODO.md`、`HANDOFF.md`、`docs/images/dashboard-agentic.png`、`docs/images/workbench-running.png`、`docs/images/agent-run-trace.png`、`docs/images/knowledge-base-rag.png`、`docs/images/prompt-studio.png`、`docs/images/human-review-trace-detail.png`。
- 截图脚本：
  - 使用既有 `frontend` 脚本 `npm run screenshots:portfolio`。
  - 本轮没有修改 `scripts/capture-portfolio-screenshots.mjs`。
  - 脚本确认覆盖 `/`、`/workbench`、`/agent-runs`、`/knowledge`、`/prompts`，并通过真实后端 API 预热 demo Agent Workflow。
- 实际生成截图：
  - `docs/images/dashboard-agentic.png`，1440x1040。
  - `docs/images/workbench-running.png`，1440x1040。
  - `docs/images/agent-run-trace.png`，1440x1040。
  - `docs/images/knowledge-base-rag.png`，1440x1040。
  - `docs/images/prompt-studio.png`，1440x1040。
  - `docs/images/human-review-trace-detail.png`，1440x1040。
- README 更新：
  - 顶部截图展示区补充 Prompt Studio 和 Human Review Trace。
  - 项目截图小节明确 `docs/images/` 是真实浏览器运行截图来源。
  - 新增说明：`docs/design/references/` 是 AI-generated visual references，不是 runtime screenshots，也不作为 README 主展示图来源。
  - 核心功能文案补充 Prompt Studio、Tool Call、Generation Trace / Human Review 关联；继续保持 local-rule、Knowledge Base 关键词检索、非完整多 Agent Runtime、非自动改代码系统等真实边界。
- 验证结果：
  - `cd backend && mvn test` 通过，`Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`。
  - `cd frontend && npm run build` 通过；仍有既有 VueUse PURE 注释提示和 Element Plus / Markdown 大 chunk 警告。
  - 遗留优化：后续可做前端 bundle split / manual chunks，降低大 chunk 警告。
- 建议 commit message：`docs: refresh devflow portfolio screenshots and readme`

## P1-14：Production Demo Readiness / 部署前安全收口 ✅ 本轮完成

- 状态：**done**
- 背景：今天准备部署服务器，本轮只做 portfolio demo 部署前安全收口，不继续重构页面、不改后端核心业务逻辑、不新增复杂功能。
- 涉及文件：`README.md`、`docs/deployment-plan.md`、`docs/deployment-production-demo.md`、`docs/env.example`、`docs/nginx/devflow-demo.conf.example`、`TODO.md`、`HANDOFF.md`。
- 完成内容：
  - 新增 `docs/deployment-production-demo.md`，说明项目定位、部署边界、推荐 Nginx + Spring Boot 架构、服务器部署步骤、环境变量、安全说明和验收 checklist。
  - 新增 `docs/env.example`，只包含安全占位符，说明公开演示可使用 `local-rule`，真实 Provider 必须通过服务器环境变量配置。
  - 新增 `docs/nginx/devflow-demo.conf.example`，使用占位 `server_name devflow.example.com`、占位前端 `dist` 路径，并将 `/api/` 反代到 `127.0.0.1:8080`；未写真实域名、服务器 IP 或证书私钥路径。
  - README 新增 `Portfolio Demo 部署` 小节，链接生产演示部署指南、env 示例和 Nginx 示例，并继续明确 `docs/images/` 是真实截图、`docs/design/references/` 不是运行截图。
  - 更新 `docs/deployment-plan.md`，把过期的“仓库还没有 env example”说明改为引用 `docs/env.example` 和新的 production demo 指南。
- 部署风险 / 边界记录：
  - 当前没有独立 `/api/health`，部署验收使用已有 `/api/dashboard/stats`。
  - `application-prod.yml` 的数据库默认值仅适合示例；服务器部署必须用环境变量覆盖 `DB_PASSWORD` 等敏感配置。
  - 前端生产环境依赖 Nginx `/api/` 反向代理；Vite proxy 仅用于本地开发。
  - 该部署方案是 portfolio demo，不是 production SaaS；不新增登录、权限、SSE、自动 Git 提交或完整多 Agent Runtime。
- 验证结果：
  - `cd backend && mvn test` 通过，`Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`。
  - `cd frontend && npm run build` 通过；仍有既有 VueUse PURE 注释提示和 Element Plus / Markdown 大 chunk 警告。
  - 后续可选优化：做前端 bundle split / manual chunks，进一步降低大 chunk 警告。
- 建议 commit message：`docs: add production demo deployment guide`

## P1-15：作品集化只读审查、公开参考调研与指标采集地基 ✅ 本轮完成

- 状态：**done**
- 背景：用户要求把项目进一步整理为适合简历、Boss 直聘、GitHub README 和面试展示的 AI 全栈开发作品；本轮先做只读审查、公开参考调研、参考图索引、指标方案和采集脚本，不直接进入正式 UI 大改。
- 涉及文件：`docs/portfolio_audit.md`、`docs/reference_research.md`、`docs/design_refs/README.md`、`docs/metrics_plan.md`、`docs/metrics/metrics_snapshot.md`、`docs/showcase_upgrade_plan.md`、`scripts/collect-portfolio-metrics.js`、`TODO.md`、`HANDOFF.md`。
- 完成内容：
  - 新增 `docs/portfolio_audit.md`，记录当前项目状态、已有优势、主要问题、展示风险、README 不一致点、前后端问题、可写和不可写简历数据。
  - 新增 `docs/reference_research.md`，实际联网调研 OpenHands、Langfuse、Arize Phoenix、Dify、Flowise、Promptfoo、AgentOps 等公开项目，只借鉴信息架构和指标思路，不复制代码、截图、Logo、样式或文案。
  - 新增 `docs/design_refs/README.md`，建立参考图索引；说明当前本目录无图片，已有参考图位于 `docs/design/references/`，且是 AI-generated concept images，不是运行截图。
  - 新增 `docs/metrics_plan.md`，把指标分为功能规模、工程质量、性能体验、AI 工作流效果 4 类，并说明采集命令、结果保存位置、简历可写边界和不能写的情况。
  - 新增 `scripts/collect-portfolio-metrics.js`，可静态采集页面、路由、接口、测试、migration、seed、截图、README 图片和基础敏感信息模式；传入 `--run-checks` 时执行 `npm run build` 和 `mvn test` 并统计 bundle。
  - 生成 `docs/metrics/metrics_snapshot.md`，记录本地采集快照。
  - 新增 `docs/showcase_upgrade_plan.md`，给出后续 README、Human Review / Trace Evidence 和指标化展示的分阶段计划；本轮未改正式页面。
- 当前已采集指标：
  - 前端页面文件：7 个。
  - 前端真实 component route：7 个，redirect route：1 个，disabled nav item：4 个。
  - 后端 Controller：10 个，endpoint mapping：30 个。
  - 后端测试源码中的 `@Test`：20 个。
  - Flyway migration：4 个，Prompt 模板 seed：6 条，Generation Record seed：6 条，Knowledge Document seed：2 条，Knowledge Chunk seed：4 条。
  - `docs/images` PNG 截图：11 张，README 图片引用：6 张。
- 保留边界：
  - 未接真实 API Key，未写入任何真实密钥。
  - 未把 `local-rule` 包装成真实 LLM 推理。
  - 未把 Knowledge Base 写成向量数据库。
  - 未把 Agent Run Trace 写成完整多 Agent Runtime。
  - 未修改后端业务逻辑、前端正式页面、数据库 schema 或生产依赖。
- 验证结果：
  - `node scripts/collect-portfolio-metrics.js --run-checks` 成功生成 `docs/metrics/metrics_snapshot.md`。
  - `cd frontend && npm run build` 由指标脚本执行通过；仍有既有 VueUse PURE 注释提示。
  - `cd backend && mvn test` 由指标脚本执行通过，20 tests passed。
- 后端默认 `http://127.0.0.1:8080/api/dashboard/stats` 未作为本轮服务启动验收采集成功，指标快照标记为“当前未采集 / 默认接口不可用（HTTP 404）”。
- 建议 commit message：`docs: add portfolio audit and metrics plan`

## P1-16：公开前端参考截图采集与索引 ✅ 本轮完成

- 状态：**done**
- 背景：用户要求补充公开前端参考截图，仅用于内部视觉分析；不登录、不绕过权限、不复用第三方截图、不进入前端代码改造。
- 涉及文件：`.gitignore`、`docs/frontend_reference_screenshot_index.md`、`TODO.md`、`HANDOFF.md`。
- 本地 ignored 产物：
  - `.local/capture-reference-screenshots.mjs`
  - `.local/reference_screenshots/*.png`
  - `.local/reference_screenshots/results.json`
- 完成内容：
  - 在 `.gitignore` 中补充 `.local/` 与 `.local/reference_screenshots/`，确保第三方参考截图不会进入 Git。
  - 使用本地 Playwright 自动化访问公开页面并截图，全部保存到 `.local/reference_screenshots/`。
  - 新增 `docs/frontend_reference_screenshot_index.md`，记录采集时间、联网状态、来源名称、URL、本地截图文件名、访问结果、适合参考的 DevFlow 页面、可借鉴点、不能照搬点和第一阶段采用结论。
- 实际采集结果：
  - 共 16 个公开目标页面，成功 16 个，失败 0 个，跳过 0 个。
  - 之所以生成到 `16_promptfoo_github.png`，是因为用户实际列出的公开来源共有 16 个。
  - 已采集来源包括：Ant Design Pro Analysis、Ant Design Pro Monitor、Arco Design、Semi Design、Google Cloud Console、Google Cloud Monitoring Dashboards、Huawei Cloud Management Console Overview、Alibaba Cloud SLS Dashboard、Langfuse、LangSmith Observability、Helicone、Arize Phoenix GitHub、AgentOps GitHub、Dify GitHub、Open WebUI GitHub、Promptfoo GitHub。
- 保留边界：
  - 未登录任何账号。
  - 未绕过权限、付费墙、验证码、反爬或访问限制。
  - 未把任何第三方截图复制到 `docs/images/` 或 README。
  - 未开始 Vue、CSS、README、截图脚本或前端页面改造。
- 验证结果：
  - `node .local/capture-reference-screenshots.mjs` 成功执行。
  - `.local/reference_screenshots/results.json` 记录 `networkAvailable=true`、`total=16`、全部 `status=success`。
  - `git status --short` 只显示 `.gitignore` 和 `docs/frontend_reference_screenshot_index.md` 为本轮可提交改动；`.local/` 截图目录未进入 Git。
- 建议 commit message：`docs: add frontend reference screenshot index`

## P1-17：第一阶段前端作品集化 UI 改造 ✅ 本轮完成

- 状态：**done**
- 背景：用户已确认前端参考调研、moodboard、公开截图索引、视觉落地规格和指标快照，可以开始第一阶段前端代码改造。
- 涉及文件：`frontend/src/styles/theme.css`、`frontend/src/router/index.ts`、`frontend/src/components/MetricCard.vue`、`frontend/src/components/SidebarNav.vue`、`frontend/src/components/TopBar.vue`、`frontend/src/views/DashboardView.vue`、`frontend/src/views/WorkbenchView.vue`、`frontend/src/views/AgentRunTraceView.vue`、`frontend/src/views/HumanReviewView.vue`、`frontend/src/views/KnowledgeBaseView.vue`、`frontend/src/views/PromptTemplatesView.vue`、`scripts/capture-portfolio-screenshots.mjs`、`docs/images/*.png`、`docs/metrics/metrics_snapshot.md`、`TODO.md`、`HANDOFF.md`。
- 完成内容：
  - 全局视觉系统切换为深色开发者工具风格、Runtime Mint 信号色、蓝黑 / 石墨黑背景、弱边框、低阴影、6px 圆角和高密度企业级控制台节奏。
  - App Shell 按 220px Sidebar、52px Topbar、24px page padding、16px panel padding / grid gap 落地；导航结构升级为工作流、可观测性、知识与引用、治理与审核、配置。
  - Provider / Settings 保持 disabled，并显示 `配置预留 / 当前通过环境变量配置`；未接真实 API Key。
  - Dashboard 增加 Product Evidence Hero、中文 KPI、Metrics Snapshot、Provider Health、最近运行、复核队列、执行证据和可见的数据来源声明。
  - Workbench 强化需求与上下文、Prompt 模板、Provider 状态、Knowledge References、Artifact Preview、Change Summary、Trace Summary、Tool Calls Timeline 和 Human Review 决策位；`Request Changes` 明确为后端未提供 API 的 disabled 控制。
  - Trace Evidence 重构为 Run list + Timeline / Steps / Spans + Step Inspector + Raw JSON / Rendered Prompt / Fallback Reason / Tool I/O tabs。
  - Trace Evidence 已完成人工评审并进入阶段冻结；评审记录见 `docs/design_reviews/trace_evidence_review.md`，后续只允许小范围收尾。
  - 新增独立 Human Review 页面，展示复核队列、风险标签、Artifact 预览、决策面板、复核原因、状态历史和为什么需要人工复核说明。
  - Knowledge Base / Prompt Templates 轻量对齐页面级链路：`Document -> Chunk -> Search -> Citation -> Generation Reference`、`Template -> Variables -> Render Preview -> Test Run -> Trace metadata`。
  - 截图脚本改为 1440x1000 viewport，并把 Human Review 截图入口指向独立 `/reviews` 页面；同时产出 `dashboard.png`、`workbench.png`、`trace-evidence.png`、`human-review.png` canonical 截图和现有 README 兼容别名。
- 数据来源与边界：
  - 页面可见位置标注 `Demo Data / 来源：本地指标快照 / local demo`。
  - 指标来自现有接口、Trace / Knowledge 派生、本地 seed/demo 数据或 `docs/metrics/metrics_snapshot.md`；缺失字段显示未采集、未记录或 0。
  - 未复制第三方 Logo、文案、截图、完整布局或产品承诺；`.local/reference_screenshots/` 仍为 ignored，未加入 Git。
  - 未把 `local-rule` 包装成真实 LLM，未把 demo seed / UI-only 字段写成生产数据，未声称 Eval、RBAC、多 Agent Runtime 或向量数据库已实现。
- 验证结果：
  - `cd frontend && npm run build`：通过；仍有既有 VueUse PURE 注释提示和 Element Plus / Markdown 大 chunk 警告。
  - `cd backend && mvn test`：通过，`Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`。
  - `node scripts/collect-portfolio-metrics.js --run-checks`：通过，已刷新 `docs/metrics/metrics_snapshot.md`，当前前端页面文件 8 个、真实 component route 8 个、disabled nav item 3 个。
  - `cd frontend && npm run screenshots:portfolio`：通过，基于本地真实运行页面生成 6 张作品集截图。
  - `git diff --check`：通过，仅有 LF/CRLF 提示。
- 建议 commit message：`feat: upgrade devflow portfolio showcase UI`

## P1-18：Trace Evidence 目标图单页重做 ✅ 本轮完成

- 状态：**done**
- 背景：用户明确要求本轮只做 `Trace Evidence` 页面，并以 `.local/visual_targets/trace-evidence-target.png` 作为唯一视觉参考；不得改 Dashboard、Workbench、Human Review 或根据公开参考图自由设计。
- 涉及文件：`frontend/src/views/AgentRunTraceView.vue`、`frontend/src/router/index.ts`、`frontend/src/layouts/DevFlowLayout.vue`、`frontend/src/components/SidebarNav.vue`、`frontend/src/components/TopBar.vue`、`frontend/src/styles/theme.css`、`scripts/capture-portfolio-screenshots.mjs`、`docs/images/trace-evidence.png`、`docs/metrics/metrics_snapshot.md`、`TODO.md`、`HANDOFF.md`。
- 完成内容：
  - Trace Evidence 页面按目标图重做为左侧运行记录、中间执行时间线、右侧步骤详情与人工复核、底部证据详情的深色开发者工具布局。
  - 页面标题改为 `执行证据 Trace Evidence`，副标题说明从 Prompt 渲染到人工复核的可解释证据链。
  - 当前执行摘要包含执行步骤、知识命中、Tool Call、Token 用量、复核结果 5 个指标卡。
  - 左侧导航和顶部栏只做与目标图一致的轻量文字 / badge 调整：`DevFlow`、当前工作空间、中文分组导航、当前选中的 `执行证据 Trace`、`Demo Data`、`local-rule fallback`、`OpenAI-compatible 可选`。
  - 底部证据详情提供 `Raw JSON / Rendered Prompt / Fallback Reason / Tool I/O` 标签页，默认显示 Raw JSON。
  - 截图脚本新增 `--only=trace-evidence` 单页截图能力，并支持 1440x900 viewport；单页模式只写 `docs/images/trace-evidence.png`，不刷新其它页面截图。
- 数据来源与边界：
  - 页面数据来自本地 H2 Demo、现有 Agent Run Trace / Generation Trace / Knowledge Reference / Human Review 接口和 local-rule 生成记录。
  - 页面明确展示 `Demo Data`、`local-rule fallback`、`OpenAI-compatible 可选` 和不连接真实 API Key 的边界。
  - 未改后端核心业务逻辑，未接真实 API Key，未把 local-rule 写成真实 LLM，未把 demo 数据写成生产数据。
- 验证结果：
  - `cd frontend && npm run build`：通过；仍有既有 VueUse PURE 注释提示和 Element Plus / Markdown 大 chunk 警告。
  - `cd backend && mvn test`：通过，`Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`。
  - `node scripts/collect-portfolio-metrics.js --run-checks`：通过，刷新 `docs/metrics/metrics_snapshot.md`。
  - `node scripts/capture-portfolio-screenshots.mjs --only=trace-evidence`：通过，生成本地真实截图 `docs/images/trace-evidence.png`。
- 建议 commit message：`feat: rebuild trace evidence showcase page`

## 下一轮建议

建议下一步先由用户人工查看 `docs/images/dashboard-agentic.png`、`docs/images/workbench-running.png`、`docs/images/agent-run-trace.png`、`docs/images/human-review-trace-detail.png` 等本地真实截图，再决定是否进入第二阶段 README 展示文案、截图编排或更细的页面 polish。继续保持不提交 API Key、不提交 `.env`、不提交 `.local/`、不提交 `node_modules` / `dist` / `target` / 日志文件。
