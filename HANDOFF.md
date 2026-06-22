# Claude / Codex 交接记录

使用规则：每轮把新记录追加在“历史记录”顶部，不覆盖旧记录。没有证据时不要写“测试通过”。

## 当前待处理交接

### 更新时间

`2026-06-22（Asia/Shanghai）`

### 当前真实状态

- 当前工作分支：`resume-optimization-v1`。
- 最新已完成提交：
  - `0abd047 docs: add resume-oriented project summary`
  - `9babb4f docs: clarify InMemoryStore demo profile boundary`
  - `bce0368 feat: add ai task query endpoint`
  - `5908a7b docs: clarify resume README boundaries`
  - `d5a4486 docs: add project TODO roadmap`
  - `e38126c chore: avoid Docker compose port conflict`
- P0-1 已完成：根目录 `TODO.md` 已创建，AGENTS.md 启动流程中的 TODO 读取闭环已补齐。
- P0-2 已完成：`ai_task` 已补最小只读查询入口 `GET /api/tasks?projectId={projectId}`，不再只是表 / Entity / Mapper 空壳。
- P1-1 已完成：`InMemoryStore` 已补充 demo-only / `memory-demo` profile 边界 Javadoc；它仅用于 `memory-demo` 或无数据库轻量演示场景，不是默认 dev/test/prod 主流程持久化方案，默认主流程应使用 MyBatis-Plus + H2/MySQL。本次仅补注释，没有修改业务逻辑；未运行 `mvn test`，原因是只修改 Javadoc 注释，不改变运行行为。
- P1-2 已完成：`README.md` 已在“项目定位”之后、“技术栈”之前新增“简历版项目介绍”小节；本次仅新增 4 句话的简历投递版项目介绍，没有重写 README，没有修改 Java / Vue 业务代码。
- 后续每轮任务应先读 `AGENTS.md`、`HANDOFF.md`、`TODO.md`，每轮只做一个明确任务，校验通过后再 commit。
- README.md 已完成边界澄清：不再把 Docker Compose runtime 写成完整部署成功；不再笼统写 `API smoke test passed`；已压实 `local-rule` 是本地规则/模板生成，不是真实 LLM 推理；已压实日志分析是关键词规则引擎，不是 AI 自动推理；已压实 local-rule tokenUsage 是文本长度估算，不是真实 tokenizer。
- 项目仍适合作为大三 Java 后端实习简历项目基础，亮点应聚焦 Spring Boot 分层架构、MyBatis-Plus 持久化、Prompt 模板管理、生成记录状态流转、规则化日志诊断、Vue 3 控制台前端、后端测试和前端构建。

### Docker / Compose 当前状态

- WSL Ubuntu-22.04 中 Docker Engine 可用。
- Docker Engine：`29.1.3`。
- Docker Compose Plugin：`v5.1.4`。
- `docker compose config` 已通过。
- 后端宿主默认端口已改为 `18080`，避免和现有 `sub2api` 的 `8080` 冲突；容器内部端口仍为 `8080`。
- `docker compose up --build` 已尝试，但失败原因是 Docker Hub `registry-1.docker.io` 镜像元数据请求 `i/o timeout`。
- 已执行 `docker compose down`，不带 `-v`；不应写成 Docker Compose runtime 已完整部署成功。

### P0-2 只读复核结论

- P0-2 已解决。
- 实现符合 Controller -> Service -> Mapper 分层风格。
- 返回结构沿用 `ApiResponse<List<AiTask>>`。
- 参数校验与现有 `GlobalExceptionHandler` 风格一致：缺失 `projectId` 返回 HTTP 400，错误结构 `code=4000`。
- 测试覆盖正常查询、空结果、缺失 `projectId` 三类场景。
- 后端验证已通过：`mvn test`，`Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`。
- 未发现高风险。
- 低风险：当前排序使用 `orderByDesc(AiTask::getId)`，与部分历史查询使用 `createdAt` 倒序略有风格差异，但功能正确，可解释为 ID 自增近似创建顺序。
- 低风险：测试中 `projectId=2L` 依赖 seed 数据，当前与项目测试惯例一致。

