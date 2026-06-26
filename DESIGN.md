# Design System: DevFlow Copilot

> Production-grade AI Coding Workbench for Java backend developers. This document is the single source of truth for Google Stitch screen generation and frontend implementation.

## 0. Product Thesis

DevFlow Copilot is a dark, dense command center where a developer can see an AI-assisted Java workflow move from requirement to Artifact, inspect every intermediate decision, and explicitly approve code-changing actions. It should feel closer to an IDE sidecar, build console, and review station than to a SaaS dashboard.

The visual identity is built around one recurring object: the **Trace Spine**. It is a thin vertical or horizontal execution rail that connects Requirement, Context, Plan, Generate, Verify, and Review. Nodes use precise state marks, timestamps, and compact log fragments. The Trace Spine appears on Overview, Workbench, Log Analyzer, and Generation History so the product has a recognizable visual grammar rather than a collection of unrelated screens.

Core product promise: **AI 执行过程可见，代码变更必须经过人确认。**

### Design calibration

- Density: **8/10 — Cockpit Dense**. This is a daily-use developer tool with compact information, 12–14px labels, narrow rows, and minimal dead space.
- Variance: **6/10 — Controlled Asymmetry**. Unequal grid spans and one dominant operational area per screen; never a symmetric tile dashboard.
- Motion: **4/10 — Instrumented and Restrained**. Movement signals execution, streaming, focus, or state change. Nothing moves for decoration.
- Emotional tone: precise, calm, technical, accountable, slightly industrial.
- Reference blend: Cursor's coding focus, Linear's density, Raycast's command fluency, and Vercel's disciplined hierarchy — without copying their layouts.

## 1. Visual Theme & Atmosphere

Imagine a Java build console designed by an excellent editorial systems team: layered charcoal surfaces, hairline seams, terse monospace telemetry, and a single muted mint signal color. The interface has the quiet pressure of a live deployment room, but remains readable for interns.

The app shell is persistent. A compact left navigation rail anchors the workspace; a 40px command bar spans the top; the remaining canvas behaves like an IDE work area with resizable panes, tabs, inspectors, and structured output. Panels meet edge-to-edge with 1px borders. Elevation is rare. Rounded cards floating in space are not the organizing principle.

The first screen must be visually memorable through **live workflow topology**, not illustration. The Overview's dominant region is an asymmetric execution map showing active AI runs flowing through the Trace Spine. A selected run expands into a slim diagnostic lane with branch decisions, Artifact types, duration, and human checkpoints. It should look operational and real at first glance.

### Surface behavior

- Use edge-to-edge work surfaces, inset only 16–20px from the shell.
- Separate regions with seams, tonal shifts, tabs, and split-pane handles.
- Use shallow surface stacking: canvas → panel → selected row. No floating glass layers.
- Radius is **0–8px only**. Default panel radius is 6px; nested controls are 4px; data rows and tabs may be square.
- Shadows appear only for menus, command palette, and drag previews.
- Use a subtle fixed 1px noise texture at 1.5–2% opacity only if it does not reduce text contrast. Never use blurred blobs or gradients.

## 2. Color Palette & Roles

The palette is a cool graphite family with one muted mint accent. All screens must use these exact roles. Do not drift into blue-gray, purple, or warm charcoal.

### Core neutrals

- **Workbench Void** `#101312` — app canvas and deepest background. Never use pure black.
- **Console Base** `#151918` — primary workspace surface and left navigation.
- **Raised Bay** `#1A1F1D` — inspector, selected module, command palette, expanded detail.
- **Active Row** `#202724` — hover, selected rows, active tab background.
- **Hard Seam** `#2B332F` — default 1px structural border and pane divider.
- **Soft Seam** `#222925` — subtle row separators and internal grid lines.
- **Primary Ink** `#E7ECE9` — headings, important values, primary code output.
- **Secondary Ink** `#AAB5AF` — descriptions, labels, inactive navigation.
- **Muted Ink** `#6F7B75` — timestamps, placeholders, disabled controls.
- **Ghost Ink** `#4C5752` — noninteractive ornaments, inactive Trace segments.

### Single accent

- **Runtime Mint** `#63B59F` — the only product accent. Use for active Workflow nodes, current step, primary action, focus ring, enabled toggle, selected code line, and healthy live connection.
- **Runtime Mint Dim** `rgba(99, 181, 159, 0.14)` — selected background and focus fill.
- **Runtime Mint Border** `rgba(99, 181, 159, 0.46)` — active outline and Trace connection.

Runtime Mint must occupy less than 8% of any screen. It is a signal, not decoration. Do not use an accent gradient and do not apply glow.

### Semantic status colors

These are reserved exceptions for status communication, never brand accents or CTA fills.

