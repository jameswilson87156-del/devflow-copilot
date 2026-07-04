# DevFlow Copilot 前端作品集化改造计划

计划日期：2026-07-04

本计划只定义后续 UI / README / 指标升级方向，不在本轮直接改正式页面。

## 用户与目标

- 目标用户：准备 Java 后端实习、AI 工具开发或 Java + AI 应用岗位投递的项目作者，以及面试时查看仓库的技术面试官。
- 产品目标：把 DevFlow Copilot 展示成一个可运行、可截图、可解释、数据可追溯的 AI Coding Workbench / Agentic Workflow Console。
- 使用场景：GitHub README、Boss 直聘项目描述、简历项目 bullet、面试现场演示、本地截图证据。
- 核心约束：不接真实 API Key，不包装 mock/local-rule，不复制公开项目代码或素材，不一次性大重构。

## 第一阶段信息架构

明确 5 个核心展示入口：

| 页面 | 当前状态 | 作品集目标 | 数据来源 |
| --- | --- | --- | --- |
| Dashboard 总览 | 已有真实页面 `/` | 展示 Today Runs、Success Rate、Avg Latency、Human Reviews、Tool Calls、Recent Runs | `GET /api/dashboard/stats` + Trace / Knowledge 派生 |
| Workbench 工作台 | 已有真实页面 `/workbench` | 展示 Prompt 输入、Provider 状态、Context、AI 输出、Run、Save、Confirm | Projects、Prompts、AI Generation、Generation Trace |
| Prompt Templates | 已有真实页面 `/prompts` | 展示模板列表、版本、场景、变量、试运行和使用次数派生 | Prompts、Generations、Traces |
| Trace Evidence | 已有 `/agent-runs` 和 `/history` | 聚合 Prompt、Provider、Tool Call、Result、Token Usage、Error/Fallback、Timeline | Agent Runs、Generation Traces、Tool Calls、Human Reviews |
| Human Review | 当前为 disabled 导航项 | 后续可独立成审核列表，或明确作为 Trace Evidence 的详情模式 | Human Review records + Generation status |

## 用户流程

### Flow：作品集演示闭环

- Goal：让面试官在 2-3 分钟内看到 DevFlow 的 AI 工作流不是聊天套壳，而是可追踪、可验证、可人工确认的工程系统。
- Trigger：打开 README 或本地 Dashboard。
- Entry Points：
  - README 顶部截图和启动命令。
  - 本地 `/` Dashboard。
  - 面试时直接打开 `/workbench` 或 `/agent-runs`。
- Steps：
  1. Dashboard 查看今日运行、最近 runs、人工审核队列和工具调用数量。
  2. Workbench 输入需求，运行 local-rule workflow。
  3. 查看生成结果、Provider、Knowledge 引用和 Trace。
  4. 保存并确认生成记录。
  5. Trace Evidence 回放 Prompt Render、Knowledge Retrieval、Provider Call、Tool Call、Human Review。
- Exit：面试官能追问每个指标和页面来源，用户能回到代码/脚本/测试证据。

### 状态覆盖

| 状态 | 页面表现要求 |
| --- | --- |
| Loading | 使用骨架行或固定高度占位，不使用泛用大 spinner。 |
| Empty | 说明当前未采集或未生成，不伪造数据。 |
| Partial | 接口缺字段时显示“未记录 / demo-only / UI-only”，并保留主流程。 |
| Error | 显示接口、步骤、错误摘要和可重试范围。 |
| Success | 展示真实 recordId、traceId、provider、model、latency、review status。 |
| Fallback | 明确标注 fallback reason，不写成真实模型成功。 |

## 视觉系统方向

优先沿用根目录 `DESIGN.md`：

- 深色 graphite / blue-black 背景。
- Runtime Mint / 青绿色作为唯一产品强调色，状态色只用于语义。
- 面板弱边框、低投影、紧凑行高、开发者工具感。
- Trace Spine 作为识别性图形语言：Prompt、Context、Provider、Tool、Review 节点串联。
- 半透明面板可以有轻微层次，但不使用毛玻璃、渐变光斑、霓虹光效或纯装饰图形。
- 所有状态必须有文字 + 图形标识，不仅依赖颜色。

