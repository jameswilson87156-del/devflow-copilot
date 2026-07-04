# DevFlow Copilot 作品集化只读审查

审查日期：2026-07-04

审查范围：项目规则、README、TODO、HANDOFF、架构/API/部署文档、前端路由与页面、后端 Controller、截图脚本、测试目录、数据库迁移和本地 Git 状态。

## 当前项目状态

- fact：当前工作分支已切到 `feat/portfolio-showcase-v1`，开始修改前 `main` 分支工作区无未提交 diff。
- fact：`docs/PRD.md` 和 `docs/DESIGN.md` 当前不存在；AGENTS 要求读取这两个文件，实际项目使用根目录 `DESIGN.md`、`docs/frontend-design.md`、`docs/architecture.md` 承接设计与架构说明。
- fact：前端技术栈为 Vue 3、TypeScript、Vite、Vue Router、Element Plus、Axios、Markdown-it、Highlight.js、Playwright 截图脚本。
- fact：后端技术栈为 Java 17、Spring Boot 3.3.5、MyBatis-Plus、Flyway、H2/MySQL、JUnit 5、MockMvc。
- fact：当前前端存在 7 个真实页面文件：Dashboard、Workbench、Log Analyzer、Agent Run Trace、Knowledge Base、Prompt Studio、Generation History。
- fact：路由中 `/docs` 仅重定向到 `/`；`Tool Calls`、`Human Review`、`Provider`、`Settings` 当前是 disabled 导航项，不是独立页面。
- fact：后端 Controller 映射约 30 个接口方法，覆盖 Project、AI Generation、Generation History、Generation Trace、Agent Run Trace、Knowledge Base、Log Analysis、Prompt Template、Dashboard、ai_task 查询。
- fact：后端测试源码中有 20 个 `@Test`，历史记录最近一次为 `mvn test` 通过 20 tests。
- fact：`scripts/capture-portfolio-screenshots.mjs` 已存在，会要求前后端服务运行，并通过真实后端 API 预热 demo workflow 后输出截图到 `docs/images/`。
- fact：`docs/images/` 当前有 11 张 PNG；README 顶部展示 6 张真实浏览器截图。

## 已有优势

- fact：项目已经不是普通聊天 demo，而是有 Prompt 模板、Provider 抽象、Generation Trace、Agent Run Trace、Tool Call、Knowledge 引用、Human Review 和状态机的工程闭环。
- fact：默认 `local-rule` 可无 Key 演示，OpenAI-compatible Provider 以环境变量配置，仓库不提交真实 API Key。
- fact：后端分层清晰，Controller -> Service -> Mapper -> Entity/DTO 的结构适合 Java 后端实习面试展示。
- fact：Flyway migration、H2/MySQL profile、统一响应、DTO 校验、全局异常处理、测试和 CI 均有真实文件证据。
- fact：前端已完成深色 AI SaaS 控制台方向的多页改造，并有真实截图脚本支撑 README 展示。

## 主要问题

- fact：AGENTS 要求的 `docs/PRD.md`、`docs/DESIGN.md` 缺失，容易造成新协作者启动流程卡顿；后续应补齐文件或修正 AGENTS 的必读清单。
- fact：`docs/api.md` 写截图 demo backend 为 `http://127.0.0.1:18090/api`，而当前截图脚本默认 `http://127.0.0.1:18081/api`，存在文档不一致。
- fact：README 有作品集链接和截图，但还没有独立的数据指标快照章节。
- fact：Human Review 和 Tool Calls 有数据模型与 Trace 展示，但目前没有独立可点击页面，只能在 Agent Run Trace / Generation History / Workbench 中查看。
- fact：前端已有 `docs/design/references/` AI-generated concept images，但本轮之前没有 `docs/design_refs/README.md` 这个统一索引入口。
- inference：当前作品集表达已经强于 MVP，但如果继续投递 Boss 直聘，最好补一页“实测指标快照”，把页面数、接口数、测试数、截图数、构建状态说清楚。

## 展示风险

- risk：不能把 `local-rule` 写成真实 LLM 推理；它是本地规则/模板生成。
- risk：不能把 Knowledge Base 写成向量数据库；当前是关键词/简单相似度检索，embedding 字段只是预留。
- risk：不能把 Agent Run Trace 写成完整多 Agent Runtime；当前是一次生成任务的可解释审计闭环。
- risk：不能声称 Docker Compose runtime 已完整部署成功；已有记录显示 `docker compose config` 通过，但 `up --build` 曾因 Docker Hub 超时未完成。
- risk：不能写真实线上用户、真实商业收益、未验证性能提升或真实生产数据。
- risk：公开 demo 若启用真实 Provider，只能通过服务器环境变量配置 Key，不能写入仓库、README、截图或日志。

## README 不一致点