- **Failure Red** `#D97A72` — failed verification, blocking error, destructive action.
- **Warning Amber** `#C7A45A` — needs review, uncertain diagnosis, skipped check.
- **Info Steel** `#7F9CAA` — neutral informational state, queued task.

Always pair status color with an icon shape and text label. Never rely on color alone. Status fills use 10–14% opacity; solid semantic fills are allowed only for a 4px node or 2px bar.

### Code syntax

- Keyword: `#C7A45A`
- Type / class: `#83AFA3`
- String: `#B6C68E`
- Method: `#A8B8D0`
- Comment: `#66736D`
- Error underline: `#D97A72`

Syntax colors are text semantics inside code blocks, not UI accents.

## 3. Typography Rules

### Font families

- **Interface:** `Geist`, `Noto Sans SC`, `Microsoft YaHei UI`, sans-serif.
- **Code and telemetry:** `Geist Mono`, `JetBrains Mono`, `Noto Sans Mono CJK SC`, monospace.
- Serif fonts are forbidden throughout this software product.
- Inter is forbidden. Avoid generic system UI typography as the first choice.

### Scale

- Screen title: 20px / 28px, weight 620, letter spacing `-0.02em`.
- Pane title: 14px / 20px, weight 600.
- Body: 13px / 20px, weight 400.
- Dense row label: 12px / 18px, weight 500.
- Metadata: 11px / 16px, weight 450; uppercase English labels may use `0.06em` tracking.
- Code: 12.5px / 20px, weight 400.
- Operational number: 20–28px / 1.1, Geist Mono, tabular numerals.
- Command palette query: 15px / 22px, weight 450.

All numbers, durations, percentages, token counts, timestamps, Trace IDs, line numbers, and versions use Geist Mono with tabular numerals. Chinese body copy uses normal punctuation and natural sentence rhythm.

### Hierarchy

- Build hierarchy with weight, alignment, and ink color before increasing size.
- One 20px title per screen. No oversized display type.
- Keep prose blocks under 65 Chinese characters per line where practical.
- Use labels such as `TRACE`, `ARTIFACT`, and `WORKFLOW` sparingly as small technical markers, not as decorative eyebrow copy.
- Do not use gradient text, outlined text, or all-caps Chinese.

## 4. Spatial System & App Shell

### Desktop frame

- Designed for 1440–1920px wide viewports.
- Minimum supported productive width: 1280px. Below this, secondary inspectors collapse into drawers.
- Maximum content width is not fixed; the workbench uses available width like an IDE. Individual reading panes have max widths.
- Base spacing unit: 4px.
- Standard gaps: 4, 8, 12, 16, 20, 24, 32px.
- Main workspace padding: 16px at 1440px, 20px at 1600px+, 24px at 1920px.

### Persistent shell

1. **Navigation rail — 220px expanded / 56px collapsed**
   - Product mark: compact `DF` monogram built from two bracket-like strokes; no mascot.
   - Workspace selector: `支付平台 / dev` with branch icon and chevron.
   - Primary destinations: Overview, Workbench, Agent Run Trace, Knowledge Base, Log Analyzer, Generation History, Prompt Studio.
   - Utility destinations at bottom: Settings and keyboard shortcuts.
   - Active item uses a 2px Runtime Mint left rule, Primary Ink text, and Active Row background. No pill navigation.

2. **Command bar — 40px high**
   - Left: breadcrumb, e.g. `支付平台 / Workbench / FLOW-1842`.
   - Center: compact command trigger `搜索 Artifact、Trace 或命令…` with `⌘ K` keycap.
   - Right: environment `DEV`, agent runtime status, notifications, and avatar initials `WX`.
   - Use bottom border only; no shadow.

3. **Workspace canvas**
   - Full remaining height with `min-height: calc(100dvh - 40px)`.
   - Every screen has a 48px local header for title, context, and screen-level actions.
   - Main content uses CSS Grid and resizable split panes. No percentage `calc()` tricks.

### Grid logic

- Use a 12-column grid with 12px gutters for overview-level composition.
- Dominant regions span 7–8 columns; supporting regions span 4–5 columns.
- Avoid repeated equal-width modules. A screen should have one obvious place to look first.
- High-density lists use full-width rows with dividers, not isolated cards.
- Pane interiors use 12–16px padding. Code panes use 0px horizontal padding around line-number gutters.

## 5. Signature Pattern: Trace Spine

The Trace Spine is DevFlow Copilot's defining visual and interaction pattern.

### Anatomy

- 1px connecting rail in Soft Seam; completed section in Runtime Mint Border.
- Nodes are 8px squares with 2px radius, never circles by default.
- Current node: 8px Runtime Mint square with a subtle 1.6s opacity pulse from 1 to 0.58.
- Completed node: outlined mint square with a 3px center mark.
- Waiting node: Ghost Ink outline.
- Failed node: Failure Red square containing a 2px horizontal slash.
- Human checkpoint: 12px diamond outline with label `需确认`.
- Each node has step name, duration, Artifact count, and timestamp aligned in columns.