### P1-1 InMemoryStore 边界同步结论

- P1-1 已完成：`InMemoryStore` 已补充 demo-only / `memory-demo` profile 边界 Javadoc。
- 完成提交：`9babb4f docs: clarify InMemoryStore demo profile boundary`。
- 完成边界：`InMemoryStore` 仅用于 `memory-demo` 或无数据库轻量演示场景。
- 持久化边界：它不是默认 dev/test/prod 主流程持久化方案；默认主流程应使用 MyBatis-Plus + H2/MySQL。
- 修改范围：仅修改 `InMemoryStore.java` 类头 Javadoc，没有修改字段、方法、注解或任何业务逻辑。
- 验证说明：未运行 `mvn test`，原因是本次只修改 Javadoc 注释，不改变运行行为。

### P1-2 README 简历版项目介绍同步结论

- P1-2 已完成：`README.md` 已新增“简历版项目介绍”小节。
- 完成提交：`0abd047 docs: add resume-oriented project summary`。
- 完成内容：只修改 `README.md`，在“项目定位”之后、“技术栈”之前新增 4 句话的简历投递版项目介绍。
- 能力表达：突出 Java 后端工程化能力，包括 Spring Boot 3、REST API、Controller -> Service -> Mapper 分层、MyBatis-Plus、Flyway、H2/MySQL Profile、统一响应、状态流转和后端测试；也突出 Prompt 模板、生成记录、人工确认、local-rule 演示生成链路与 OpenAI-compatible Provider 代码层适配。
- 边界表达：继续说明 `local-rule` 不是真实 LLM 推理，日志分析不是 AI 自动推理，tokenUsage 是估算，Docker runtime 未完整成功，`ai_task` 不是完整任务系统，`InMemoryStore` 不是默认主流程持久化。
- 修改范围：仅新增短小节，没有重写 README；没有修改 Java / Vue 业务代码。

### 下一轮建议任务

> 一次只处理一个任务。

**P1-3：只读审查 README.md、HANDOFF.md、TODO.md 和最近提交历史，判断 P1-1、P1-2 是否已闭环，并决定是否进入最终验收报告阶段。**

- 下一轮先只读审查，不直接修改文件。
- 审查重点：文档是否一致、是否还有夸大表述、是否适合进入最终验收。
- 不要继续扩功能。
- 不要运行 Docker。
- 不要把 Docker runtime 写成完整部署成功。

## Codex 修复结果

- `d5a4486 docs: add project TODO roadmap`：已创建根目录 `TODO.md`，P0-1 完成。
- `5908a7b docs: clarify resume README boundaries`：已修正 README 简历表述和验收状态边界。
- `bce0368 feat: add ai task query endpoint`：已新增 `GET /api/tasks?projectId={projectId}`、`AiTaskController`、`AiTaskService`、`AiTaskServiceImpl`，复用 `AiTaskMapper`，并补充 `ControllerAndMapperIntegrationTest` 覆盖；`mvn test` 通过，18 tests。
- `9babb4f docs: clarify InMemoryStore demo profile boundary`：已在 `InMemoryStore.java` 类头补充 demo-only / `memory-demo` profile 边界 Javadoc；仅用于无数据库或轻量演示场景，不是 dev/test/prod 默认持久化方案，默认主流程应使用 MyBatis-Plus + H2/MySQL；本次仅补注释，没有修改业务逻辑，未运行 `mvn test`。
- `0abd047 docs: add resume-oriented project summary`：已在 `README.md` 的“项目定位”之后、“技术栈”之前新增“简历版项目介绍”小节；仅新增 4 句话，没有重写 README，没有修改业务代码；继续压住 `local-rule`、日志规则、tokenUsage、Docker runtime、`ai_task`、`InMemoryStore` 等边界。
- 剩余 P0：当前无已知未闭环 P0；建议下一轮做 P1-3 只读审查 README.md、HANDOFF.md、TODO.md 和最近提交历史，判断是否进入最终验收报告阶段。
- 本次 HANDOFF 同步为纯文档状态更新，不代表业务代码变更。

## 历史记录

### YYYY-MM-DD HH:mm — Agent — 任务名称

