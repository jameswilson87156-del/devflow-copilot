# design.md

## 1. 产品设计原则

1. **工具优先，不是展示品**：每个像素服务于操作目标。不存在装饰性区块，所有 UI 元素必须携带功能意图。
2. **密度可控的信息层级**：默认紧凑密度（compact），用户可切换宽松模式。行高、内边距全部基于 8px 格栅，不允许随意数字。
3. **状态即反馈**：AI 任务的运行/等待/失败状态必须在视觉上立即可辨别，不依赖 tooltip 传递关键信息。
4. **单一主线操作路径**：主工作区永远只有一条最高优先级的操作轨道（当前任务），其他内容降级至侧边或折叠面板。
5. **中性基调 + 单一强调色**：背景使用近黑/近白中性色，仅用一种品牌强调色标记可交互/活跃状态，拒绝多色渐变竞争注意力。

---

## 2. 视觉系统（CSS Variables）

```css
:root {
  /* ─── Brand ─── */
  --color-accent:        #5B6CF9;   /* 唯一强调色，hover 加深 10% */
  --color-accent-hover:  #4756E8;
  --color-accent-muted:  rgba(91, 108, 249, 0.12); /* 选中背景 */

  /* ─── Neutral (Dark mode 优先) ─── */
  --color-bg:            #0E0F12;
  --color-surface:       #16181D;   /* 卡片/面板 */
  --color-surface-raised:#1E2028;   /* 悬浮菜单、popover */
  --color-border:        #2A2C35;
  --color-border-subtle: #1F2129;

  /* ─── Text ─── */
  --color-text-primary:  #E8E9EF;
  --color-text-secondary:#8B8FA8;
  --color-text-disabled: #484B5E;

  /* ─── Status ─── */
  --color-success:       #3DD68C;
  --color-warning:       #F5A623;
  --color-error:         #F26969;
  --color-info:          #5B6CF9;
  --color-running:       #5B6CF9;   /* 与 accent 同色，配脉冲动画 */

  /* ─── Typography ─── */
  --font-mono:  "JetBrains Mono", "Fira Code", monospace;
  --font-sans:  "Inter", "PingFang SC", system-ui, sans-serif;

  --text-xs:    11px;
  --text-sm:    12px;
  --text-base:  13px;  /* 工作台默认字号 */
  --text-md:    14px;
  --text-lg:    16px;
  --text-xl:    20px;

  --leading-tight:  1.3;
  --leading-normal: 1.5;
  --leading-code:   1.6;

  /* ─── Spacing (8px 格栅) ─── */
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 24px;
  --space-6: 32px;
  --space-7: 48px;

  /* ─── Radius ─── */
  --radius-sm: 4px;
  --radius-md: 6px;
  --radius-lg: 8px;   /* 最大卡片圆角 */
  --radius-pill: 100px;

  /* ─── Shadows (仅轻微深度，无 glow) ─── */
  --shadow-sm:  0 1px 2px rgba(0,0,0,0.3);
  --shadow-md:  0 2px 8px rgba(0,0,0,0.4);
  --shadow-pop: 0 4px 16px rgba(0,0,0,0.5);

  /* ─── Border ─── */
  --border-default: 1px solid var(--color-border);
  --border-subtle:  1px solid var(--color-border-subtle);
}
```

**Light mode** 覆写示例（通过 `.theme-light` 切换）：
```css
.theme-light {
  --color-bg:           #F5F6FA;
  --color-surface:      #FFFFFF;
  --color-surface-raised:#F0F1F7;
  --color-border:       #E0E2EC;
  --color-text-primary: #1A1C26;
  --color-text-secondary:#6B6F85;
}
```

---

## 3. 布局规则

### 3.1 首屏骨架

```
┌─────────────────────────────────────────────────────────┐
│ [16px] 顶栏 NavBar  h=40px  [logo 24px | 面包屑 | 右侧工具条] │
├──────┬──────────────────────────┬───────────────────────┤
│      │                          │                       │
│ 侧边  │     主工作区 Main          │  AI 助手面板 AiPanel   │
│ Nav  │     (flex-grow: 1)       │  w=320px 可折叠        │
│      │                          │                       │
│ w=   │                          │                       │
│ 200px│                          │                       │
│      │                          │                       │
└──────┴──────────────────────────┴───────────────────────┘
```