### Behavior

- Clicking a node opens its output in the adjacent inspector without navigating away.
- Hovering a connector reveals structured context passed between steps.
- Branches offset 16px and reconnect visibly; never draw decorative curves.
- Long traces virtualize rows. The sticky current step remains visible.
- Keyboard navigation: `J/K` moves between nodes, `Enter` opens output, `A` approves when permitted.

### Copy examples

- `解析需求` — `1.8s` — `Context 6`
- `检索代码上下文` — `4.2s` — `12 files`
- `生成变更计划` — `7.6s` — `Artifact PLAN-1842`
- `等待人工确认` — `已等待 03:14`
- `执行验证` — `mvn test · 42/42 passed`

## 6. Core Component Styling

### Buttons

- Height: 28px compact, 32px standard, 36px emphasized. Radius 5px.
- Primary: Runtime Mint fill with `#0F1714` text, weight 600. Only one primary action per view or modal.
- Secondary: transparent or Raised Bay fill, 1px Hard Seam border, Secondary Ink text.
- Ghost: no border; used inside toolbars.
- Destructive: transparent with Failure Red text and border; solid red requires a second confirmation.
- Hover changes background by one tonal step. Active translates down 1px and scales to 0.99 for 80ms.
- Focus uses a 2px Runtime Mint ring with 2px offset. Never use an outer glow.
- Icon-only buttons require visible tooltips and 28×28px minimum on desktop.

### Inputs and editors

- Labels sit above fields, 11px Muted Ink; required state is textual, not an asterisk alone.
- Input height 32px, radius 5px, Console Base fill, Hard Seam border.
- Focus changes border to Runtime Mint Border and adds an inset 1px line, no glow.
- Requirement editor is a code-editor-like surface with line gutter, Prompt variable chips, and slash commands.
- Placeholder example: `描述目标、约束与验收标准。输入 /context 引用代码范围…`
- Validation appears inline below the field. Preserve user input on errors.

### Panels

- Default radius 6px; border 1px Hard Seam; background Console Base.
- Panel headers are 34–40px high with title left, actions right, and a bottom seam.
- Panels may be flush and square where they meet in split layouts.
- Never place every metric in a card. Prefer one continuous diagnostic surface segmented with rules.
- Shadows are forbidden on ordinary panels.

### Data rows and tables

- Row height 34px standard, 40px with subtitle.
- Header height 30px, sticky, Raised Bay background.
- Horizontal cell padding 10px.
- Hover uses `#1C2320`; selection adds a 2px Runtime Mint left inset.
- Status is text plus a small geometric mark: `待复核`, `已采纳`, `已驳回`, `验证失败`.
- Sort icons appear only on hover or active sort.

### Tabs

- Square strip with 32px height and bottom border.
- Active tab uses Primary Ink and a 2px Runtime Mint bottom rule.
- Inactive tabs use Muted Ink. Do not use pill-shaped segmented controls for primary navigation.

### Code and diff

- Code background Workbench Void; line height 20px.
- 44px line-number gutter in `#121614`, right aligned.
- Added lines: Runtime Mint Dim with a 2px mint inset; removed lines: `rgba(217,122,114,.10)` with red inset.
- Current line uses `#1D2521`, not a bright overlay.
- Diff toolbar shows file path, language, additions/deletions, copy, and expand actions.
- Long code uses virtualized scrolling and a minimap only above 500 lines.

### Tags and badges

- Radius 4px, never fully pill-shaped except compact connection state.
- Height 20px; 10–11px type.
- Neutral tags use Raised Bay and Hard Seam. Runtime tags use Runtime Mint Dim.
- Examples: `Spring Boot 3.3`, `Java 21`, `RISK: MEDIUM`, `2 files`, `需确认`.

### Toggles

- 28×16px track, 7px thumb, 8px maximum radius.
- Enabled uses muted mint track; disabled uses Hard Seam.
- Toggle labels are always visible. Enabled state is also expressed as `已启用`.

### Menus, dialogs, and command palette

- Menus radius 6px; Raised Bay fill; 1px Hard Seam; shadow `0 12px 36px rgba(0,0,0,.34)`.
- Dialog width 480–720px depending on content; no oversized modal.
- Command palette width 680px, top aligned 12vh from viewport. Results use grouped dense rows and keyboard hints.
- Confirmation dialogs name the exact Workflow, files, and action. Avoid vague `确定吗？` copy.

### Feedback states