- 做了什么：
- 修改文件：
- 验证证据：
- 遗留问题：
- 下一步：

---

### 2026-06-22 — Codex — P1-2 README 简历版项目介绍完成同步

- 做了什么：同步提交 `0abd047 docs: add resume-oriented project summary`，标记 P1-2 已完成；记录 README 已新增“简历版项目介绍”小节，并继续保持 local-rule、日志规则、tokenUsage、Docker runtime、ai_task、InMemoryStore 等边界。
- 修改文件：`HANDOFF.md`、`TODO.md`
- 验证证据：本轮为纯文档同步；已执行 `git diff -- HANDOFF.md TODO.md`、`git diff --check -- HANDOFF.md TODO.md`（退出码 0，仅 LF/CRLF 行尾提示）、`git status --short`（仅 `HANDOFF.md`、`TODO.md` 修改）。
- 遗留问题：无新增阻塞；下一轮不扩功能、不运行 Docker，先只读审查文档与提交历史是否一致。
- 下一步：P1-3 只读审查 README.md、HANDOFF.md、TODO.md 和最近提交历史，判断 P1-1、P1-2 是否已闭环，并决定是否进入最终验收报告阶段。

---

### 2026-06-21 — Codex — P1-1 InMemoryStore demo-only 边界说明完成同步

- 做了什么：同步提交 `9babb4f docs: clarify InMemoryStore demo profile boundary`，标记 P1-1 已完成，并记录 `InMemoryStore` 仅用于 `memory-demo` / 无数据库轻量演示场景，不是默认 dev/test/prod 主流程持久化方案。
- 修改文件：`HANDOFF.md`、`TODO.md`
- 验证证据：本轮为纯文档同步；已执行 `git diff -- HANDOFF.md TODO.md`、`git diff --check -- HANDOFF.md TODO.md`（退出码 0，仅 LF/CRLF 行尾提示）、`git status --short`（仅 `HANDOFF.md`、`TODO.md` 修改）。
- 遗留问题：无新增阻塞；`ai_task` 仍只是最小只读查询入口；`local-rule` 仍是本地规则/模板生成；日志分析仍是关键词规则引擎；Docker Compose runtime 仍未完整部署成功。
- 下一步：P1-2 只读审查 README.md 中“简历可写亮点”和“面试回答口径”，先判断是否需要补一段更适合大三 Java 后端实习简历的项目介绍，不直接修改 README。

---

### 2026-06-21 — Codex — P0-2 ai_task 最小查询入口完成复核

- 做了什么：记录提交 `bce0368 feat: add ai task query endpoint`，同步 P0-2 已完成，并补充只读复核结论。
- 修改文件：`HANDOFF.md`、`TODO.md`
- 验证证据：代码提交中已运行 `mvn test`，结果为 `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`。
- 遗留问题：`ai_task` 仅为最小只读查询入口，不是完整任务系统；无新增/更新/删除、调度、异步执行或 LLM 接入。
- 下一步：建议进行 P0 收口只读总审查，确认 README/HANDOFF/TODO/测试结果/提交历史一致后再进入 P1。

---

### 2026-06-21 — Codex — HANDOFF 最新状态同步

- 做了什么：同步最新提交 `d5a4486`、`5908a7b` 和 Docker Compose 当前真实状态；标记 P0-1 已完成；记录 README 边界澄清已完成；明确下一轮只处理 P0-2 ai_task 空壳问题。
- 修改文件：`HANDOFF.md`
- 验证证据：本轮仅文档同步；修改后应执行 `git diff -- HANDOFF.md`、`git diff --check -- HANDOFF.md`、`git status --short`。
- 遗留问题：`ai_task` 表 / 实体 / Mapper 仍无 Service / Controller 或明确扩展预留说明；`InMemoryStore` 仍建议后续补 demo-only Javadoc。
- 下一步：只处理 P0-2 ai_task 空壳问题，二选一实现最小接口或标注扩展预留。

---

### 2026-06-21 — Claude — 简历版只读审查

#### 审查对象