- fact：README 对 `local-rule`、OpenAI-compatible Provider、Knowledge Base、Docker runtime 边界表述较准确。
- issue：README 快速启动写后端默认端口 `8080`，Docker Compose 写宿主端口 `18080`，截图脚本默认 API 为 `18081`；这些是不同场景，但缺少一张端口用途表。
- issue：README 暂无“数据指标快照”章节；如果要作为 GitHub 作品集首页，应引用 `docs/metrics/metrics_snapshot.md` 的实测结果。
- issue：README 展示了 6 张截图，但未说明截图数量、截图脚本运行状态和最近生成时间。
- issue：README 已有“简历版项目介绍”，但可进一步提炼成更适合 Boss 直聘的一句话定位 + 3-5 条可验证 bullet。

## 前端问题

- fact：当前 7 个页面均是真实路由或页面文件，其中 Dashboard、Workbench、Knowledge Base、Prompt Studio 已做高保真中文化改造。
- fact：现有导航将 Tool Calls、Human Review、Provider、Settings 显示为 disabled 项，避免创建假页面。
- issue：如果后续要满足“明确 5 个核心页面：Dashboard / Workbench / Prompt Templates / Trace Evidence / Human Review”，需要决定 Human Review 是独立页面，还是明确作为 Trace Evidence 的详情页状态。
- issue：`docs/design/references/` 的参考图是 AI-generated concept images，不能作为 README 真实截图，也不应被包装为 GitHub 公开项目调研来源。
- issue：前端 bundle 曾有 Element Plus / Markdown 大 chunk 警告，虽然 Vite manualChunks 已配置，仍建议后续用指标脚本持续记录 bundle 体积。

## 后端问题

- fact：后端已支持 Prompt -> Provider -> Result -> Trace -> Human Review 的核心闭环。
- fact：Provider fallback、Generation Trace、Agent Run Trace、Tool Call、Human Review、Knowledge Reference 均有表和接口支撑。
- issue：当前没有独立 `/api/health`，部署验收用 `/api/dashboard/stats` 替代。
- issue：`ai_task` 当前仅是按 projectId 查询的最小只读入口，不能写成完整任务系统。
- issue：日志分析是关键词规则引擎，不能写成 AI 根因分析或 LLM 诊断。

## 可写入简历的数据

| 标签 | 数据 | 推荐表述 |
| --- | --- | --- |
| fact | 7 个前端页面文件 | 实现 Vue 3 + TypeScript 多页 AI Coding 控制台，覆盖工作台、Trace、知识库、Prompt 管理等核心视图。 |
| fact | 约 30 个后端 Controller 接口映射 | 设计并实现 Spring Boot REST API，覆盖生成、追踪、知识库、模板、历史和人工确认流程。 |
| fact | 20 个后端自动化测试源码 | 使用 JUnit 5 / MockMvc / SpringBootTest 覆盖状态机、Provider fallback、Trace、Knowledge Base 等路径。 |
| fact | 11 张本地截图文件，README 展示 6 张 | 为作品集准备真实浏览器截图，并通过脚本复现截图生成流程。 |
| fact | 4 个 Flyway migration 文件 | 使用 Flyway 管理核心表结构和 demo seed 数据，支持 H2/MySQL profile。 |
| fact | local-rule + OpenAI-compatible Provider | 默认无 Key 演示，保留兼容 `/v1/chat/completions` 的真实 Provider 适配能力。 |

## 暂时不能写的数据

| 不能写 | 原因 |
| --- | --- |
| 已稳定接入真实大模型 | 仓库默认 `local-rule`，真实 Key 不提交；真实 Provider 不是当前默认验收路径。 |
| 线上真实用户或商业收益 | 项目没有真实用户、业务收入或生产统计证据。 |
| 向量数据库 RAG | 当前没有向量数据库，只有关键词/简单相似度检索。 |
| 完整多 Agent Runtime | 当前是 Agent Workflow 审计记录，不是复杂多 Agent 调度。 |
| Docker Compose 已完整部署成功 | 历史记录显示 runtime 曾因 Docker Hub 网络超时未完成。 |
| Lighthouse 分数或首屏性能分数 | 本轮之前没有可追溯采集结果。 |

## 第一阶段建议优化方向

1. 新增作品集化文档：审查报告、公开参考调研、参考图索引、指标计划、展示改造计划。
2. 新增本地指标采集脚本，生成 `docs/metrics/metrics_snapshot.md`，优先记录可静态采集和可本地命令验证的数据。
3. README 后续只基于实测指标升级，不加入未采集的性能或用户数据。
4. 前端正式改造前先确认参考图来源：本地 AI concept 只能作为布局参考，公开项目只能借鉴信息架构，不复制代码、样式、图片、商标或文案。
5. 下一轮若进入 UI 改造，优先处理 Human Review 展示路径和 Trace Evidence 信息组织，不继续扩大后端能力。