- Loading uses skeleton rows that match final dimensions. A streaming Artifact uses a left-edge progress line and caret; no generic spinner.
- Empty state is a compact working hint with an example command, never an illustration in a floating card.
- Error state includes cause, affected step, retry scope, and `复制诊断信息`.
- Toasts stack bottom-right, 360px wide, maximum three, and disappear only for noncritical success.

## 7. Screen Specification — Overview

### Intent

The first screen answers: What is the AI doing now? What needs me? What was produced? Is the team moving safely? It must read as a live command center in under five seconds.

### Local header

- Title: `Overview`
- Subtitle: `支付平台 · dev · 最近同步 12 秒前`
- Right actions: environment filter, date range `今天`, primary `新建 Workflow`

### Composition

Use a 12-column, two-row asymmetric grid inside the canvas.

#### A. Live Workflow Topology — columns 1–8, dominant, minimum 430px tall

- Header: `运行中的 Workflow` with counts `3 active · 2 waiting`.
- A horizontal Trace Spine begins with requirement IDs and progresses through six named stages.
- Each live run is a lane, not a card. Lanes share aligned stage columns, allowing comparison.
- Selected lane `FLOW-1842` expands vertically to show branch decisions and latest stream output.
- Right edge contains a narrow operational summary: elapsed time, tokens, model, context files.
- Realistic rows:
  - `FLOW-1842  订单退款幂等处理` — current `生成变更计划` — `00:42`
  - `FLOW-1839  统一异常响应结构` — current `等待人工确认` — `03:14`
  - `FLOW-1837  修复库存扣减竞态` — current `执行验证` — `mvn test 31/42`
- The topology area may use a faint 24px technical grid at 3% opacity. This is the only area allowed to use it.

#### B. Review Queue — columns 9–12, top aligned

- Header: `Review Queue` with filter `全部 6`.
- Dense stacked rows separated by seams, ordered by risk and wait time.
- Each row: Artifact type icon, concise title, risk, files changed, wait duration, reviewer.
- Sample:
  - `CODE PLAN` `退款幂等键落库方案` — `RISK: HIGH` — `4 files` — `等待 8m`
  - `PROMPT` `日志根因诊断模板 v12` — `RISK: LOW` — `等待 21m`
  - `PATCH` `GlobalExceptionHandler` — `RISK: MEDIUM` — `+48 −17`
- Selected item exposes two inline actions at bottom: `查看变更` and primary `开始复核`.

#### C. Artifact Stream — columns 1–7, second row

- Full-width chronological rows with Artifact ID, type, origin Workflow, summary, review state, author, time.
- First column carries a compact vertical Trace rail linking artifacts produced by the same Workflow.
- Sample: `ART-6F29 · PATCH · FLOW-1842 · 新增 refund_request 幂等约束 · 待复核 · 14:32:18`.

#### D. Developer Activity — columns 8–12, second row

- Not a vanity chart. Show an event ledger grouped by developer with review outcomes and concrete actions.
- Top has a restrained 7-day bar strip for `生成 / 采纳 / 驳回`; bars are thin and use neutrals plus Runtime Mint only for adopted changes.
- Event copy:
  - `王逊 采纳了 PLAN-1842，并要求补充并发测试`
  - `陈屿 驳回 PATCH-6E11：事务边界不完整`
  - `林乔 更新 Prompt「Spring 日志诊断」至 v12`

### Memorable details

- The expanded live lane streams one line of real execution text at a time: `Scanning OrderRefundService.java:118–204`.
- The top command bar shows a tiny live indicator labeled `Agent Runtime · 3 running`.
- The topology, queue, and Artifact stream share IDs and selection state. Clicking `FLOW-1842` highlights its related items across the screen.

## 8. Screen Specification — Workbench

### Intent

The Workbench turns a requirement into inspectable artifacts and stops at a clear human checkpoint before code changes are applied.

### Local header

- Breadcrumb: `Workbench / FLOW-1842`
- Title: `订单退款幂等处理`
- Status: `等待人工确认`
- Actions: `保存草稿`, `复制 Trace`, overflow menu.

### Three-pane layout

#### Left pane — Requirement & context, 28%, minimum 340px

- Tabs: `Requirement`, `Context`.
- Requirement editor with structured sections:
  - `目标` — `避免退款回调重试导致重复入账。`
  - `约束` — `不得修改对外 API；兼容 MySQL 8；沿用现有事务模型。`
  - `验收标准` — `相同 refundRequestId 并发请求只成功一次。`
- Context sources below: repository, branch, selected modules, Jira reference, related Trace.
- Each source shows freshness and scope: `payment-service · main@8e2f1c9 · 18 files`.
- Footer actions: `重新解析上下文`, primary `运行 Workflow`.

#### Center pane — Workflow execution, 34%, minimum 400px

