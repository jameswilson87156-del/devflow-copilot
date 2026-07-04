# 前端参考调研

访问日期：2026-07-04

本文件是 DevFlow Copilot 前端视觉与页面级设计门禁的第一步。已联网访问公开来源；只借鉴信息架构、页面层级、组件组织、交互思路、指标展示、README 展示方法和视觉氛围。

重要说明：

- `docs/reference_research.md` 是通用公开项目调研，不等于本文件的前端视觉参考调研。
- `docs/showcase_upgrade_plan.md` 是作品集升级计划，不等于页面级前端设计方案。
- `docs/design_refs/README.md` 是项目内参考图状态说明，不等于全网参考 moodboard。
- 不复制源代码、Logo、商标、产品文案、图片素材、完整页面布局或受版权保护插图。

## 候选参考来源

| # | 来源名称 | URL | 适合参考的页面类型 | 可借鉴的信息架构 | 可借鉴的视觉风格 | 不允许复制的部分 | 对 DevFlow Copilot 的具体启发 | 推荐优先级 | 第一阶段落地 | 不复制声明 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Langfuse | https://github.com/langfuse/langfuse | Trace、Prompt、Evaluation、Observability Dashboard | Trace / Prompt / Eval / Dataset 分区，按 LLM 调用链路组织 | 深色工程工具、表格、时间线、状态标签 | Logo、截图、完整页面、功能文案 | Trace Evidence 应把 Prompt Render、Knowledge Retrieval、Provider Call、Tool Call、Human Review 放进一条可解释链路 | P0 | 是 | 只借鉴 Trace 信息架构，不复制 UI |
| 2 | LangSmith Observability | https://docs.langchain.com/langsmith/observability | Trace Timeline、Dashboard、Feedback Queue | View traces、Monitor performance、Automations、Collect feedback | 数据密集、面向调试、强调筛选/导出/对比 | LangChain / LangSmith 品牌、trace 截图、文案 | Human Review 可参考 feedback / annotation queue；Dashboard 指标可参考 trace health | P0 | 是 | 只借鉴观测和反馈结构 |
| 3 | Helicone | https://www.helicone.ai/ | LLM Observability、Prompt Management、Session Trace | Request log、session、prompt env、latency/cost/quality 指标 | 现代 LLM dashboard、指标卡 + 请求表 + trace 详情 | 品牌、官网图、具体图表样式 | Prompt Studio 可展示 dev/staging/prod-like 状态，但必须标注当前只是本地 demo | P1 | 部分 | 只借鉴 prompt/env 组织 |
| 4 | Arize Phoenix | https://github.com/arize-ai/phoenix | AI Observability、Evaluation、Prompt Management | Tracing、Evaluation、Datasets、Experiments、Playground、Prompt Management | ML/LLM 实验台、tabbed inspector、trace replay | Phoenix UI、Logo、功能承诺 | DevFlow 的未来路线要把已实现 Trace 与未实现 Eval / Dataset 分开 | P1 | 部分 | 不复制产品能力表述 |
| 5 | AgentOps | https://github.com/AgentOps-AI/agentops | Agent Session Drilldown、Tool Call Trace | Session list、event breakdown、LLM calls、duration debug info | Agent 运行面板、session replay、事件分布 | Dashboard 截图、SDK 集成文案 | Agent Run Trace 可做左 run 列表 + 中 timeline + 右 Tool/Human Review inspector | P0 | 是 | 只借鉴 session drilldown |
| 6 | OpenHands | https://github.com/OpenHands/openhands | AI Coding Workbench、Agent Console、README Showcase | Developer control center、agent backend、quickstart、automation sections | 工程控制台、清晰 README 顶部截图 | Agent Canvas 视觉、Logo、自动化能力承诺 | Workbench 要强调“人确认前停止”，README 顶部可借鉴定位 + 截图 + quick links | P0 | 是 | 不复制代码或自动 coding claims |
| 7 | Dify | https://github.com/langgenius/dify | Agent Workflow、Knowledge Base、Provider Console | Workflow、model providers、knowledge、app builder 分区 | 面向 AI app 构建的多模块控制台 | 工作流 canvas、截图、品牌与文案 | Knowledge Base 页面可按 Document -> Chunk -> Retrieval -> Citation -> Generation 组织 | P0 | 是 | 不复制 workflow canvas |
| 8 | Flowise | https://github.com/FlowiseAI/Flowise | Visual Agent Workflow、README Demo | Quick Start、Docker、Docs、Self-host、visual builder 展示 | 节点式工作流、README 截图优先 | 节点编辑器、Logo、低代码定位 | README 可借鉴目录与启动方式组织；DevFlow 不做节点式低代码 | P1 | 部分 | 不复制节点图或安装文案 |
| 9 | Open WebUI | https://github.com/open-webui/open-webui | AI Platform、Knowledge、Tools、Model Console | Models、Agents、Knowledge、Tools、Notes、RBAC、install sections | 自托管 AI 平台、功能密集、README 图片展示 | Open WebUI branding、demo 图、权限能力 | DevFlow 可借鉴“Models / Tools / Knowledge”分组，但不能写 RBAC/离线平台能力 | P1 | 部分 | 不复制平台定位 |
| 10 | Vercel AI SDK | https://vercel.com/docs/ai-sdk | AI Developer Docs、Tool Calling、Provider Abstraction | Provider API、structured output、tool calling、streaming、examples | 开发者文档清晰、代码块强、章节层次强 | Vercel 品牌、示例代码、官网视觉 | Provider / Tool Call 说明可更工程化；README 可用“能力 + 代码层边界”组织 | P1 | 部分 | 不复制示例代码 |
| 11 | Promptfoo | https://github.com/promptfoo/promptfoo | PromptOps、Eval Matrix、README Demo | CLI + web viewer、automated eval、CI/CD、model comparison | 表格矩阵、结果对比、命令行与 UI 双证据 | Eval 矩阵截图、red teaming claims | Prompt Studio 可参考测试结果表，但只能写本地试运行和静态校验 | P0 | 是 | 不复制 eval 能力 |
| 12 | Supabase | https://supabase.com/ | Developer Platform Dashboard、Database Console | Project dashboard、database/auth/storage/vector 功能分组 | 开发者工具感、深色、代码与数据并置 | 品牌、产品图、绿色视觉识别 | DevFlow 的 Knowledge / Trace 表格可以参考“开发者平台”密度，不照搬绿色品牌 | P2 | 部分 | 只借鉴工具密度 |
| 13 | PostHog Dashboards | https://posthog.com/docs/product-analytics/dashboards | Metrics Dashboard、Product Health | Dashboards、templates、tracking common metrics over time | 指标可视化、可复用模板、轻量说明 | PostHog 插画、模板图、产品文案 | Dashboard 指标要区分常驻健康指标和 ad hoc 分析；metrics_snapshot 可作为数据源 | P0 | 是 | 不复制 chart 样式 |
| 14 | Linear | https://linear.app/ | Human + Agent Workflow、Review Queue、Issue/Project Console | Cycles、issues、projects、workflow shared by humans and agents | 极简、密集、键盘优先、低噪音 | Linear 品牌、交互细节、图标风格 | Human Review 队列可参考 Linear 的低噪列表、状态、负责人和优先级 | P0 | 是 | 不复制完整布局 |
| 15 | Raycast | https://www.raycast.com/ | Command Center、Keyboard-first Developer Tool | Command palette、extensions、quick actions、search-first | 快速、深色、命令式、轻量面板 | Raycast 品牌、launcher 视觉、商标 | Workbench 顶部可强化 command/search/action bar；不做 launcher 克隆 | P1 | 部分 | 只借鉴命令感 |
| 16 | Mobbin Admin Dashboard | https://mobbin.com/explore/web/screens/admin-dashboard | SaaS Dashboard Pattern | Side navigation、filters、tables、charts、empty states | 大量真实产品 pattern，可参考密度与布局 | Mobbin 截图内容、付费素材、完整布局 | Dashboard / Review / Prompt 列表可参考常见 pattern，但必须原创组合 | P1 | 部分 | 不复制截图和页面 |
| 17 | Mobbin Log Dashboard | https://mobbin.com/explore/web/screens/log-dashboard | Log / Observability Dashboard | Log list、filters、progress、timeline、show/hide controls | 面向日志和运维的紧凑数据 UI | Mobbin 截图和具体产品图 | Trace / Log Analyzer 可参考日志筛选和 timeline 密度 | P1 | 部分 | 不复制截图 |
| 18 | Dribbble Observability Dashboard | https://dribbble.com/search/observability-dashboard | Visual Mood Exploration | Observability、chart、data visualization 搜索集合 | 可观察性 dashboard 氛围参考 | 任何 shot 图、插图、布局、作者作品 | 只用于验证视觉方向，不能作为实现蓝图 | P2 | 否 | 不复制设计作品 |
| 19 | Landingfolio | https://www.landingfolio.com/ | README / Portfolio Landing Inspiration | Hero、demo section、feature block、screenshot storytelling | 高质量 landing 信息层级 | 模板、截图、商业文案 | README 可借鉴“截图先行 + 功能分组 + demo section”，但 DevFlow 不做营销页 | P2 | 部分 | 不复制模板 |
| 20 | Figma Make Design Systems | https://www.figma.com/solutions/ai-design-systems-generator/ | Design System / AI Design Workflow | Tokens、prototype、validate、publish、collaboration | 设计系统流程化表达 | Figma 产品图、AI 生成方案 | `frontend_showcase_design.md` 可采用 token / validation / handoff 结构 | P2 | 部分 | 不复制 Figma 视觉 |

## 第一阶段可用结论

- Dashboard：优先参考 Langfuse、PostHog、Mobbin Admin Dashboard，做数据可信的运行总览。
- Workbench：优先参考 OpenHands、Raycast、Linear，做 AI Coding 控制台而不是聊天页。
- Trace / Timeline：优先参考 LangSmith、Langfuse、AgentOps、Phoenix，做可回放的执行证据。
- Human Review：优先参考 Linear、LangSmith feedback、AgentOps session inspector，做低噪审核队列。
- Knowledge Base / Prompt Templates：优先参考 Dify、Open WebUI、Promptfoo、Helicone，做 Document / Chunk / Prompt / Test Run 的工程台。
- README 截图展示：参考 OpenHands、Flowise、Promptfoo、Landingfolio 的截图先行与命令/文档并列结构。