- 功能名称：项目整体简历可用性评估
- 审查文件范围：README.md、AGENTS.md、CLAUDE.md、HANDOFF.md（空模板）、docs/（全部 md 文件）、backend/src/main/java/（全部 Java 源码）、backend/src/test/（全部测试）、frontend/src/（App.vue、WorkbenchView.vue）、db/migration/V1__create_core_schema.sql、docs/images/（截图目录）
- 审查者：Claude（只读，未执行任何构建或测试命令）

#### 当前结论

项目整体完成度在同类大三实习项目中属于上游水平。后端分层清晰、状态机设计正确、15 个集成测试有据可查、截图文件实际存在、Provider 抽象和 Flyway 迁移均有真实代码落地。主要风险集中在三件事：ai_task 表空壳、InMemoryStore 遗留代码位置容易误导、local-rule 生成内容是模板 boilerplate 而非真实 LLM 输出——后两件需要在文档中说清楚，第一件需要补最小实现或说明。

关于 Git 仓库状态：旧验收记录（final-acceptance-report.md）曾显示 `fatal: not a git repository`，但当前项目已在 `resume-optimization-v1` 分支并已提交协作规则，此问题当前状态标记为**已解决**，不再列为 P0。后续建议保持真实小步提交，不伪造历史，不为凑 commit 数拆旧代码假提交。

#### 已确认可写进简历的真实能力

以下技术点均有对应源码，面试时可直接展示：

- Spring Boot 3 + Java 17 分层架构（controller / service / service.impl / provider / mapper / entity / dto）
- MyBatis-Plus 持久化 + Flyway 数据库迁移（V1 建表5张，FK 约束完整）
- H2（dev/test）/ MySQL（prod）Profile 双数据库支持
- 生成状态机 `GENERATING → READY_FOR_REVIEW → SAVED → CONFIRMED`，`canTransitionTo` / `allowedTargets` 逻辑正确，非法流转返回 HTTP 409
- Prompt 模板变量渲染（正则替换 `{{variable}}`）、缺填报 TemplateRenderException、模板版本记录进 generation_record
- OpenAI-compatible Provider（真实 RestClient HTTP 调用 `/v1/chat/completions`）+ local-rule 降级
- DTO 校验（`@Valid`）+ GlobalExceptionHandler 统一 ApiResponse 返回
- 15 个 JUnit 5 / MockMvc / `@SpringBootTest` 集成测试（`@Transactional` 回滚隔离）
- Vue 3 + TypeScript + Element Plus 前端，WorkbenchView 三栏布局，状态机按钮联动
- Docker Compose（backend / frontend / mysql 三服务）配置存在，`docker compose config` 已通过；runtime `up --build` 因 Docker Hub 超时未完整成功；GitHub Actions CI 配置文件存在

#### 不能夸大的能力

以下内容不得在简历或面试中声称为已实现：

- **不能写**"接入了真实大模型" —— 默认运行模式是 local-rule，生成内容是模板 boilerplate；只有配置了真实 API Key 并端到端验证后才可写
- **不能写**"AI 智能日志分析" —— 日志诊断是关键词规则引擎（8种异常类型硬编码），不是 LLM 推理
- **不能写**"毫秒级 latency 追踪" —— local-rule 的 costTimeMs 实测约为 0–1ms，不代表 LLM 网络延迟
- **不能写**"SSE 流式输出" —— 当前是同步请求，无 SSE
- **不能写**"Docker Compose 已验证部署" 或 "完整容器化部署成功" —— WSL Docker Engine 和 Compose Plugin 当前可用，`docker compose config` 已通过，`docker compose up --build` 已尝试但失败于 Docker Hub `registry-1.docker.io` 镜像元数据请求 `i/o timeout`，runtime smoke test 尚未完整完成

---

#### 当前 P0 问题

**P0-1：TODO.md 不存在（已完成）**

- 状态：已完成，提交 `d5a4486 docs: add project TODO roadmap`。
- 当前结果：根目录 `TODO.md` 已创建，记录已完成能力、不能夸大的能力、P0/P1/P2 待办和下一轮建议。
- AGENTS.md 启动流程闭环：后续每轮任务应先读 `AGENTS.md`、`HANDOFF.md`、`TODO.md`。
- 当前不再把 TODO 缺失列为 P0；剩余 P0 为 `ai_task` 空壳问题。