- 总布局：`display: grid; grid-template-columns: 200px 1fr 320px`
- NavBar 固定顶部，`position: sticky; top: 0; z-index: 100`，高度 40px，背景 `var(--color-surface)`，底部 `var(--border-subtle)`
- AiPanel 折叠后宽度退为 40px（仅保留图标栏），通过 `transition: width 200ms ease` 动画

### 3.2 侧边导航

- 宽度 200px，背景 `var(--color-bg)`
- 导航项：高度 32px，内边距 `0 var(--space-3)`，字号 `var(--text-sm)`，颜色 `var(--color-text-secondary)`
- 激活项：背景 `var(--color-accent-muted)`，文字 `var(--color-text-primary)`，左边框 `2px solid var(--color-accent)`
- 分组标题：字号 `var(--text-xs)`，大写，颜色 `var(--color-text-disabled)`，margin-top `var(--space-3)`
- 禁止：图标 >16px、多级展开超过 2 层、折叠按钮使用汉堡图标

### 3.3 主工作区

- 内边距：`var(--space-4) var(--space-5)`
- 顶部区：页面标题 `var(--text-lg)` + 副标题 `var(--text-sm) var(--color-text-secondary)` + 操作按钮组，整体高度 ≤56px
- 内容区：垂直滚动，`overflow-y: auto`，不允许横向滚动
- Section 间距：`var(--space-5)`，用细线 `var(--border-subtle)` 或纯空白分隔，不用分组标题卡片

### 3.4 AI 助手面板（AiPanel）

- 固定右侧，`position: sticky; top: 40px; height: calc(100vh - 40px)`
- 内部三段：顶部上下文信息条（32px）+ 中间对话流（flex-grow）+ 底部输入框区（auto）
- 输入框：`border: var(--border-default); border-radius: var(--radius-md); min-height: 72px`，支持 `Cmd+Enter` 提交
- 消息气泡：AI 消息靠左，无气泡背景色，仅用左侧 `3px solid var(--color-border)` 竖线区分；用户消息靠右，背景 `var(--color-surface-raised)`
- 代码块：使用 `var(--font-mono)`，背景 `var(--color-bg)`，内边距 `var(--space-3)`

### 3.5 历史/时间线区域

- 作为抽屉（Drawer）从底部滑出，高度最大 50vh，不占用主布局列
- 列表项高度 36px，显示：状态点 + 任务名 + 时间戳 + 耗时
- 状态点颜色映射：running=`var(--color-running)` 配 `@keyframes pulse`，success=`var(--color-success)`，error=`var(--color-error)`

---

## 4. 核心页面改造方向

### 4.1 Dashboard

**现状问题**：信息卡片堆叠，无法一眼知道当前在做什么。

**改造目标**：
- 移除所有统计数字大卡片（PV/UV 风格）
- 第一屏只有两个区域：「当前进行中任务」列表（最多 5 条）+ 「最近活动时间线」（最近 10 条）
- 用两列布局：左列 60% 显示任务列表，右列 40% 显示时间线
- 任务行高 48px：状态图标(16px) + 任务名称 + 模型标签(badge) + 时间 + 操作按钮（仅 icon）
- Badge 规格：高度 18px，内边距 `0 var(--space-2)`，字号 `var(--text-xs)`，圆角 `var(--radius-sm)`

### 4.2 Workbench（核心页）

**布局**：三列（侧边导航 + 编辑/配置区 + AI 面板）

**编辑/配置区拆分**：
- 上半部分（60%）：Prompt 编辑器，使用 CodeMirror/Monaco，背景 `var(--color-bg)`，行号显示
- 下半部分（40%）：参数配置，使用紧凑表单，label 在左 120px 固定宽，input 填充剩余宽度
- 中间分割线可拖拽调整比例
- 运行按钮：固定在顶部操作栏，主色背景，`height: 32px; padding: 0 var(--space-4)`，快捷键 `⌘↩` 显示在按钮右侧

**禁止在 Workbench 出现**：欢迎横幅、空状态插画、「开始你的 AI 之旅」类文案

### 4.3 Log Analyzer

