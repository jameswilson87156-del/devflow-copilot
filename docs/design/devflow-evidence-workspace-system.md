# DevFlow Evidence Workspace System

状态：Phase 1 实现合同
视觉语言：**Layered Graphite**
Golden Slice：`Run Evidence Workspace`
范围：Design Token、最小 App Shell、Trace Golden Slice、View Model、响应式与候选截图

## 1. 产品设计原则

DevFlow 是 AI Coding Workflow 的证据工作台，不是聊天套壳、通用监控大屏或 IDE 仿制品。第一阶段围绕一个稳定任务组织页面：

> 选择 Run → 阅读真实 AgentStep → 检查关联 Evidence / Tool → 查看或执行当前 API 允许的 Human Review 动作。

实现必须遵守：

1. **真实数据优先**：Evidence Stream 只来自 `trace.steps`，按 `stepOrder` 排序。
2. **证据来源可见**：Direct、Derived、Unavailable 必须显示来源或缺失，不用视觉确定性掩盖数据不确定性。
3. **选择同步**：Run、Event 与 Inspector 共享一个明确 selection。
4. **状态不只靠颜色**：状态必须同时包含文字、标记形状和颜色。
5. **长内容是正常状态**：中文标题允许多行，JSON 在自己的滚动区域内查看。
6. **界面不宣称因果**：顺序轨道表达记录顺序，不宣称 AgentStep 之间存在后端未记录的绝对因果。
7. **操作能力不伪造**：不存在的写接口保持 Disabled，并写明边界。

## 2. 为什么选择 Layered Graphite

Layered Graphite 保留 DevFlow 深色工程产品定位，但用层级、seam、窄状态轨和局部对比建立信息结构，而不是纯黑背景、霓虹发光或 KPI 卡片墙。

它适合 DevFlow 的原因：

- Trace、Tool Call、Generation Trace 与 Human Review 都是高密度、可追溯工程数据；
- 深石墨环境能让不同 evidence provenance 和状态持续可读；
- Evidence Stream 是页面主对象，Inspector 是第二主对象，Run Ledger 只承担导航；
- 一张真实页面截图即可表达 DevFlow 的工程差异，而不需要装饰性图表。

它与项目现有其他页面的区别：

- 不复制 Dashboard 的 Hero + KPI 结构；
- 不复制 Workbench 的表单 + Artifact Preview 三栏；
- 不把 Trace 做成 IDE 文件树、Terminal 或普通 CRUD 明细页；
- 以 Evidence selection 和 provenance 为核心，形成 Trace 专属工作流语言。

后续全站只共享 Token、Surface、Status、Focus、App Shell 与基础 panel seam；不会强制其他页面复制 Evidence Stream。

## 3. Surface 层级

| 层级 | Token | 用途 |
|---|---|---|
| Level 0 | `--surface-shell` / `--surface-navigation` | 全局背景、Sidebar、Top Bar |
| Level 1 | `--surface-workspace` | 页面工作区与工具区背景 |
| Level 2 | `--surface-panel` | Run Ledger、Evidence Stream、Inspector |
| Level 3 | `--surface-elevated` / `--surface-selected` | 选中行、工具条、局部展开区 |
| Level 4 | `--surface-overlay` | Tablet Inspector、Mobile Bottom Sheet 遮罩 |

约束：

- 不使用纯黑；
- panel 主要依靠 `--line-subtle` seam 分隔，不把每个字段做成卡片；
- `--surface-selected` 只用于当前选择；
- overlay 只在抽屉打开时出现。

## 4. Typography

| 角色 | Token / 下限 | 规则 |
|---|---|---|
| 页面标题 | `--type-page`，20–24px | 最多三行，桌面优先单行 |
| 区域标题 | `--type-region`，15–18px | Run Ledger、Stream、Inspector |
| Event 标题 | `--type-event`，14–16px | 可读性优先 |
| 正文 | `--type-body`，13px | 中文行高约 1.62 |
| 辅助正文 | `--type-support`，12px | 不再大量使用 7–9px |
| Mono 元数据 | `--type-mono`，11px | ID、type、timestamp、JSON |
| 按钮 | 至少 12px | 高度至少 32px |

只有 panel kicker、紧凑 provenance 标签允许 9–10px；它们不是主阅读内容。

## 5. Spacing 与 Radius

Spacing 使用稳定 4px 倍数：`4 / 8 / 12 / 16 / 20 / 24 / 32 / 40`。

Radius：

- 小控件：`--radius-control: 4px`
- 面板：`--radius-panel: 8px`
- 浮层：`--radius-float: 12px`

大面积使用极大圆角、胶囊卡片或相同尺寸模块会削弱工程层级，因此禁止。

## 6. Status 与 Provenance

### 6.1 Status

| 语义 | Token | 形状 / 文案 |
|---|---|---|
| Success | `--status-success` | 圆点 + “成功 / 已确认” |
| Running | `--status-running` | 环形标记 + “运行中” |
| Pending | `--status-pending` | 方角标记 + “待处理 / 待复核” |
| Attention | `--status-attention` | 方角 `!` + 明确原因 |
| Error | `--status-error` | 方角 `×` + “失败 / 错误” |
| Derived | `--status-derived` | 文案必须以 `Derived` 开头 |
| Unavailable | `--status-unavailable` | `—` + “未记录 / Unavailable” |