- Vertical Trace Spine with steps: `解析需求`, `检索上下文`, `识别影响面`, `生成计划`, `生成 Artifact`, `验证`, `人工确认`.
- Active and completed states reveal duration and compact output.
- Clicking `识别影响面` shows affected modules and confidence, not hidden chain-of-thought. Use concise evidence summaries and citations to files.
- Human checkpoint is an expanded diamond node with `需要你确认 4 项代码变更`.

#### Right pane — Artifact & change plan, flexible remainder

- Top tabs: `Generated Artifact`, `Code-change Plan`, `Diff`, `Validation`.
- `Generated Artifact` displays a structured implementation brief with headings and file citations.
- `Code-change Plan` uses numbered rows with checkbox-style review states, file path, action, rationale, risk, and dependent tests.
- `Diff` uses split or unified code diff with file tree.
- `Validation` shows exact commands and results, e.g. `./mvnw -pl payment-service test -Dtest=RefundServiceTest`.

### Human confirmation bar

- Sticky 52px bar at bottom of center + right panes.
- Left: `4 项变更 · 3 个文件 · 风险 MEDIUM · 预计新增 2 个测试`.
- Middle: required confirmation check `我已检查事务边界与数据库约束`.
- Right: `退回修改` and primary `确认并生成 Patch`.
- Confirmation is disabled until blocking review items are opened. Explain why inline.

### Realistic plan copy

1. `V20260620__add_refund_idempotency.sql` — `新增 refund_request_id 唯一索引` — risk `HIGH`.
2. `RefundApplicationService.java` — `捕获唯一键冲突并返回已有结果` — risk `MEDIUM`.
3. `RefundRepository.java` — `增加按 refundRequestId 查询接口` — risk `LOW`.
4. `RefundConcurrencyTest.java` — `覆盖 20 路并发回调` — validation required.

## 9. Screen Specification — Log Analyzer

### Intent

Turn raw Java/Spring Boot logs into a source-linked diagnosis, actionable fix sequence, and reusable repair Prompt. The page must feel like a forensic workstation, not a chatbot.

### Local header

- Title: `Log Analyzer`
- Context: `payment-service / production / pod-7d9c8f`
- Actions: `导入日志`, `连接日志源`, `新建诊断`.

### Layout

Use a 7/5 split with a full-width evidence ribbon at the bottom.

#### Left — Log evidence

- Toolbar: source, time window, severity, thread, Trace ID, search.
- Main viewer is a virtualized monospace log stream with timestamps aligned, severity abbreviations, thread names, and collapsible stack frames.
- Error cluster selection highlights related lines using a 2px Failure Red inset, not a large fill.
- Anomaly markers appear in the scrollbar gutter.
- Sample incident:
  - `2026-06-20 14:07:31.842 ERROR [http-nio-8080-exec-7]`
  - `org.springframework.dao.CannotAcquireLockException`
  - `Deadlock found when trying to get lock; try restarting transaction`
  - `at com.devflow.payment.refund.RefundApplicationService.confirm(RefundApplicationService.java:146)`

#### Right — Diagnosis inspector

