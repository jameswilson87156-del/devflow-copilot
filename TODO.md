# DevFlow Copilot — 项目 TODO

> 阅读规则（AGENTS.md 要求）：每轮开始前先读本文件，了解当前已完成情况和待处理任务，再读 HANDOFF.md 了解最近一轮审查或修复记录。

## 项目当前定位

面向大三实习简历的 **AI Coding 工作流控制台 / Java 后端工程化项目**。

核心价值在于把 AI Coding 行为工程化：Prompt 模板渲染、Provider 抽象与降级、生成历史持久化、状态机约束、人工确认闭环，而不是聊天套壳或完整生产级大模型平台。

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
- **日志诊断规则引擎**：识别8种 Java/Spring Boot 常见异常关键词，输出排查步骤和修复 Prompt（规则引擎，不是 LLM 推理）
- **DTO 校验 + GlobalExceptionHandler**，统一 `ApiResponse` 响应结构
- **18 个 JUnit 5 / MockMvc / `@SpringBootTest` 集成测试**（`@Transactional` 隔离，`@ActiveProfiles("test")`），最近一次 `mvn test`：`Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`
- **ai_task 最小只读查询入口**：`GET /api/tasks?projectId={projectId}`，复用 `AiTaskMapper`，按 ID 倒序返回指定项目任务列表
- **Vue 3 + TypeScript + Element Plus** 前端，WorkbenchView 三栏布局，状态机按钮联动，`npm run build` 通过
- **截图**：`docs/images/` 下5张标准截图 + 5张1920px 大图均存在
- **Docker Compose** 配置存在，`docker compose config` 通过（三服务：mysql / backend / frontend）
- **后端宿主端口避让**：已从 `8080` 改为 `${BACKEND_HOST_PORT:-18080}:8080`，避免与现有服务冲突
- **GitHub Actions CI** 配置存在，包含后端 `mvn -B verify` 和前端 `npm ci && npm run build`
- **环境审计文档**：`docs/ENVIRONMENT_CHECK.md` 已记录本机工具版本和 Docker Compose 验证记录
- **Git 仓库**：当前分支 `resume-optimization-v1`，协作规则已提交

---

## 当前不能夸大的能力

以下内容**不能写进简历，也不能在面试中声称为已实现**：

| 不能声称 | 真实情况 |
|---|---|
| 接入了真实大模型 | 默认 `local-rule` 生成的是结构化模板 boilerplate，不是 LLM 推理结果；OpenAI-compatible 代码已实现但未用真实 Key 端到端验证 |
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

### P1-2：只读审查 README.md 中“简历可写亮点”和“面试回答口径”

- 状态：**待处理**
- 背景：P0 与 P1-1 已收口后，需要确认 README 是否还需要一段更适合大三 Java 后端实习简历的项目介绍。
- 涉及文件：只读审查 `README.md`，下一轮先不直接修改 README。
- 审查重点：判断项目是否可以更稳地表达为 Java 后端工程化 + AI 工具链项目。
- 不可夸大：不能写成生产级大模型平台；不能把 `local-rule` 写成真实 LLM；不能把日志规则引擎写成 AI 推理。
- 验收：输出只读审查结论；如果确实需要修改 README，应作为后续单独任务处理。

### 已完成：README 补充 local-rule 和 token 估算说明（原 P1-2）

- 状态：**done**
- 背景：README 描述了 token 记录和 local-rule 模式，但未说明 token 是估算值、local-rule 生成内容是模板 boilerplate
- 涉及文件：`README.md`（仅相关段落）
- 方案：在 LLM Provider 配置或核心功能段落补充两句话：① local-rule token 为字符数估算；② local-rule 生成结构化模板内容，非 LLM 推理
- 不可破坏：README 整体结构和简历描述部分不变
- 验收：描述与实现一致；不引入任何虚构功能
- 完成提交：`5908a7b docs: clarify resume README boundaries`

### P1-3：HANDOFF.md 持续记录 Claude 审查和 Codex 修复结果

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

---

## P2 — 有时间再做（加分项）

| 编号 | 任务 | 说明 |
|---|---|---|
| P2-1 | 真实 LLM 端到端验证 | 配置低成本 OpenAI-compatible Key（如 DeepSeek、Groq），跑通真实调用，更新 README |
| P2-2 | 前端 Vitest 最小测试 | 为 StatusTag 或 WorkbenchView 计算属性添加 1–2 个 Vitest 测试 |
| P2-3 | Docker compose up --build 网络条件好时重跑 | 换镜像源或等网络恢复后重新验证，成功后补充进 HANDOFF.md 和 ENVIRONMENT_CHECK.md |
| P2-4 | 30 秒演示 GIF | 录制 Workbench 生成 → 保存 → 确认流程，放入 README |
| P2-5 | 前端 chunk 体积优化 | 做路由级 dynamic import，解决 `npm run build` 的 500 kB 体积警告 |

---

## 下一轮建议任务

**P1-2：只读审查 README.md 中“简历可写亮点”和“面试回答口径”。**

下一轮先只读审查，不直接修改 README。

审查重点是判断是否可以把项目更稳地表达为 Java 后端工程化 + AI 工具链项目。

边界要求：不能夸大为生产级大模型平台；不能把 `local-rule` 写成真实 LLM；不能把日志规则引擎写成 AI 推理。

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
