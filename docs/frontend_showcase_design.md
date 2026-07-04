# 前端作品集展示设计方案

设计日期：2026-07-04

本文件是 DevFlow Copilot 正式前端设计方向门禁，不包含代码实现，不要求立即改 Vue 页面或样式。用户确认前不得进入前端代码改造。

本次更新依据：

- `docs/frontend_reference_screenshot_index.md`
- `.local/reference_screenshots/top_candidates.md`
- `docs/frontend_moodboard.md`
- 用户人工筛选后的第一梯队 / 第二梯队参考结论

## 1. 视觉定位

DevFlow Copilot 应呈现为 AI Coding Workbench / Agentic Workflow Console：深色、克制、数据密集、偏开发者工具，而不是普通后台管理系统、AI 聊天页或营销落地页。

核心气质：

- 像 IDE sidecar + observability console + review station 的组合。
- 第一眼看到 Prompt、Provider、Trace、Tool Call、Human Review 的工程链路。
- 所有数据都可追溯到本地 demo、接口、测试或指标脚本。
- 页面视觉要好看，但好看来自信息层级、状态清晰和截图可信，不来自装饰图。

### 1.1 中文企业级视觉方向

页面主体必须中文为主，技术词保留英文。整体表达应更接近中文企业级中后台 + AI Observability 控制台，而不是海外 SaaS 营销页。

中文主标签示例：

- 今日运行
- 成功率
- 平均耗时
- 待人工复核
- 工具调用
- 最近运行
- 执行证据
- 生成结果预览
- 知识引用
- 复核队列

保留英文技术词：

- Prompt
- Provider
- Trace
- Tool Call
- Human Review
- local-rule fallback
- OpenAI-compatible
- Token
- JSON

文案原则：

- 指标、状态、复核动作、数据来源优先中文。
- 技术对象、链路节点、Provider 类型保留英文。
- 不使用“智能赋能”“无限可能”“一站式提升效率”等营销表达。
- Demo 数据必须显式标注，例如 `Demo Data`、`来源：本地指标快照`。

## 2. 参考来源

主要参考来源来自 `docs/frontend_reference_screenshot_index.md`、`.local/reference_screenshots/top_candidates.md`、`docs/frontend_reference_research.md` 和 `docs/frontend_moodboard.md`。

### 2.1 人工筛选后的参考层级

第一梯队主参考：

- `01_ant_design_pro_analysis.png`：Dashboard 的中文企业级指标密度、筛选区、指标卡和图表节奏。
- `09_langfuse_home_or_github.png`：Trace Evidence / Dashboard 的 LLM Observability 氛围。
- `10_langsmith_observability.png`：Trace Evidence / Human Review 的 Trace + feedback 结构。
- `13_agentops_github.png`：Trace Evidence / Human Review 的 session drilldown、event breakdown、inspector 思路。
- `14_dify_github.png`：Knowledge Base / Workbench / Prompt Templates 的 AI 平台模块分区。
- `16_promptfoo_github.png`：Prompt Templates / README metrics 的 PromptOps 和证据展示。

第二梯队辅助参考：

- `08_aliyun_sls_dashboard.png`：Log Analyzer / Dashboard 的中文运维监控思路。
- `06_google_cloud_monitoring_dashboards.png`：Dashboard / Observability 的监控命名和结构。
- `02_ant_design_pro_monitor.png`：Dashboard / Trace 的监控卡片组织。
- `15_openwebui_github.png`：Knowledge / Provider / Tool grouping 的模块分类。

不作为主视觉参考：

- Arco / Semi 首页只作为设计系统和组件质感参考，不作为页面布局参考。
- Google Cloud / 华为云文档页只作为控制台叙事和术语参考，不作为主视觉参考。
- Helicone / Phoenix GitHub 页面只作为能力边界参考，不作为主视觉参考。

只借鉴结构，不复制源代码、Logo、商标、截图、图片素材、完整页面布局或受版权保护的插图。

### 2.2 页面级参考绑定