- Section 1: `Root Cause` with confidence `0.86`, short diagnosis, and evidence citations.
- Example: `RefundApplicationService.confirm()` 与 `LedgerWriter.append()` 以相反顺序锁定 `refund_order` 和 `ledger_entry`，形成循环等待。`
- Evidence chips link to exact log lines and source lines: `log:L284–L339`, `RefundApplicationService.java:146`.
- Section 2: `影响范围` — affected endpoint, observed rate, first occurrence, related deploy.
- Section 3: `Fix Steps` — ordered, checkable actions with risk and validation command.
- Section 4: `Repair Prompt` — editable prompt block with copied variables and target agent.

#### Bottom evidence ribbon

- Timeline from deploy to first error to recurrence to rollback.
- Events align by actual timestamps; no fake smooth chart.
- Example: `13:58 deploy 2.14.0` → `14:07 first deadlock` → `14:11 17 occurrences`.

### Repair Prompt example

`请修改 RefundApplicationService.confirm() 的锁顺序，使 refund_order 始终先于 ledger_entry 获取。保持现有事务边界，不修改 REST API。补充两个并发事务交叉执行的集成测试，并运行 payment-service 的定向测试。引用诊断 TRACE-LOG-928。`

Primary action: `送入 Workbench`. Secondary: `复制 Repair Prompt`.

## 10. Screen Specification — Generation History

### Intent

Provide an auditable history of generated artifacts, their lineage, review status, and eventual adoption. It is a temporal ledger, not a gallery.

### Local header

- Title: `Generation History`
- Summary: `过去 30 天生成 184 个 Artifact，采纳 117 个`.
- Actions: saved filter, export audit log.

### Composition

- Left filter rail, 220px: date, Artifact type, Workflow, status, developer, model, risk.
- Center timeline, flexible: dates as sticky group headers; a vertical Trace Spine links versions and review events.
- Right inspector, 380px: opens on selection with metadata, lineage, review comments, and diff summary.

### Timeline row

- Timestamp `14:32:18`.
- Artifact ID `ART-6F29`.
- Type `PATCH`.
- Title `新增退款请求幂等约束`.
- Origin `FLOW-1842`.
- Outcome `已采纳`.
- Change summary `+126 −18 · 4 files`.
- Reviewer `王逊`.

### Lineage behavior

- Expanding a row reveals `Prompt v11 → PLAN-1842 → PATCH-6F29 → REVIEW-882 → commit 8e2f1c9`.
- Rejected versions remain visible and visually muted; never erase history.
- Compare action allows any two versions of the same Artifact.
- Review comments are shown inline with file and line references.

### Empty and no-result copy

- Empty workspace: `还没有生成记录。运行第一个 Workflow 后，Artifact 与复核轨迹会出现在这里。`
- Filter has no result: `没有符合当前筛选条件的记录。` Action: `清除 3 个筛选条件`.

## 11. Screen Specification — Prompt Templates

### Intent

Manage production Prompt assets as versioned, testable configuration. Prompts are code-adjacent artifacts, not marketing content.

### Layout

Use a master-detail split: 42% template list, 58% editor. No grid of cards.

#### Left — Template registry

- Search and filters: scenario, enabled, owner, last updated.
- Group rows by scenario: `代码生成`, `日志诊断`, `Review`, `测试生成`.
- Row columns: template name, version, enabled status, variable count, owner, updated time.
- Realistic names:
  - `Spring Boot 变更计划生成` — `v18` — `已启用` — `8 variables`
  - `Java 异常根因诊断` — `v12` — `已启用` — `6 variables`
  - `Repository 层 Review` — `v7` — `已停用` — `5 variables`

#### Right — Edit panel

- Sticky header with template name, unsaved marker, version, and actions `试运行`, `保存新版本`.
- Tabs: `Prompt`, `Variables`, `Test Cases`, `Version Diff`.
- Prompt editor has line numbers, syntax treatment for `{{variable}}`, and inline validation.
- Variable side table contains name, type, required, default, source, sample value.
- Enabled toggle includes status text and deploy scope: `已启用 · dev / staging`.
- Bottom split preview shows rendered Prompt and token estimate.

### Variable examples

- `{{requirement}}` — multiline string — required — source `Workbench input`.
- `{{code_context}}` — file context — required — max `24 files`.
- `{{java_version}}` — enum — default `21`.
- `{{framework_version}}` — string — example `Spring Boot 3.3.1`.
- `{{constraints}}` — string array — optional.

### Versioning and safety

- Saving always creates a new version; never overwrite a published Prompt silently.
- Before enabling, require at least one passing test case and show affected Workflows.
- `Version Diff` displays changed lines and the last review comment.
- Destructive action copy: `停用后，3 个 Workflow 将回退到 v17。` Buttons: `取消`, `确认停用`.

## 12. Iconography & Visual Assets

- Use a single 1.5px-stroke icon set such as Lucide. Sizes 14, 16, or 18px.
- Prefer technical symbols: branch, terminal, file-code, diff, database, check-square, triangle-alert, clock, braces.
- Do not use filled colorful icons, 3D assets, emojis, avatars as decoration, or stock photography.
- Artifact type glyphs are geometric and monochrome:
  - PLAN: three offset horizontal rules.
  - PATCH: paired angle brackets with a center seam.
  - PROMPT: a cursor caret inside brackets.
  - TRACE: connected square nodes.
- The product monogram may use Runtime Mint, but must remain flat and under 20px.

## 13. Motion & Interaction

Motion communicates system state. It should feel like a finely tuned instrument, never a showcase.

### Timing

- Hover / press: 80–120ms.
- Pane, tooltip, menu: 140–180ms.
- Inspector swap: 180–220ms.
- Trace node progression: 240ms with staggered 40ms metadata reveal.
- Default spring equivalent: stiffness 100, damping 20.
- CSS fallback easing: `cubic-bezier(.2,.8,.2,1)`.

### Perpetual active states

- Current Trace node: opacity pulse every 1.6s.
- Streaming output: caret blink every 900ms.
- Runtime connected indicator: one 2px translate sweep inside its 12px track every 2.4s.
- Do not animate completed, idle, or historical content.

### Rules

- Animate only `transform` and `opacity` for continuous effects.
- Use stagger only when items are newly generated, maximum 30ms between rows and 240ms total.
- Preserve scroll and selection when inspectors update.
- Respect `prefers-reduced-motion`; replace pulses with static high-contrast marks.
- Never animate width, height, top, left, blur, or large background areas.
- No celebratory confetti, bouncing icons, floating panels, or parallax.

## 14. Responsive & Window Behavior

This is a desktop-first workbench, but it must degrade cleanly.

### 1600–1920px

- Navigation expanded at 220px.
- All specified panes visible.
- Overview uses 8/4 and 7/5 unequal splits.
- Workbench right inspector receives extra width.

### 1440–1599px

- Navigation may remain expanded.
- Reduce workspace padding to 16px.
- Hide nonessential descriptive metadata before shrinking core columns.
- Workbench left pane can reduce to 320px; center to 380px.

### 1280–1439px

- Navigation defaults collapsed to 56px.
- Right inspector opens as a resizable 420px overlay drawer with solid Raised Bay background.
- Overview review queue remains visible; developer activity moves below Artifact stream.

### Below 1024px

- Treat as read/review mode rather than full production workspace.
- Collapse multi-pane regions to one pane with tabs.
- No horizontal page overflow; code panes may scroll internally.
- All touch targets are at least 44px.
- Below 768px, navigation becomes a drawer and all content is single-column. Keep text at 14px minimum.

## 15. Accessibility & Operational Clarity

- Target WCAG 2.2 AA contrast for all text and interactive states.
- Keyboard access is first-class: visible focus, logical pane order, skip links, command shortcuts.
- Do not override standard cursor behavior.
- Icon-only controls require accessible labels and tooltips.
- Every status has text plus shape; charts have tabular alternatives.
- Error summary receives focus after failed submit and links to the exact field or step.
- Streaming content uses polite live regions and batches updates to avoid constant screen reader interruption.
- Resizable pane handles are keyboard adjustable and at least 8px hit area.
- Code diffs announce added/removed line counts and provide a plain-text mode.
- Never put critical information only in hover content.

## 16. Content Voice & Realistic Copy

Use concise, calm Chinese product copy. Preserve established technical nouns in English: Prompt, Artifact, Workflow, Trace, Patch, Diff, Token, Context, Review, Root Cause.

### Voice rules

- State what happened, where, and what the user can do next.
- Prefer concrete verbs: `生成`, `复核`, `引用`, `回退`, `重试`, `停用`, `送入 Workbench`.
- Avoid praise, hype, and conversational filler.
- Do not impersonate a chat assistant. The product reports and proposes; the developer decides.

### Good copy

- `Workflow 已暂停：需要确认数据库唯一索引变更。`
- `验证失败于 RefundConcurrencyTest.java:88。已保留生成结果。`
- `Root Cause 置信度 0.86；另有 2 个低概率原因待排除。`
- `保存为 v19 后，当前运行中的 Workflow 不会自动切换版本。`
- `未发现可引用的事务测试。建议先补充 Context。`

### Forbidden copy

- `释放无限潜能`
- `一站式赋能开发`
- `无缝提升效率`
- `AI 正在思考…`
- `太棒了！代码已生成`
- Generic names such as `John Doe`, `Acme`, or `Nexus`.
- Fake metrics such as `99.99%`, `50%`, or round-number performance claims without context.

## 17. Anti-Patterns — Never Do

- No landing page, hero section, marketing banner, or oversized welcome message.
- No generic admin dashboard with a row of KPI cards.
- No fake chat layout, message bubbles, assistant avatar, or prompt composer docked like a messenger.
- No 3-column equal card grid.
- No glassmorphism, backdrop blur, translucent floating panels, or frosted surfaces.
- No large gradients, gradient text, neon glow, purple/blue AI aesthetic, or oversaturated accent.
- No decorative blobs, orbit lines, sparkles, stock illustrations, or meaningless data visualization.
- No pure black `#000000`.
- No Inter, serif, or novelty display fonts.
- No emojis anywhere.
- No radius above 8px. No excessive pills.
- No heavy shadows on routine content.
- No custom mouse cursors.
- No overlapping text and visuals; every element owns a clear spatial zone.
- No equal-height card masonry that hides priority.
- No placeholder lorem ipsum, generic people, or fake company names.
- No AI marketing clichés such as Elevate, Seamless, Unleash, or Next-Gen.
- No filler instructions such as `滚动探索`, `向下查看`, or bouncing chevrons.
- No circular loading spinner for Workflow execution.
- No hidden reasoning theater. Show evidence, sources, actions, and confidence — not simulated chain-of-thought.
- No destructive action without named scope and confirmation.