**布局**：全宽，无侧边 AI 面板（专注日志）

**日志列表规格**：
- 行高 24px（紧凑模式），字体 `var(--font-mono) var(--text-sm)`
- 列定义：时间戳(120px) | 级别(56px badge) | 来源(100px) | 消息(flex-grow) | 耗时(64px)
- 级别颜色：ERROR=`var(--color-error)` 背景 10% 透明度行高亮；WARN=`var(--color-warning)`；INFO=默认；DEBUG=`var(--color-text-disabled)`
- 顶部工具栏：搜索框(240px) + 级别过滤器(chipgroup) + 时间范围选择器 + 实时开关
- 搜索框支持正则，输入框右侧显示匹配数量

### 4.4 Generation History

**布局**：主列表(左 380px) + 右侧详情面板(flex-grow)，SplitPane 模式

**列表项规格**（高度 56px）：
- 第一行：任务名称 `var(--text-sm) var(--color-text-primary)` + 状态 badge
- 第二行：模型名 + Token 数 + 时间，字号 `var(--text-xs) var(--color-text-secondary)`
- 点击展开右侧详情，不跳转新页面

**右侧详情**：
- 顶部 meta 信息条（模型/参数/时间/总 Token）
- 下方 diff 视图：输入 Prompt vs 输出结果，用两列对比或 unified diff 风格

### 4.5 Prompt Templates

**布局**：两列，左侧模板列表(搜索 + 标签过滤)，右侧编辑器

**模板列表项**（高度 48px）：
- 模板名 + 语言/场景标签(最多 2 个 badge) + 最后修改时间
- 悬浮时显示「使用」「编辑」两个 icon 按钮，不展开卡片

**编辑器区**：
- 模板内嵌变量使用 `{{变量名}}` 语法高亮（区别色 `var(--color-warning)`）
- 右上角：「插入变量」下拉 + 「测试运行」按钮

---

## 5. 禁止模式

| 类别 | 禁止内容 |
|------|----------|
| 视觉 | 渐变大背景（含径向渐变 hero 区）、毛玻璃效果、装饰性 blob/圆形光晕 |
| 视觉 | 多色强调（超过 1 个品牌色 + 4 个状态色）|
| 视觉 | 卡片投影超过 `var(--shadow-md)` |
| 布局 | 卡片圆角超过 8px |
| 布局 | 页面主内容区存在横向滚动 |
| 布局 | 空状态使用大插画（>120px）+ 营销文案 |
| 组件 | 可交互元素高度低于 28px（无障碍最小点击区） |
| 组件 | 下拉菜单超过 8 个选项不分组 |
| 组件 | Toast/通知堆叠超过 3 条 |
| 内容 | 任何「欢迎使用」「开始探索」「赋能开发者」类文案 |
| 内容 | 展示假数据不加 `data-mock` 标识 |

---

## 6. 截图验收标准

浏览器截图后逐条核查：

1. **色彩一致性**：页面中强调色只出现一种蓝紫色（`#5B6CF9`），状态色仅 4 种（绿/黄/红/蓝），无其他彩色出现。
2. **间距格栅**：用开发者工具测量任意两个相邻元素的间距，必须是 4 的倍数（4/8/12/16/24/32px）。
3. **卡片圆角**：所有卡片/面板圆角 ≤8px，按钮圆角 ≤6px，badge 圆角 ≤4px（pill 类除外）。
4. **字号层级**：页面中出现的字号不超过 5 个档位，均在变量定义范围内。
5. **无装饰元素**：截图中不存在纯装饰性图形、渐变 banner、空洞背景纹理。
6. **状态可辨**：在灰度截图模式下，running/success/error 状态仍可通过形状或位置区分（不仅靠颜色）。
7. **AI 面板可折叠**：折叠后主工作区正常扩展填充，无布局错位。
8. **信息密度**：1920×1080 截图中，Dashboard 首屏至少展示 5 条任务信息，不存在大面积空白（单块空白区域 <200px 高）。
9. **响应式断点**：1280px 宽度时侧边导航自动折叠为 40px icon 模式，主内容区正常显示。
10. **交互元素可见**：所有按钮/链接在 hover 状态下有明确的视觉变化（背景色变化 ≥10% 亮度差或加边框）。