| 页面 | 主参考 | 辅助参考 | 目标 |
| --- | --- | --- | --- |
| Dashboard | `01_ant_design_pro_analysis.png` + `09_langfuse_home_or_github.png` | `08_aliyun_sls_dashboard.png` / `06_google_cloud_monitoring_dashboards.png` / `02_ant_design_pro_monitor.png` | 中文企业级指标看板 + AI Workflow 证据总览，不做普通后台 |
| Workbench | `14_dify_github.png` + DevFlow 原创 visual target concept | DevFlow 现有 Workbench 真实接口和状态机 | 需求输入、Prompt 模板、Provider 状态、生成 Artifact、变更摘要、知识引用、Trace 摘要、人工确认 |
| Trace Evidence | `09_langfuse_home_or_github.png` + `10_langsmith_observability.png` + `13_agentops_github.png` | `06_google_cloud_monitoring_dashboards.png` / `02_ant_design_pro_monitor.png` | Run list + Timeline + Step Inspector + Raw JSON / Rendered Prompt / Fallback Reason |
| Human Review | `10_langsmith_observability.png` + `13_agentops_github.png` | DevFlow 原创审核状态机 | 复核队列、风险标签、Artifact 预览、决策面板、复核原因、状态历史 |
| Knowledge Base | `14_dify_github.png` + `15_openwebui_github.png` | DevFlow 当前 Document / Chunk / Reference 数据结构 | Document -> Chunk -> Search -> Citation -> Generation Reference |
| Prompt Templates | `16_promptfoo_github.png` + `14_dify_github.png` | Vercel AI SDK / Promptfoo 的工程证据表达 | Template -> Variables -> Render Preview -> Test Run -> Trace metadata |
| README 展示 | `16_promptfoo_github.png` + OpenHands / Flowise README 展示方式 | 本地 `docs/images/` 真实截图和 `metrics_snapshot` | 真实截图、Quickstart、metrics snapshot、边界说明 |

## 3. 色彩系统

建议采用“蓝黑/石墨黑 + 青绿色信号色”的开发者工具调性，并兼容项目根目录 `DESIGN.md` 的 Runtime Mint 方向。

| 角色 | 建议值 | 用途 |
| --- | --- | --- |
| App Background | `#0B1110` / `#101312` | 页面底色，避免纯黑 |
| Panel Surface | `#151918` | 主面板、列表、编辑器 |
| Raised Surface | `#1A1F1D` | 右侧 inspector、选中详情 |
| Border | `#26302C` | 弱边框、分割线 |
| Primary Text | `#E7ECE9` | 标题和关键值 |
| Secondary Text | `#AAB5AF` | 描述和元信息 |
| Muted Text | `#6F7B75` | 时间戳、空状态 |
| Accent | `#63B59F` | 当前 step、主按钮、focus、健康状态 |
| Warning | `#C7A45A` | 待审核、风险提示 |
| Danger | `#D97A72` | 失败、驳回、错误 |
| Info | `#7F9CAA` | queued、neutral info |

限制：

- Accent 占屏幕面积应低于 8%。
- 不使用紫蓝大渐变、霓虹光效、装饰光斑。
- “玻璃卡片”只允许表现为轻微透明层和弱边框，不使用重度 `backdrop-filter` 毛玻璃。

## 4. 字体层级

| 层级 | 字号 / 行高 | 用途 |
| --- | --- | --- |
| 页面标题 | 20px / 28px | 每页一个主标题 |
| Section 标题 | 14px / 20px | 面板头部 |
| 正文 | 13px / 20px | 页面主要信息 |
| 表格 / 列表标签 | 12px / 18px | 高密度行 |
| 元信息 | 11px / 16px | 时间、版本、ID、状态 |
| Code / Trace | 12.5px / 20px | Prompt、JSON、日志、Diff |
| 关键数字 | 20-28px / 1.1 | Dashboard 指标，使用等宽数字 |

建议字体：

- Interface：`Geist`, `Noto Sans SC`, `Microsoft YaHei UI`, sans-serif。
- Mono：`Geist Mono`, `JetBrains Mono`, `Noto Sans Mono CJK SC`, monospace。

## 5. 卡片样式

- 卡片/面板圆角：6px，最大不超过 8px。
- Border：1px 弱边框，颜色接近 `#26302C`。
- Shadow：默认不用重阴影，只在菜单/浮层中使用轻微阴影。
- 列表优先使用连续行和分割线，不做一屏大卡片堆叠。
- 选中态使用左侧 2px accent line 或底部 accent tab，不使用大面积高亮。
- 所有状态 badge 必须有文字，不只靠颜色。

## 6. 页面布局

全局布局：

```text
┌──────────────┬────────────────────────────────────────────┐
│ Sidebar      │ Top command / breadcrumb bar               │
│ 220 / 56px   ├────────────────────────────────────────────┤
│              │ Page header + dense workspace              │
│              │                                            │
└──────────────┴────────────────────────────────────────────┘
```

布局原则：

- Desktop-first，1440px 截图优先。
- 12-column grid，主内容区必须有一个明显主焦点。
- Workbench / Trace / Knowledge 使用 split panes。
- 1280px 以下收起侧边栏，右侧 inspector 改为抽屉。
- 不出现横向页面溢出；代码块内部可横向滚动。