**P0-2：ai_task 表和 Mapper 无对应 Service / Controller（已完成）**

- 状态：已完成，提交 `bce0368 feat: add ai task query endpoint`。
- 完成内容：新增 `GET /api/tasks?projectId={projectId}`，新增 `AiTaskController`、`AiTaskService`、`AiTaskServiceImpl`，复用 `AiTaskMapper`。
- 行为边界：`projectId` 必填；缺失返回 HTTP 400 和统一错误结构 `code=4000`；返回 `ApiResponse<List<AiTask>>`；按 ID 倒序返回指定项目任务；不存在项目或无任务时返回空数组。
- 未做内容：没有新增、更新、删除接口；没有任务调度；没有异步执行；没有接入真实 LLM；没有改生成、日志分析、模板管理、历史记录主流程；没有改 `InMemoryStore`。
- 验证：`ControllerAndMapperIntegrationTest` 已覆盖正常查询、空结果、缺失 `projectId`；`mvn test` 通过，`Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`。

---

#### 当前 P1 问题

**P1-1：InMemoryStore 位置容易引起误解（已完成）**

- 状态：已完成，提交 `9babb4f docs: clarify InMemoryStore demo profile boundary`。
- 文件/位置：`backend/src/main/java/com/devflow/copilot/service/impl/InMemoryStore.java`。
- 完成内容：仅修改类头 Javadoc，明确 `InMemoryStore` 只用于 `memory-demo` profile 或无数据库轻量演示场景。
- 持久化边界：`InMemoryStore` 不是默认 dev/test/prod 主流程持久化方案；默认主流程应使用 MyBatis-Plus + H2/MySQL。
- 验证说明：未运行 `mvn test`，原因是本次只修改 Javadoc 注释，不改变运行行为。
- 验收：阅读代码不再产生“内存存储是默认生产持久化”的歧义。

**P1-2：README 简历版项目介绍（已完成）**

- 状态：已完成，提交 `0abd047 docs: add resume-oriented project summary`。
- 完成内容：`README.md` 已在“项目定位”之后、“技术栈”之前新增“简历版项目介绍”小节。
- 完成边界：仅新增 4 句话的短小节，没有重写 README，没有修改 Java / Vue 业务代码。
- 表达重点：突出 Spring Boot 3、REST API、分层设计、MyBatis-Plus、Flyway、H2/MySQL、统一响应、状态流转、后端测试，以及 Prompt 模板、生成记录、人工确认、local-rule 演示生成链路和 OpenAI-compatible Provider 代码层适配。
- 边界保持：没有把项目夸大为生产级大模型平台；继续说明 `local-rule` 不是真实 LLM 推理，日志分析不是 AI 自动推理，tokenUsage 是估算，Docker runtime 未完整成功，`ai_task` 不是完整任务系统，`InMemoryStore` 不是默认主流程持久化。

**P1-3：只读审查 README.md、HANDOFF.md、TODO.md 和最近提交历史**

- 状态：待处理。
- 目标：判断 P1-1、P1-2 是否已闭环，并决定是否进入最终验收报告阶段。
- 审查重点：文档是否一致、是否还有夸大表述、是否适合进入最终验收。
- 不可扩大：不要继续扩功能，不要运行 Docker，不要把 Docker runtime 写成完整部署成功。
- 验收：输出只读审查结论；如需进入最终验收报告阶段，应作为后续单独任务处理。

**已完成：README token 估算说明（原 P1-2）**

- 状态：已完成，提交 `5908a7b docs: clarify resume README boundaries`。
- 当前结果：README 已说明 local-rule 模式下 tokenUsage 是基于文本长度的估算值，不是真实 tokenizer；OpenAI-compatible 模式下只有 provider 返回 `usage` 字段时才可记录真实 usage。

**已完成：local-rule 生成内容性质说明（原 P1-3）**