## 18. Stitch Generation Directives

Apply this block to every screen request:

```text
Create a desktop-only production UI for DevFlow Copilot at 1600×1000. Use the exact graphite palette, Geist + Geist Mono typography, 0–8px radii, compact 12–14px density, hairline pane seams, and Runtime Mint as the sole product accent. Preserve the persistent 220px navigation rail and 40px command bar. The interface must feel like an IDE command center, not a generic SaaS admin dashboard and not a chat app. Use realistic Chinese copy while preserving Prompt, Artifact, Workflow, and Trace in English. Build hierarchy with asymmetric split panes and full-width data rows, not floating cards. No hero, gradients, glass, blobs, glows, large rounded cards, emojis, stock imagery, or three equal columns.
```

### Screen prompt — Overview

```text
Generate the DevFlow Copilot Overview. Make the dominant 8-column region a memorable live Workflow topology with three execution lanes sharing a Trace Spine across Requirement, Context, Plan, Generate, Verify, and Review. Expand FLOW-1842 and stream one current operation line. Place a dense Review Queue in the 4-column right region, then an Artifact Stream and Developer Activity ledger below in an unequal 7/5 split. Cross-highlight the selected Workflow across regions. Use the realistic Chinese IDs, states, file counts, durations, and activity copy from DESIGN.md. Do not turn each item into a card.
```