### 6.2 Provenance

- **Direct**：来自 DTO 的字段，例如 `agent_step`、`human_review`。
- **Derived**：前端整理或计数，必须显式写 `Derived`。
- **Unavailable**：字段缺失，显示“未记录”，不设置默认 Provider、Model、Review 结果或 Risk。

## 7. Evidence Stream

Evidence Stream 是 Phase 1 第一主对象。

每个 Event 只对应一个真实 `AgentStep`，展示：

- `stepOrder`
- `stepType`
- `stepName`
- `status`
- `summary`
- `latencyMs`
- `startedAt / completedAt`
- 与该 `step.id` 精确匹配的 Tool Call 数量
- `Direct · agent_step` provenance

规则：

1. `trace.steps` 按 `stepOrder` 排序，同序时只用 `id` 保证稳定显示。
2. 不补 Prompt、Knowledge、Provider、Record、Review 等固定七步。
3. 两个 Step 只生成两个 Event。
4. Tool Call 只按 `stepId` 关联；无 `stepId` 或指向不存在 Step 的记录进入 Run-level Linked Evidence。
5. 上下键切换 selection，Enter / Space 保留按钮原生选择语义。
6. Focus 必须可见。

## 8. Inspector

Inspector 是第二主对象，包含五个标签页：

### Context

当前 AgentStep 的 Summary、Status、Source、Provenance、Latency、Started、Completed 与 Missing Fields。

### Evidence

只展示可确认的 Run-level 关联：

- Generation Record
- Generation Trace
- Knowledge Reference
- Provider / Model
- 真实 `errorMessage`
- Generated Output

Provider / Model 缺失显示“未记录”。`local-rule` 只说明 Local demo route，不自动代表 Provider 失败。

### Tools

- 当前 Step：只展示精确 `stepId` 命中的 Tool Call；
- Run-level：展示无可靠 Step 关联的 Tool Call；
- Input / Output 缺失显示“未记录”。

### Snapshot

名称固定为 **Normalized Snapshot**。它是前端 DTO normalization 的 Derived 对象，不命名为 Raw JSON。长 JSON 内部横向和纵向滚动，可复制，不扩大页面宽度。

### Review

复核状态、Reviewer、Comment、Created / Updated 与操作边界。它复用独立 Decision Ledger。

## 9. Decision Ledger

Decision Ledger 表达：

- 当前真实 Review 状态；
- 进入复核原因：如果 DTO 没有字段，显示“未记录”；
- 可用 Evidence 数量；
- 缺失 Evidence 数量；
- Reviewer、Comment、时间；
- 当前 API 允许的操作。

现有 Generation API 支持：

- `READY_FOR_REVIEW` 时 Save；
- `SAVED` 时 Confirm。

Request Changes / Reject 没有现有写接口，因此始终 Disabled 并展示边界。无 `humanReviews` 时显示“未进入人工复核”，禁止显示“复核已完成”。

## 10. 响应式规则

### Desktop ≥ 1181px

三栏：

- Run Ledger：216–248px
- Evidence Stream：`minmax(480px, 1fr)`
- Inspector：310–352px

页面在 viewport 内形成工作台，三个 panel 各自滚动。

### Tablet 721–1180px

- App Shell Sidebar 收窄至 72px；
- Run Ledger + Evidence Stream 保持两栏；
- Inspector 成为可关闭的右侧浮层；
- 不压缩成不可读三栏。

### Mobile ≤ 720px，目标 390×844

结构：

1. Compact Top Bar
2. Run Summary
3. Evidence Stream
4. Selected Event
5. Inspector Bottom Sheet
6. Safe-area padding

移动端不承担完整 Workbench。Run Ledger 隐藏，由当前 Run Summary 保持上下文。Bottom Sheet 可关闭，默认不永久遮挡正文；JSON 在 Sheet 内滚动。页面不产生横向溢出。

## 11. Motion

只使用 100–180ms：

- selection 背景变化；
- Inspector / Bottom Sheet 抽出；
- tab 切换；
- loading 状态。

禁止常驻闪烁、宽高动画、粒子、霓虹光和影响中文阅读的运动。`prefers-reduced-motion` 会把 motion token 归零并停止 loading 旋转。

## 12. 全站共享与 Trace 专属

### 全站共享

- Layered Graphite Surface
- Text / Status token
- 4px spacing
- Radius
- Focus
- Reduced Motion
- App Shell 断点
- Disabled / Unavailable 语义

### Trace 专属

- Run Ledger
- Evidence Stream / Event Rail
- AgentStep provenance
- Run-level Linked Evidence
- Context / Decision Inspector
- Normalized Snapshot
- Decision Ledger

## 13. 后续扩展

- **Workbench**：共享 Token 和 App Shell；生成完成后提供进入 Run Evidence 的明确入口，不复制 Evidence Stream。
- **Human Review**：共享 Decision Ledger 与 Review 状态语义；队列仍保持自己的主结构。
- **Dashboard**：只共享 Surface / Status；不把 Dashboard 改成 Evidence Stream。
- **History**：共享 Run / Generation ID、状态和 provenance 标签；保持列表主对象。

Phase 1 完成后停止，不重构上述页面内部业务结构。
