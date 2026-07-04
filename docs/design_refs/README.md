# DevFlow Copilot 前端参考图索引

创建日期：2026-07-04

## 当前参考图状态

- fact：`docs/design_refs/` 是本轮新增的参考图索引目录，目前本目录内没有图片文件。
- fact：项目内已存在参考图目录 `docs/design/references/`，其中包含 5 张 AI-generated UI concept images。这些图片不是本轮生成，也不是运行截图。
- fact：真实运行截图仍以 `docs/images/` 为准，README 主展示图不得使用 concept image。
- boundary：后续前端改造可以参考这些本地 concept images 的布局和信息层级，但不能把它们写成 GitHub 调研来源，也不能把它们当作真实产品截图。

## 已发现的本地参考图

| 文件 | 当前路径 | 可借鉴的地方 | 适合页面 | 不适合照搬的地方 |
| --- | --- | --- | --- | --- |
| `01-dashboard-ai-concept-cn.png` | `docs/design/references/01-dashboard-ai-concept-cn.png` | Dashboard 总览、KPI、运行列表、Trace 概览的信息层级 | Dashboard 总览 | 不可照搬图片、Logo、精确布局；指标必须来自脚本或接口。 |
| `02-workbench-ai-concept-cn.png` | `docs/design/references/02-workbench-ai-concept-cn.png` | 左配置、中结果、右详情、底部 Trace 的工作台结构 | Workbench 工作台 | 不可把 UI-only 字段写成后端已持久化字段。 |
| `03-agent-run-trace-ai-concept-cn.png` | `docs/design/references/03-agent-run-trace-ai-concept-cn.png` | Timeline、Tool Call、状态历史、JSON/日志详情 | Trace Evidence / Agent Run Trace | 不可包装成完整多 Agent Runtime。 |
| `04-knowledge-base-ai-concept-cn.png` | `docs/design/references/04-knowledge-base-ai-concept-cn.png` | 文档列表、Chunk、检索结果、引用历史 | Knowledge Base | 不可把关键词检索写成向量数据库。 |
| `05-prompt-studio-ai-concept-cn.png` | `docs/design/references/05-prompt-studio-ai-concept-cn.png` | 模板列表、变量、渲染预览、测试运行 | Prompt Templates / Prompt Studio | 不可写成 LLM-as-Judge 或自动评测平台。 |

## 仍建议用户补充的非 AI / 公开参考

如果后续要进行更强的前端作品集化改造，建议用户额外提供或确认以下参考来源：

- Dashboard 公开产品截图 1 张：偏 LLM observability / agent dashboard。
- Workbench 工作台公开产品截图 1 张：偏 IDE sidecar / coding agent console。
- Trace / Timeline 公开产品截图 1 张：偏 span trace / session drilldown。
- Human Review / 审核页面公开产品截图 1 张：偏 review queue / approval workflow。
- Portfolio README 展示参考 1 个：偏 GitHub README 顶部截图和指标组织。

## 统一视觉方向建议

- 保持深色 AI SaaS / 开发者工具感，但优先沿用项目根目录 `DESIGN.md` 的 graphite + Runtime Mint 方向。
- 使用蓝黑或石墨黑背景、弱边框、紧凑信息密度、Trace Spine、状态形状标识和真实截图。
- “玻璃卡片”建议降级为低透明度面板和边框层次，不使用 `backdrop-filter` 毛玻璃、发光、渐变 blob 或纯装饰背景。
- 每页必须有一个主操作焦点，不做普通后台管理式 KPI 卡片堆叠。
- 每个截图页都要保留真实数据来源说明：接口数据、seed demo、local-rule 或 UI-only demo 字段必须区分。
