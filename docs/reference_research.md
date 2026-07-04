# DevFlow Copilot 公开参考调研

调研日期：2026-07-04

调研方式：联网访问 GitHub / 官方文档 / 官网公开页面，仅观察产品结构、README 展示、信息架构、指标设计和交互组织；不复制代码、样式、图片、商标、Logo 或文案。

## 参考项目汇总

| 项目 | 链接 | 方向 | 第一阶段是否采用 |
| --- | --- | --- | --- |
| OpenHands | https://github.com/OpenHands/openhands | AI Coding Workbench / Coding Agent 控制台 | 部分采用 |
| Langfuse | https://github.com/langfuse/langfuse | LLM Observability / Trace / Prompt Management | 采用 |
| Arize Phoenix | https://github.com/arize-ai/phoenix | AI Observability / Evaluation / Prompt Management | 部分采用 |
| Dify | https://github.com/langgenius/dify | Agentic Workflow / Knowledge Base / Provider | 部分采用 |
| Flowise | https://github.com/FlowiseAI/Flowise | Visual Agent Workflow Builder | 参考 README 与启动组织 |
| Promptfoo | https://github.com/promptfoo/promptfoo | PromptOps / Eval / CI | 采用指标思路 |
| AgentOps | https://github.com/AgentOps-AI/agentops | Agent Observability / Session Drilldown | 部分采用 |

## OpenHands

- 项目名称：OpenHands
- 链接：https://github.com/OpenHands/openhands
- 公开信息观察：README 将项目定位为 self-hosted developer control center for coding agents and automations，并展示 Agent Canvas 预览、Quickstart、Docs、Self-Hosting 等入口。
- 可以借鉴的地方：
  - 一句话定位直接说明“开发者控制中心”，适合作品集首屏标题。
  - README 顶部先放产品定位、截图/预览、快速入口，再放细节。
  - 把 coding agent 的执行范围、运行后端和自托管能力作为产品解释重点。
- 不能直接复制的地方：
  - 不能复制 OpenHands 的 Logo、截图、README 文案或 Agent Canvas 视觉。
  - DevFlow 当前不自动修改代码、不执行真实工程任务，不能写成 OpenHands 同等能力。
- 对 DevFlow Copilot 的启发：
  - README 可以改成“AI Coding Workbench / Agentic Workflow Console”并突出本地可演示、可追踪、可人工确认。
  - Workbench 应强调“人确认前停止”，而不是包装成自治 agent。
- 是否适合当前项目第一阶段采用：部分采用，主要用于 README 定位和页面信息架构。

## Langfuse

- 项目名称：Langfuse
- 链接：https://github.com/langfuse/langfuse
- 公开信息观察：核心功能包含 LLM Application Observability、Prompt Management、Evaluations；README 强调 traces 可跟踪 LLM calls、retrieval、embedding 或 agent actions，并可调试复杂日志和 sessions。
- 可以借鉴的地方：
  - Trace 页面按调用、检索、工具、状态、耗时组织。
  - Prompt Management 需要版本、缓存、协作、变更记录等概念。
  - Evaluations / user feedback / manual labeling 的表达可以启发 Human Review 页面。
- 不能直接复制的地方：
  - DevFlow 不能声称已有 Langfuse 级 eval、cost dashboard、dataset 或 production monitoring。
  - 不能复制 Langfuse 的 UI 截图和功能文案。
- 对 DevFlow Copilot 的启发：
  - Trace Evidence 页面应把 Prompt Render、Knowledge Retrieval、Provider Call、Tool Call、Human Review 放在同一条时间线。
  - README 应把 token/latency/trace 明确写成本地演示指标，并说明 local-rule token 是估算。
- 是否适合当前项目第一阶段采用：采用，主要用于 Trace / Prompt / Review 信息组织。

## Arize Phoenix

- 项目名称：Arize Phoenix
- 链接：https://github.com/arize-ai/phoenix
- 公开信息观察：Phoenix 将能力拆成 Tracing、Evaluation、Datasets、Experiments、Playground、Prompt Management，并强调 OpenTelemetry-based instrumentation 和 replay traced calls。
- 可以借鉴的地方：
  - 把 Observability 和 Evaluation 拆成清晰导航，而不是都塞进一个 dashboard。
  - Trace replay、prompt experiments、retrieval eval 这些概念可作为未来路线，但需要边界说明。
  - README 功能说明按动词 + 价值组织，适合 DevFlow 的“功能证据”章节。
- 不能直接复制的地方：
  - DevFlow 当前没有 OpenTelemetry 接入、datasets、experiments、playground 级能力，不能写进已实现。
  - 不复制 Phoenix 的产品截图、Logo 或指标命名。
- 对 DevFlow Copilot 的启发：
  - `metrics_plan.md` 应区分“已采集指标”和“待接入指标”。
  - Prompt Studio 可以展示“试运行记录”和“版本变更”，但不能包装成自动评测平台。
- 是否适合当前项目第一阶段采用：部分采用，用于指标分类和未来路线边界。

## Dify