- 状态：已完成，提交 `5908a7b docs: clarify resume README boundaries`。
- 当前结果：README 已明确 `local-rule` 是本地规则/模板生成，不是真实 LLM 推理；OpenAI-compatible Provider 是代码层适配，当前仓库不提交真实 API Key，也不把真实模型端到端调用写成已稳定验收。
- 同步结果：README 也已压实日志分析是关键词规则引擎，不是 AI 自动推理；已修正 Docker Compose runtime 状态和 `API smoke test passed` 的笼统表述。

**P1-4：HANDOFF.md 原为空白模板，无实际审查记录**

- 文件/位置：HANDOFF.md
- 证据：原文件仅包含占位符 `YYYY-MM-DD HH:mm`，无任何真实记录
- 影响：AGENTS.md 要求启动前读取 HANDOFF.md，空模板无法传递项目状态
- 建议：追加本次审查记录（即本条目）
- 验收：本条目追加完成即视为解决

---

#### 当前 P2 问题

**P2-1：OpenAI-compatible Provider 无真实端到端测试**

- 当前状态：只测试了缺 API Key 时的降级逻辑，未配置真实 Key 做端到端调用
- 建议：配置一个低成本兼容服务（如 DeepSeek、Groq），跑通真实调用，更新 README 说明已验证

**P2-2：前端无单元测试**

- 当前状态：只有 `npm run build` 通过，无 Vitest / Vue Test Utils 测试
- 建议：为 StatusTag 组件或 WorkbenchView 核心计算属性添加 1–2 个 Vitest 测试

**P2-3：Docker Compose runtime 未完整成功**

- 当前状态：WSL Ubuntu-22.04 Docker Engine `29.1.3` 可用，Docker Compose Plugin `v5.1.4` 可用；`docker compose config` 已通过；后端宿主默认端口已改为 `18080` 避让 `sub2api` 的 `8080`。
- 已尝试：`docker compose up --build` 已执行，但失败原因是 Docker Hub `registry-1.docker.io` 镜像元数据请求 `i/o timeout`；后端/前端容器未创建，runtime smoke test 未完整完成。
- 已清理：已执行 `docker compose down`，不带 `-v`。
- 建议：待 Docker Hub 网络恢复或基础镜像可用后，重新执行 runtime smoke test；未成功前不得写成 Docker Compose 已完整部署成功。

**P2-4：缺少动态演示材料**

- 当前状态：有静态截图10张，无 GIF 或视频演示
- 建议：录制 30 秒 GIF 展示 Workbench 生成→保存→确认流程，放入 README

---

#### 交给 Codex 的第一批任务

> 规则：每次只处理一个任务，完成后回写"Codex 修复结果"再开始下一个。

**任务1：创建或完善 TODO.md（已完成）**

- 目标：在项目根目录创建 `TODO.md`，记录已完成功能和当前待处理项
- 完成提交：`d5a4486 docs: add project TODO roadmap`
- 涉及文件：`TODO.md`（新建）
- 不能破坏：其他任何文件不做改动
- 不在本轮处理：其他 P0/P1 问题
- 验收方式：
  - [x] `TODO.md` 文件存在于项目根目录
  - [x] 文件内容列出"已完成"与"待处理"两个分区，内容与项目实际现状一致
  - [x] AGENTS.md 描述的"启动前读取 TODO.md"流程可正常执行

**任务2：处理 ai_task 空壳问题（已完成）**

- 目标：选择方案A或B —— A: 添加最简 `GET /api/tasks?projectId=` 返回空列表；B: 在 AiTaskMapper 头部 Javadoc 注释中说明"为后续扩展预留，当前无业务实现"
- 完成提交：`bce0368 feat: add ai task query endpoint`
- 涉及文件：方案A 涉及 `AiTaskMapper.java`、新建 `AiTaskService.java`、`AiTaskController.java`；方案B 仅涉及 `AiTaskMapper.java`
- 不能破坏：现有15个测试须全部通过，`mvn test` 结果不能回归
- 不在本轮处理：ai_task 的完整 CRUD
- 验收方式：
  - [x] 选择了明确的方案（A）并实施
  - [x] `GET /api/tasks?projectId=1` 返回 HTTP 200 和列表；未知项目返回 HTTP 200 空列表
  - [x] `mvn test` 通过，18 tests
  - [x] 面试时对"这个 Mapper 是干嘛的"有完整答案：它支撑最小只读任务查询入口，不是完整任务系统