### 6.1 Implementation Visual Spec

| 项目 | 规格 |
| --- | --- |
| Screenshot viewport | `1440x900` 或 `1440x1000` |
| Sidebar 宽度 | `220px` |
| Topbar 高度 | `52px` |
| Page padding | `24px` |
| Panel padding | `16px` |
| Grid gap | `16px` |
| Card radius | `6px`，最大不超过 `8px` |
| Table row height | `40-44px` |
| Inspector 宽度 | `320-360px` |
| Workbench split | `28% / 46% / 26%` |
| Trace split | `24% / 42% / 34%` |
| Code block | 使用等宽字体 |
| Technical metadata | `traceId`、`provider`、`latency`、`token`、`model` 使用等宽字体 |
| Badge | 小圆点 + 文案，不只靠颜色 |
| Demo label | 固定使用 `Demo Data / 来源：本地指标快照` 等标签 |
| Disabled page | 标注 `配置预留 / 当前通过环境变量配置` |

落地要求：

- 所有指标区必须能说明数据来源。
- 所有 demo / derived / UI-only 数据必须显式标注。
- inspector 面板优先承载证据详情，不做装饰性侧栏。
- 表格与列表应保持稳定行高，避免截图时文字拥挤或错位。

## 7. 导航结构

第一阶段建议导航：

- 工作流
  - Dashboard
  - Workbench
  - Prompt Templates
- 可观测性
  - Trace Evidence
  - Generation History
  - Log Analyzer
- 知识与引用
  - Knowledge Base
- 治理与审核
  - Human Review
- 配置
  - Provider（可 disabled，说明通过环境变量配置）
  - Settings（可 disabled）

注意：disabled 项必须明确说明“当前在哪里查看该能力”，不能制造假页面。

## 8. 核心页面草图说明

### Dashboard

```text
[Product Evidence Hero: DevFlow Copilot / AI Coding 工作台 / Agentic Workflow 控制台]
[Mode strip: 本地 Demo 模式 / local-rule fallback / OpenAI-compatible 可选 / 来源：metrics_snapshot]
[Workflow chain: Prompt -> Provider -> Trace -> Tool Call -> Human Review]
[Metric strip: 今日运行 | 成功率 | 平均耗时 | 待人工复核 | 工具调用]
[Main: 最近运行 table + Trace 状态]
[Right: 复核队列 + Provider Health]
[Bottom: Metrics Snapshot + 执行证据]
```

Dashboard 顶部需要 Product Evidence Hero，不做营销 Hero。内容必须包含：

- DevFlow Copilot
- AI Coding 工作台 / Agentic Workflow 控制台
- 本地 Demo 模式
- local-rule fallback
- OpenAI-compatible 可选
- 数据来源：本地 metrics_snapshot
- Prompt -> Provider -> Trace -> Tool Call -> Human Review 链路

### Workbench

```text
[Left: Requirement / Context / Prompt Template]
[Center: Generated Artifact + Run controls + Save / Confirm]
[Right: Provider + Knowledge References + Trace summary]
[Bottom: Timeline / Tool Calls / Logs]
```

### Trace Evidence

```text
[Left: Run list]
[Center: Trace timeline / steps / spans]
[Right: selected step inspector: Prompt, Provider, Tool Call, Error, Review]
[Bottom: raw JSON / rendered prompt / fallback detail]
```

Trace Evidence 是核心展示页，必须比普通历史详情更强。Timeline 步骤中文化为：

1. Prompt 渲染
2. 知识命中
3. Provider 选择
4. Tool Call 模拟
5. 生成记录
6. 需要人工复核
7. 已确认 / 已驳回

Step Inspector 必须支持：

- Raw JSON
- Rendered Prompt
- Provider / Model / Token / Latency
- Tool Call 输入输出摘要
- local-rule fallback reason
- Human Review 状态与复核原因

### Human Review

```text
[Left: Review queue filters]
[Center: pending review list with risk / wait time / artifact]
[Right: artifact preview + decision panel]
[Bottom: review history and state transitions]
```

### Knowledge Base

```text
[Left: Document list]
[Center: Document detail + Chunk list + Search]
[Right: Citation preview + generation usage]
[Bottom: reference history / index status]
```

### Prompt Templates

```text
[Left: Template registry]
[Center: Prompt editor + variables + render preview]
[Right: Test run result + Provider / Trace metadata]
[Bottom: version history / validation / recent runs]
```

## 9. 每个页面的信息模块