## 组件与交互需求

| 组件 | 用途 | 核心状态 | 可访问性 |
| --- | --- | --- | --- |
| MetricTile / MetricRow | 展示本地实测指标 | loading、zero、stale、verified | 数字使用等宽字体，状态有文本说明。 |
| TraceSpine | 展示 workflow 时间线 | completed、running、failed、waiting、review | 节点可键盘聚焦，状态不只靠颜色。 |
| EvidencePanel | 展示 Prompt / JSON / Tool Call / Error | raw、summary、empty、copyable | 代码块支持复制和横向滚动。 |
| ReviewQueue | 展示待审核结果 | pending、saved、confirmed、rejected | 操作按钮命名具体范围。 |
| ProviderBadge | 展示 local-rule / openai-compatible / fallback | local、remote、fallback、failed | tooltip 解释是否真实 Provider。 |
| DemoDataLabel | 标注 seed / mock / local demo data | seed、ui-only、derived、measured | 避免用户误以为是真实线上数据。 |

## 响应式与截图验收

- 1440px：所有作品集截图优先按当前截图脚本尺寸生成，核心文字不能截断。
- 1280px：侧边栏可折叠，右侧 inspector 可变成抽屉。
- 1024px 以下：进入阅读/复核模式，多栏切为 tabs，保证无横向页面溢出。
- 每个核心页面至少准备一个可截图状态：有数据、空状态、错误/fallback 其中至少一种。

## README 升级计划

后续 README 升级应基于本轮文档和指标脚本，不直接写未采集数据：

1. 顶部一句话定位：`DevFlow Copilot 是一个面向 AI Coding 的 Agentic Workflow Console，用本地可演示闭环展示 Prompt、Provider、Trace、Tool Call 与 Human Review 工程化能力。`
2. 保留 6 张真实截图，并补充截图生成命令和最近指标快照链接。
3. 新增“数据指标快照”小节，只引用 `docs/metrics/metrics_snapshot.md` 中已采集项。
4. 新增“AI Workflow 流程图”，复用现有 Mermaid 架构图并突出 Prompt -> Provider -> Trace -> Review。
5. 简历亮点只写 fact，不写 assumption；所有 demo/local-rule/mock 边界用醒目但简洁的方式说明。
6. 增加“不能夸大的能力”短表，保持面试可信度。

## 分阶段实施建议

### 阶段 A：文档与指标收口

- 新增 `docs/portfolio_audit.md`、`docs/reference_research.md`、`docs/design_refs/README.md`、`docs/metrics_plan.md`、`docs/metrics/metrics_snapshot.md`、`docs/showcase_upgrade_plan.md`。
- 新增 `scripts/collect-portfolio-metrics.js`。
- 验收：`mvn test`、`npm run build`、指标脚本通过，git status clean。

### 阶段 B：README 作品集化升级

- 基于指标快照新增 README 指标小节。
- 补端口场景表：dev 8080 / Docker 18080 / screenshot env 18081。
- 保持所有截图来自 `docs/images/`。

### 阶段 C：Human Review / Trace Evidence 展示补强

- 优先新增或强化 Human Review 页面/详情，不改后端主流程。
- Trace Evidence 聚合 Generation Trace、Agent Run Trace、Tool Call 和 Review 状态。
- 所有缺失字段显示“未记录”，不造假。

### 阶段 D：性能与截图证据补强

- 增加 Playwright 页面性能或 Lighthouse 采集，若环境支持再写入 README。
- 重跑作品集截图脚本，记录截图生成时间和页面状态。

## 当前不做

- 不接真实 API Key。
- 不新增数据库或付费服务。
- 不复制 GitHub 项目代码、截图、Logo、样式或文案。
- 不把 concept image 当作真实截图。
- 不新增完整多 Agent Runtime、SSE、自动改代码、自动提交 Git、登录权限或向量数据库。