### Screen prompt — Workbench

```text
Generate the DevFlow Copilot Workbench for FLOW-1842, 订单退款幂等处理. Use a three-pane IDE layout: structured Requirement and Context on the left, vertical Workflow Trace Spine in the center, Generated Artifact / Code-change Plan / Diff / Validation tabs on the right. Add a sticky human confirmation bar listing exact change count, files, risk, and required acknowledgement. Show realistic Java and Spring Boot file paths. The workflow must visibly stop at 人工确认.
```

### Screen prompt — Log Analyzer

```text
Generate the DevFlow Copilot Log Analyzer as a forensic workstation. Use a 7/5 split: virtualized Spring Boot log viewer on the left and a structured Root Cause inspector with confidence, exact evidence citations, impact, Fix Steps, and editable Repair Prompt on the right. Add a bottom evidence timeline from deploy to first deadlock and recurrence. Do not use chat bubbles. Primary action is 送入 Workbench.
```

### Screen prompt — Generation History

```text
Generate the DevFlow Copilot Generation History as an audit ledger. Use a narrow left filter rail, a dense chronological center timeline with the Trace Spine connecting Artifact versions and review events, and a right metadata inspector. Show Prompt → PLAN → PATCH → REVIEW → commit lineage, including rejected versions. Use table-like rows with dividers instead of cards.
```

### Screen prompt — Prompt Templates

```text
Generate the DevFlow Copilot Prompt Templates screen as a versioned configuration editor. Use a 42/58 master-detail split: grouped template registry rows on the left and a Prompt editor on the right with Prompt, Variables, Test Cases, and Version Diff tabs. Show {{variable}} syntax, enabled scope, test status, rendered preview, token estimate, and 保存新版本. No card gallery.
```

## 11.1 Screen Specification — Agent Run Trace

### Intent

Show the true execution record of one Agentic Coding Workflow run. This screen must make the system explainable without pretending to be a full multi-agent runtime.

### Layout

- Left rail: dense Agent Run list with status, generation record ID, provider/model, created time, and elapsed time.
- Main pane: vertical Trace Spine of `TASK_DECOMPOSITION`, `PROMPT_RENDER`, `KNOWLEDGE_RETRIEVAL`, `LLM_GENERATION`, and `HUMAN_REVIEW`.
- Tool calls appear inline under the step that produced them, with tool name, input summary, output summary, status, and latency.
- Right inspector: Human Review records with `PENDING`, `SAVED`, `CONFIRMED`, or `REJECTED` state.

### Boundary Copy

Use copy such as `可解释的 Agent Workflow 记录，不模拟复杂多 Agent` when needed. Do not describe this as autonomous multi-agent orchestration.

## 11.2 Screen Specification — Knowledge Base

### Intent

Show the lightweight RAG loop: create a knowledge document, split it into chunks, search chunks, and return citations for generation.

### Layout

- Left rail: document list with source type, chunk count, updated time.
- Top main pane: document creation form and search form.
- Results pane: chunk citation label, score, and snippet.
- Bottom pane: selected document chunks with chunk index, summary, keywords, and embedding field status.

### Boundary Copy

Show `embedding 待接入` when vectors are not populated. Do not present keyword retrieval as a vector database.

## 19. Final Quality Gate

Before accepting any generated screen, verify all items:

- The first glance communicates active AI coding operations, not business analytics.
- The Trace Spine is present and functionally meaningful where specified.
- There is one clear dominant region and an asymmetric hierarchy.
- The palette uses graphite neutrals and Runtime Mint only; no purple or neon drift.
- Text looks like a real Chinese Java developer product, with concrete IDs and file paths.
- Workflow state, risk, evidence, and human confirmation are visible.
- Panels are structural, not a sea of floating cards.
- Radius never exceeds 8px.
- No hero, glass, large gradient, decorative blob, fake chat, or generic SaaS composition appears.
- Focus, error, loading, empty, and reduced-motion states are accounted for.
- The screen remains usable at 1440px without clipped critical controls.