**任务3：补充 README 中 token 估算和 local-rule 说明（已完成）**

- 目标：在 README.md 相关段落补充两句话：(1) local-rule token 是字符数估算；(2) local-rule 生成的是结构化模板内容，非 LLM 推理
- 完成提交：`5908a7b docs: clarify resume README boundaries`
- 涉及文件：`README.md`
- 不能破坏：README 整体结构和其他内容保持不变
- 不在本轮处理：其他文档修改
- 验收方式：
  - [x] README 中出现对 token 估算方式的准确说明
  - [x] README 中出现对 local-rule 内容性质的准确说明
  - [x] 不引入任何夸大或虚构的功能描述

**任务4：整理 InMemoryStore 的 demo-only 说明（已完成）**

- 目标：在 `InMemoryStore.java` 类头添加 Javadoc 注释，说明该类仅在 `memory-demo` profile 下激活，主存储已切换为 MyBatis-Plus + H2/MySQL。
- 完成提交：`9babb4f docs: clarify InMemoryStore demo profile boundary`。
- 涉及文件：`backend/src/main/java/com/devflow/copilot/service/impl/InMemoryStore.java`
- 完成边界：仅修改类头 Javadoc；`@Profile("memory-demo")` 注解未删除，类功能和业务逻辑未改变。
- 不在本轮处理：包结构调整
- 验收方式：
  - [x] InMemoryStore.java 类头有清晰的 Javadoc 注释
  - [x] 注释内容说明“演示/轻量 demo 用途，memory-demo profile 专用，主流程不加载”
  - [x] 说明默认主流程应使用 MyBatis-Plus + H2/MySQL
  - [x] 本次只改注释，不改变运行行为；未运行 `mvn test`

**任务5：补充 README 简历版项目介绍（已完成）**

- 目标：在 `README.md` 的“项目定位”之后、“技术栈”之前新增“简历版项目介绍”短小节。
- 完成提交：`0abd047 docs: add resume-oriented project summary`。
- 涉及文件：`README.md`。
- 完成边界：仅新增 4 句话的短小节，没有重写 README，没有修改 Java / Vue 业务代码，没有修改 `HANDOFF.md`、`TODO.md` 或 `InMemoryStore.java`。
- 验收方式：
  - [x] 小节标题为“简历版项目介绍”
  - [x] 覆盖 Java 后端工程化能力和 AI Coding 工具链能力
  - [x] 继续压住 local-rule、日志规则、tokenUsage、Docker runtime、ai_task、InMemoryStore 等边界

---

#### Codex 修复结果（已同步）

- P0-1 / 任务1：已完成，提交 `d5a4486 docs: add project TODO roadmap`。
- P0-2 / 任务2：已完成，提交 `bce0368 feat: add ai task query endpoint`。
- README 边界澄清 / 任务3：已完成，提交 `5908a7b docs: clarify resume README boundaries`。
- P1-1 / 任务4：已完成，提交 `9babb4f docs: clarify InMemoryStore demo profile boundary`；仅补充 `InMemoryStore.java` 类头 Javadoc，明确其仅用于 `memory-demo` / 无数据库轻量演示场景，不是默认 dev/test/prod 主流程持久化方案。
- P1-2 / 任务5：已完成，提交 `0abd047 docs: add resume-oriented project summary`；仅在 `README.md` 新增“简历版项目介绍”短小节，没有重写 README，并继续保持所有边界说明。
- Docker Compose 端口避让：已完成配置修改，提交 `e38126c chore: avoid Docker compose port conflict`；runtime `up --build` 未完整成功，原因是 Docker Hub 镜像元数据请求 `i/o timeout`。
- 当前剩余优先任务：P1-3 只读审查 README.md、HANDOFF.md、TODO.md 和最近提交历史，判断 P1-1、P1-2 是否已闭环，并决定是否进入最终验收报告阶段。
- 本条同步仅更新交接信息，不代表本轮修改业务代码。
