# 前端 Moodboard

访问与筛选日期：2026-07-04

本文件从 `docs/frontend_reference_research.md` 的候选来源中筛选 DevFlow Copilot 第一阶段最适合采用的 5 个参考方向。它是页面级参考绑定，不是代码实现计划。

重要说明：

- `docs/reference_research.md` 是通用公开项目调研，不等于前端视觉参考调研。
- `docs/showcase_upgrade_plan.md` 是作品集升级计划，不等于前端页面级设计方案。
- `docs/design_refs/README.md` 是项目内参考图状态说明，不等于全网参考 moodboard。

## 最终推荐的 5 个参考方向

| 方向 | 核心来源 | 适合页面 | 为什么适合 DevFlow |
| --- | --- | --- | --- |
| LLM Observability Trace Console | Langfuse、LangSmith、Arize Phoenix、AgentOps | Trace Evidence、Dashboard、Human Review | DevFlow 的真实优势是 Trace、Tool Call、Human Review，不是聊天。该方向能让面试官看到“可解释的 AI 工作流证据”。 |
| AI Coding Control Center | OpenHands、Linear、Raycast | Workbench、Human Review、全局导航 | DevFlow 要像开发者控制台：任务、上下文、结果、审核、命令，而不是普通后台或聊天窗口。 |
| AI Workflow / RAG Builder Console | Dify、Flowise、Open WebUI | Knowledge Base、Prompt Templates、Workbench 右侧详情 | DevFlow 已有 Document / Chunk / Knowledge Reference / Prompt Template，可借鉴信息分区，但不做节点画布。 |
| Metrics-first Developer Dashboard | PostHog、Mobbin Admin Dashboard、Mobbin Log Dashboard、Supabase | Dashboard、README 指标快照、Log Analyzer | DevFlow 需要展示本地实测指标：页面数、接口数、测试数、截图数、build/test 状态，不能写假数据。 |
| PromptOps / README Evidence Showcase | Promptfoo、Vercel AI SDK、Flowise、OpenHands、Landingfolio | Prompt Templates、README 截图展示、项目首页说明 | 适合把脚本、命令、截图、Prompt 测试和 Provider 边界放到 README 与页面中。 |

## 页面级绑定

| DevFlow 页面 | 主参考 | 辅参考 | 应借鉴 | 不应照搬 |
| --- | --- | --- | --- | --- |
| Dashboard | Langfuse、PostHog | Mobbin Admin Dashboard、Supabase | 运行数、成功率、平均耗时、Human Review、Tool Call、Recent Runs；用真实指标和状态表组织 | 不复制 chart 视觉，不写生产成本/用户量，不做营销 hero |
| Workbench | OpenHands | Raycast、Linear、Vercel AI SDK | Developer control center、命令栏、任务上下文、Provider 状态、生成结果、Save / Confirm | 不声称自动改代码，不复制 Agent Canvas，不做聊天布局 |
| Trace / Timeline | LangSmith、Langfuse | AgentOps、Phoenix、Mobbin Log Dashboard | Trace list、timeline、span/step、tool call、error/fallback、latency、review checkpoint | 不复制 trace 截图，不模拟隐藏思考链，不写 OpenTelemetry 已接入 |
| Human Review | Linear | LangSmith feedback、AgentOps session inspector | 审核队列、风险、等待时长、负责人、通过/驳回/重新生成、状态历史 | 不做完整权限系统，不复制 Linear issue UI |
| Knowledge Base | Dify | Open WebUI、Supabase | Document -> Chunk -> Search -> Citation -> Generation Reference 链路 | 不写向量数据库已启用，不复制 Dify workflow canvas |
| Prompt Templates | Promptfoo | Helicone、Phoenix、Vercel AI SDK | 模板版本、变量、测试运行、结果表、Provider/Model/Token 边界 | 不写 LLM-as-Judge、red teaming 或自动评测已实现 |
| README 截图展示 | OpenHands、Flowise | Promptfoo、Landingfolio | 顶部定位、真实截图、Quickstart、metrics snapshot、边界说明 | 不使用 concept image，不复制官网图，不写无来源指标 |

## 每个页面的 Mood Direction

### Dashboard

- 参考谁：Langfuse + PostHog + Mobbin Admin Dashboard。
- 氛围：深色运行总览，指标是入口，Recent Runs 和 Review Queue 是主内容。
- 视觉关键词：compact metrics、status table、timeline strip、trace health。
- 用户确认点：是否接受 Dashboard 不做大营销 hero，而是偏工程运行台。

### Workbench

- 参考谁：OpenHands + Raycast + Linear。
- 氛围：AI Coding 控制台，左输入/上下文，中执行/结果，右证据/审核。
- 视觉关键词：command bar、context pane、artifact preview、human checkpoint。
- 用户确认点：是否接受 Workbench 继续强调“人工确认前停止”，不做自动 patch 体验。

### Trace / Timeline

- 参考谁：LangSmith + Langfuse + AgentOps。
- 氛围：可回放的执行证据，像调试器而不是普通详情页。
- 视觉关键词：span list、step timeline、tool call drawer、error/fallback banner。
- 用户确认点：是否把 Agent Run Trace 与 Generation Trace 合并为“Trace Evidence”展示逻辑。

### Human Review

- 参考谁：Linear + LangSmith feedback queue。
- 氛围：低噪审核队列，强调风险、状态、等待时间和可操作按钮。
- 视觉关键词：review queue、risk tag、diff/action summary、approve/reject/regenerate。
- 用户确认点：是否需要独立 `/reviews` 页面，还是先在 Trace Evidence 内作为详情模式。

### Knowledge Base / Prompt Templates

- 参考谁：Dify + Open WebUI + Promptfoo + Helicone。
- 氛围：配置台，不是文档库；每个知识和 Prompt 都要能解释“被哪个 run 使用”。
- 视觉关键词：document list、chunk inspector、citation preview、prompt variables、test run table。
- 用户确认点：是否接受把 mock 使用次数标注为 demo/derived，不写成后端真实统计。

### README 截图展示

- 参考谁：OpenHands + Flowise + Promptfoo + Landingfolio。
- 氛围：GitHub 作品集，不是商业官网；截图、命令、指标和边界先行。
- 视觉关键词：screenshot grid、one-line positioning、metrics snapshot、local startup。
- 用户确认点：README 是否保留“不能夸大的能力”表，增强面试可信度。

## 需要用户确认的点

- 是否确认第一阶段主视觉继续走“深色开发者工具 + 青绿色/Runtime Mint 信号色”，而不是紫蓝 AI 营销风。
- 是否确认不使用公开来源的截图、插图、Logo 或页面布局，只做结构借鉴。
- 是否确认 Human Review 先做独立页面，还是先作为 Trace Evidence 的详情区域。
- 是否确认 Dashboard 优先展示本地实测指标，不写任何真实用户、商业收益或生产性能。
- 是否确认 README 后续只引用 `docs/images/` 真实运行截图，不使用 `docs/design/references/` 的 concept image。