- 项目名称：Dify
- 链接：https://github.com/langgenius/dify
- 公开信息观察：README 将 Key features 组织为 Workflow、Model support 等，强调可构建并测试 AI workflows，支持多 provider。
- 可以借鉴的地方：
  - README 的 Key features 按业务能力分组，适合作品集项目说明。
  - Workflow / Knowledge / Provider 可以作为 DevFlow 的三条主能力线。
  - Knowledge Base 页面可展示文档、Chunk、检索和引用链路。
- 不能直接复制的地方：
  - DevFlow 没有 Dify 级 workflow canvas、模型市场、完整应用发布和多 provider 生态。
  - 不能复制 Dify UI、官网图片、Logo 或“production-ready platform”式定位。
- 对 DevFlow Copilot 的启发：
  - README 和 Dashboard 可用“Prompt -> Provider -> Trace -> Review”解释核心链路。
  - Knowledge Base 必须写清楚当前是关键词/简单相似度检索。
- 是否适合当前项目第一阶段采用：部分采用，用于能力分组和知识库链路表达。

## Flowise

- 项目名称：Flowise
- 链接：https://github.com/FlowiseAI/Flowise
- 公开信息观察：README 用“Build AI Agents, Visually”作为强定位，并提供 Quick Start、Docker、Developers、Env Variables、Documentation、Self Host 等清晰目录。
- 可以借鉴的地方：
  - README 快速启动和 Docker 部分结构清楚，适合 DevFlow 后续整理启动方式。
  - 视觉工作流产品通常需要截图先行，README 顶部应明确真实截图来源。
  - 自托管/环境变量说明可以作为公开 demo 部署说明参考。
- 不能直接复制的地方：
  - DevFlow 不是可视化低代码 agent builder，不能借用“visually build agents”定位。
  - 不复制 Flowise 的图标、README badge、截图或安装文案。
- 对 DevFlow Copilot 的启发：
  - README 应补一张端口与环境变量表，减少本地、截图、Docker、生产 demo 的混淆。
  - 截图脚本和真实截图应成为作品集核心证据。
- 是否适合当前项目第一阶段采用：参考 README 结构，不作为功能对标。

## Promptfoo

- 项目名称：Promptfoo
- 链接：https://github.com/promptfoo/promptfoo
- 公开信息观察：README 强调自动化 eval、red teaming、模型对比、CI/CD、PR 检查、Web viewer，并提示多数 LLM Provider 需要 API Key。
- 可以借鉴的地方：
  - 指标思路：prompt / model / output 对比、测试矩阵、CI 结果、数据驱动决策。
  - README 将命令行和 Web viewer 都展示出来，适合 DevFlow 的脚本 + 页面双证据。
  - 对 API Key 的配置说明明确，应学习其“通过环境变量配置”的边界表达。
- 不能直接复制的地方：
  - DevFlow 当前没有 automated eval、red teaming、LLM-as-judge 或 PR code scanning，不能写成已实现。
  - 不能复制 Promptfoo 的截图、矩阵样式或性能/用户规模表述。
- 对 DevFlow Copilot 的启发：
  - `metrics_plan.md` 应让每个指标都有采集命令和是否可写简历的判断。
  - Prompt Studio 可先保留“本地试运行 + 静态校验”，不要包装成 PromptOps 平台。
- 是否适合当前项目第一阶段采用：采用指标和 README 展示思路。

## AgentOps

- 项目名称：AgentOps
- 链接：https://github.com/AgentOps-AI/agentops
- 官方文档：https://docs.agentops.ai/v2/introduction
- 公开信息观察：官方文档介绍 AgentOps Dashboard 可记录 session，并展示总耗时、SDK 版本、LLM calls、事件类型和耗时分布等调试信息。
- 可以借鉴的地方：
  - Agent Run Trace 可用 session drilldown 形式组织：左侧 run 列表、中间 timeline、右侧事件/调用详情。
  - Dashboard 指标可以围绕 execution time、event type breakdown、failure/debug info。
  - 每次运行形成可回放记录，适合 DevFlow 的“Trace Evidence”页面。
- 不能直接复制的地方：
  - DevFlow 没有 AgentOps SDK、自动监控、云 dashboard 或 2-line integration 能力。
  - 不能复制 AgentOps 的页面图或 agent framework 支持列表。
- 对 DevFlow Copilot 的启发：
  - Agent Run Trace 需要突出 `tool_call_record` 与 `human_review`，让面试官看到可解释链路。
  - 指标快照可以采集“Trace 节点类型数 / Tool Call 类型数 / Human Review 状态数”。
- 是否适合当前项目第一阶段采用：部分采用，用于 Trace Evidence 和指标设计。

## 第一阶段可采用结论

- README 展示：借鉴 OpenHands / Flowise 的“定位 + 截图 + Quickstart + Self-host/env”结构。
- Dashboard 指标：借鉴 Langfuse / AgentOps 的 runs、latency、status、review、tool calls，但只写本地采集数据。
- Trace 页面：借鉴 Langfuse / Phoenix / AgentOps 的 timeline、span、session drilldown，不模拟复杂多 agent。
- Prompt / Eval：借鉴 Promptfoo / Phoenix 的“测试矩阵、CI、版本、实验”概念，但当前只做本地试运行和静态校验。
- Knowledge Base：借鉴 Dify 的文档 -> Chunk -> 检索 -> 引用链路，但继续标注不是向量数据库。