| 页面 | 信息模块 |
| --- | --- |
| Dashboard | 今日运行、成功率、平均耗时、待人工复核、工具调用、最近运行、复核队列、Metrics Snapshot、执行证据 |
| Workbench | Prompt 输入、Project Context、Provider 状态、Knowledge Query、生成结果预览、Run、Save、Confirm、Trace Summary |
| Trace Evidence | Prompt 渲染、Provider Call、Tool Call、Result、Token Usage、Error/Fallback、Timeline、Human Review |
| Human Review | 待审核结果、风险标签、人工修改、审核状态、通过、驳回、重新生成、状态历史、复核原因 |
| Knowledge Base | 文档列表、Chunk、关键词检索、知识引用、引用预览、生成引用历史、embedding 预留状态 |
| Prompt Templates | 模板列表、模板版本、适用场景、变量、渲染预览、测试运行、Trace metadata、使用次数 derived/demo 标识 |

## 10. 每个页面的截图重点

| 页面 | 截图重点 |
| --- | --- |
| Dashboard | 一屏看到真实指标、最近运行、审核队列和 Trace 健康状态 |
| Workbench | 运行工作流后，输出结果、Provider、Knowledge、Save / Confirm 同屏 |
| Trace Evidence | Timeline + Tool Call + Human Review inspector 同屏 |
| Human Review | 待审核 Artifact、风险、决策按钮、状态历史同屏 |
| Knowledge Base | Document -> Chunk -> Search -> Citation -> Generation Reference 链路 |
| Prompt Templates | Template -> Variables -> Render Preview -> Test Run -> Trace metadata 链路 |

## 11. README 展示截图顺序

推荐顺序：

1. Dashboard：证明这是工作流控制台，不是单页 demo。
2. Workbench：证明核心生成链路可操作。
3. Trace Evidence：证明生成过程可解释。
4. Human Review：证明 AI 输出需要人工确认。
5. Knowledge Base：证明 RAG 引用链路存在。
6. Prompt Templates：证明 Prompt 可版本化、可试运行。
7. Metrics Snapshot：证明数据来自脚本，不是口头描述。

README 不应使用 `docs/design/references/` 或设计站点图片，只使用本地真实运行截图。

## 12. 借鉴与原创边界

借鉴：

- Ant Design Pro Analysis：中文企业级 Dashboard 的指标密度、筛选区、指标卡和图表节奏。
- Langfuse / LangSmith / AgentOps：Trace、observability、feedback、session drilldown、Step Inspector 信息架构。
- Dify / Open WebUI：Knowledge / Tool / Model / Prompt 分组。
- Promptfoo / Vercel AI SDK：Prompt 测试、Provider / Tool 说明、README 命令证据。
- Alibaba Cloud SLS / Google Cloud Monitoring：中文运维监控思路、observability 命名和结构。

原创：

- DevFlow 的核心链路：Prompt -> Provider -> Generation Trace -> Agent Run Trace -> Tool Call -> Human Review。
- local-rule / OpenAI-compatible 的边界表达。
- Java 后端工程化项目的面试证据组织。
- 本地 metrics snapshot 和截图脚本驱动的作品集证据。

## 13. 绝对不能照搬

- 不复制任何公开项目源代码。
- 不复制 Logo、商标、品牌色、图片素材或受版权保护截图。
- 不复制完整页面布局。
- 不复制产品文案或功能承诺。
- 不把 local-rule 包装成真实 LLM。
- 不把 demo seed / mock / UI-only 字段写成真实生产数据。
- 不把 Keyword Knowledge Base 写成向量数据库。
- 不把 Agent Run Trace 写成完整多 Agent Runtime。
- 不把参考截图放进 `docs/images` 或 README。
- 不把未实现的 Eval、RBAC、多 Agent Runtime、向量数据库写成已实现。
- 不复制第三方页面布局、产品承诺或截图视觉细节。

## 14. 用户确认清单

请确认以下方向后再进入前端代码改造：

- [ ] 同意主视觉走深色开发者工具 + 青绿色信号色。
- [ ] 同意不使用公开来源截图/Logo/完整布局，只借鉴结构。
- [ ] 同意 Dashboard 优先展示本地可采集指标，不写未验证性能。
- [ ] 同意 Workbench 保持“人工确认前停止”，不做自动改代码承诺。
- [ ] 同意 Trace Evidence 强调可解释证据，不模拟隐藏推理链。
- [ ] 同意 Human Review 是否做独立页面：待确认。
- [ ] 同意 README 只使用本地真实截图和指标快照。

用户未确认前，本文件只作为设计门禁，不进入 Vue / CSS / README 截图改造。
