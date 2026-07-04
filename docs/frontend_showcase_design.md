# 前端作品集展示设计方案

设计日期：2026-07-04

本文件是 DevFlow Copilot 正式前端设计方向门禁，不包含代码实现，不要求立即改 Vue 页面或样式。用户确认前不得进入前端代码改造。

## 1. 视觉定位

DevFlow Copilot 应呈现为 AI Coding Workbench / Agentic Workflow Console：深色、克制、数据密集、偏开发者工具，而不是普通后台管理系统、AI 聊天页或营销落地页。

核心气质：

- 像 IDE sidecar + observability console + review station 的组合。
- 第一眼看到 Prompt、Provider、Trace、Tool Call、Human Review 的工程链路。
- 所有数据都可追溯到本地 demo、接口、测试或指标脚本。
- 页面视觉要好看，但好看来自信息层级、状态清晰和截图可信，不来自装饰图。

## 2. 参考来源

主要参考来源来自 `docs/frontend_reference_research.md` 和 `docs/frontend_moodboard.md`：

- Dashboard：Langfuse、PostHog、Mobbin Admin Dashboard。
- Workbench：OpenHands、Raycast、Linear。
- Trace / Timeline：LangSmith、Langfuse、AgentOps、Arize Phoenix。
- Human Review：Linear、LangSmith feedback queue。
- Knowledge Base：Dify、Open WebUI。
- Prompt Templates：Promptfoo、Helicone、Vercel AI SDK。
- README 展示：OpenHands、Flowise、Promptfoo、Landingfolio。

只借鉴结构，不复制源代码、Logo、商标、截图、图片素材、完整页面布局或受版权保护的插图。

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
[Header: DevFlow Copilot / today scope / metrics source]
[Metric strip: Runs | Success | Avg latency | Reviews | Tool Calls]
[Main: Recent Runs table + Trace status]
[Right: Review Queue + Provider Health]
[Bottom: Metrics Snapshot + Recent Evidence]
```

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
| Dashboard | Today Runs、Success Rate、Avg Latency、Human Reviews、Tool Calls、Recent Runs、Review Queue、Metrics Snapshot |
| Workbench | Prompt 输入、Project Context、Provider 状态、Knowledge Query、AI 输出、Run、Save、Confirm、Trace Summary |
| Trace Evidence | Prompt Render、Provider Call、Tool Call、Result、Token Usage、Error/Fallback、Timeline、Human Review |
| Human Review | 待审核结果、风险提示、人工修改、审核状态、通过、驳回、重新生成、状态历史 |
| Knowledge Base | 文档列表、Chunk、关键词检索、引用预览、生成引用历史、embedding 预留状态 |
| Prompt Templates | 模板列表、模板版本、适用场景、变量、渲染预览、测试运行、使用次数 derived/demo 标识 |

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

- Langfuse / LangSmith：Trace、observability、feedback 信息架构。
- OpenHands / Linear / Raycast：开发者控制台、命令式操作、低噪审核队列。
- Dify / Open WebUI：Knowledge / Tool / Model / Prompt 分组。
- Promptfoo / Vercel AI SDK：Prompt 测试、Provider/Tool 说明、README 命令证据。
- PostHog / Mobbin：指标 dashboard 和真实 SaaS 密度。

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
