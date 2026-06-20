```markdown
# codex-implementation-prompt.md

## 任务背景

项目：DevFlow Copilot
前端路径：`D:\workhome\ai-coding-workbench\frontend`

你是一名资深前端工程师，负责将设计规范落地为可运行代码。

---

## 第一步：读取上下文（必须先执行）

按顺序读取以下文件，理解现有结构后再动手：

1. `README.md`
2. `docs/frontend-design.md`
3. `docs/frontend-component-spec.md`
4. `docs/frontend-codex-prompt.md`
5. `frontend/` 目录结构（列出所有文件，重点读取路由、状态管理、现有组件）

**如果以上文件不存在，读取 `frontend/src/` 下的实际文件作为替代。**

---

## 第二步：硬性约束（违反则任务失败）

- **只修改 `frontend/` 目录**，禁止修改后端代码、数据库 schema、API 路由
- **不重建项目**，在现有框架（React/Vue/Next.js，以实际为准）基础上改造
- **保留所有现有 API 调用、数据流、路由路径**，只改 UI 层
- **不引入新的组件库**（如 Ant Design、MUI、Chakra），图标统一使用 `lucide-react`
- **所有文案使用真实业务语义**（中文），禁止 `Lorem ipsum`、`示例任务`、`placeholder`、`TODO`

---

## 第三步：视觉系统注入

在 `frontend/src/styles/variables.css`（或等效全局样式文件）中写入以下 CSS 变量（替换现有变量，不允许保留旧值冲突）：

```css
:root {
  --color-accent:        #5B6CF9;
  --color-accent-hover:  #4756E8;
  --color-accent-muted:  rgba(91, 108, 249, 0.12);
  --color-bg:            #0E0F12;
  --color-surface:       #16181D;
  --color-surface-raised:#1E2028;
  --color-border:        #2A2C35;
  --color-border-subtle: #1F2129;
  --color-text-primary:  #E8E9EF;
  --color-text-secondary:#8B8FA8;
  --color-text-disabled: #484B5E;
  --color-success:       #3DD68C;
  --color-warning:       #F5A623;
  --color-error:         #F26969;
  --color-running:       #5B6CF9;
  --font-mono: "JetBrains Mono", "Fira Code", monospace;
  --font-sans: "Inter", "PingFang SC", system-ui, sans-serif;
  --text-xs: 11px; --text-sm: 12px; --text-base: 13px;
  --text-md: 14px; --text-lg: 16px; --text-xl: 20px;
  --leading-tight: 1.3; --leading-normal: 1.5; --leading-code: 1.6;
  --space-1: 4px;  --space-2: 8px;  --space-3: 12px;
  --space-4: 16px; --space-5: 24px; --space-6: 32px; --space-7: 48px;
  --radius-sm: 4px; --radius-md: 6px; --radius-lg: 8px; --radius-pill: 100px;
  --shadow-sm:  0 1px 2px rgba(0,0,0,0.3);
  --shadow-md:  0 2px 8px rgba(0,0,0,0.4);
  --shadow-pop: 0 4px 16px rgba(0,0,0,0.5);
  --border-default: 1px solid var(--color-border);
  --border-subtle:  1px solid var(--color-border-subtle);
}
.theme-light {
  --color-bg:            #F5F6FA;
  --color-surface:       #FFFFFF;
  --color-surface-raised:#F0F1F7;
  --color-border:        #E0E2EC;
  --color-text-primary:  #1A1C26;
  --color-text-secondary:#6B6F85;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50%       { opacity: 0.4; }
}
```

---

## 第四步：全局布局改造

### 4.1 根布局

```
grid-template-columns: 200px 1fr 320px
grid-template-rows: 40px 1fr
```

- `Topbar`：`position: sticky; top: 0; z-index: 100; height: 40px; background: var(--color-surface); border-bottom: var(--border-subtle)`
- `Sidebar`：`width: 200px; background: var(--color-bg)`；1280px 断点自动折叠为 `width: 40px`，仅显示图标，`transition: width 200ms ease`
- `AiPanel`：`width: 320px; position: sticky; top: 40px; height: calc(100vh - 40px)`；折叠后 `width: 40px`，`transition: width 200ms ease`
- Log Analyzer 页面：隐藏 AiPanel，主工作区全宽

### 4.2 Sidebar 导航项规格

```css
.nav-item {
  height: 32px;
  padding: 0 var(--space-3);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}
.nav-item:hover  { background: var(--color-surface); }
.nav-item.active {
  background: var(--color-accent-muted);
  color: var(--color-text-primary);
  border-left: 2px solid var(--color-accent);
}
.nav-group-title {
  font-size: var(--text-xs);
  text-transform: uppercase;
  color: var(--color-text-disabled);
  margin-top: var(--space-3);
}
```

图标使用 `lucide-react`，尺寸严格 16px，嵌套层级 ≤2 层。

---

## 第五步：逐页改造

### 5.1 Dashboard

**删除**：所有统计大卡片（PV/UV 样式的数字展示块）、欢迎横幅、空白 hero 区。

**重建为两列布局**（左 60% + 右 40%）：

**左列 —— 进行中任务列表**
- 标题：`进行中的任务`，font-size `var(--text-sm)`，color `var(--color-text-secondary)`，不加卡片外框
- 列表最多 5 条，每行高度 **48px**
- 每行内容：`[状态图标 16px] [任务名称 flex-grow] [模型 badge] [创建时间] [操作图标按钮]`
- 状态图标：running=`Loader2`（pulse 动画）/ success=`CheckCircle2`（color-success）/ error=`XCircle`（color-error）
- Badge 规格：`height: 18px; padding: 0 var(--space-2); font-size: var(--text-xs); border-radius: var(--radius-sm); background: var(--color-surface-raised); color: var(--color-text-secondary)`
- 操作按钮仅 icon（`MoreHorizontal`），28px 点击区

**右列 —— 最近活动时间线**
- 标题：`最近活动`
- 列表最多 10 条，每行高度 **36px**
- 每行：`[8px 状态圆点] [任务名 flex-grow] [时间戳] [耗时]`
- 状态圆点：`border-radius: 50%; width: 8px; height: 8px`，running 加 pulse 动画

### 5.2 Workbench

**删除**：欢迎横幅、空状态插画、所有「开始你的 AI 之旅」类文案，进入页面直接显示编辑器。

**布局**：三列（Sidebar 200px + 编辑区 flex-grow + AiPanel 320px）

**编辑区**（垂直 SplitPane，上 60% / 下 40%，分割线可拖拽）：
- 上半：Prompt 编辑器（复用现有 CodeMirror/Monaco 实例，背景 `var(--color-bg)`，显示行号，font `var(--font-mono)`）
- 下半：参数紧凑表单，`label { width: 120px; font-size: var(--text-sm); color: var(--color-text-secondary) }`，input 填充剩余宽度

**顶部操作栏**（高度 40px，`border-bottom: var(--border-subtle)`）：
- 主按钮「运行」：`background: var(--color-accent); hover: var(--color-accent-hover); height: 32px; padding: 0 var(--space-4); border-radius: var(--radius-md)`，右侧标注 `⌘↩`（font-size `var(--text-xs)` color `var(--color-text-disabled)`）
- Workflow Stepper：横向节点，节点圆点 16px，连接线 `1px solid var(--color-border)`，running 节点 pulse 动画

**AiPanel 内部结构**：
- 顶部上下文条：32px，`border-bottom: var(--border-subtle)`
- 中间对话流：`flex-grow: 1; overflow-y: auto`
- AI 消息：靠左，无气泡，左侧 `border-left: 3px solid var(--color-border)`，padding-left `var(--space-3)`
- 用户消息：靠右，`background: var(--color-surface-raised); border-radius: var(--radius-md); padding: var(--space-2) var(--space-3)`
- 底部输入框：`border: var(--border-default); border-radius: var(--radius-md); min-height: 72px`，支持 `Cmd+Enter` 提交

### 5.3 Log Analyzer

**布局**：全宽（隐藏 AiPanel），无横向滚动。

**顶部工具栏**（高度 40px，`gap: var(--space-2)`）：
- 搜索框 240px，支持正则，右侧内联显示匹配数（`color-text-secondary`）
- 级别过滤 chipgroup：ALL / ERROR / WARN / INFO / DEBUG，选中态 `color-accent-muted`
- 时间范围选择器（下拉）
- 实时开关（Toggle，开启时 `color-accent`）

**日志列表**：
```css
.log-row {
  height: 24px;
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  display: grid;
  grid-template-columns: 120px 56px 100px 1fr 64px;
  align-items: center;
  gap: var(--space-2);
}
.log-row.error { background: rgba(242, 105, 105, 0.10); }
```
级别 badge：ERROR=`color-error`，WARN=`color-warning`，INFO=默认，DEBUG=`color-text-disabled`

实时追底逻辑：自动滚动到底部；用户手动向上滚动时停止自动追底，显示「↓ 跳到最新」悬浮 badge（`color-accent` 背景，`position: sticky; bottom: var(--space-3)`）。

### 5.4 Generation History

**布局**：SplitPane（左 380px + 右 flex-grow），点击左列项加载右侧详情，**不跳转页面**。

**左列列表项**（高度 56px）：
- 第一行：任务名 `var(--text-sm) var(--color-text-primary)` + 状态 badge
- 第二行：模型名 + Token 数 + 时间，`var(--text-xs) var(--color-text-secondary)`
- 选中态：`background: var(--color-accent-muted); border-left: 2px solid var(--color-accent)`

**右侧详情**：
- 顶部 Meta 条（32px）：模型 / 参数 / 总 Token / 耗时，内联排列，`font-size: var(--text-xs)`
- 下方 Artifact Panel：diff 视图，支持 unified/split 切换，背景 `var(--color-bg)`，代码区 `var(--font-mono)`

### 5.5 Prompt Templates

**布局**：两列（左模板列表 + 右编辑器）

**左列**：搜索框 + 标签 chipgroup + 模板列表
- 列表项高度 48px：模板名 + 最多 2 个场景 badge + 最后修改时间
- hover 时右侧浮现「使用」（`Play` 图标）和「编辑」（`Pencil` 图标）两个 icon 按钮，仅 hover 可见，点击区 28px

**右侧编辑器**：
- `{{变量名}}` 语法高亮，颜色 `var(--color-warning)`
- 右上角：「插入变量」下拉按钮 + 「测试运行」主色按钮

---

## 第六步：通用组件规范

### Action Button
```css
.btn-primary {
  height: 32px; padding: 0 var(--space-4);
  background: var(--color-accent); color: #fff;
  border-radius: var(--radius-md); border: none;
  font-size: var(--text-sm);
}
.btn-primary:hover  { background: var(--color-accent-hover); }
.btn-primary:active { transform: scale(0.98); }
.btn-primary:disabled { opacity: 0.4; cursor: not-allowed; }
/* loading 态：文字替换为 Loader2 旋转 icon */
.btn-secondary {
  height: 32px; padding: 0 var(--space-4);
  background: transparent; border: var(--border-default);
  border-radius: var(--radius-md); color: var(--color-text-primary);
  font-size: var(--text-sm);
}
```
最小点击区高度 28px（所有可交互元素）。

### Empty State
```tsx
// 合规写法
<div style={{ textAlign: 'center', padding: 'var(--space-5)' }}>
  <p style={{ fontSize: 'var(--text-sm)', color: 'var(--color-text-secondary)' }}>
    暂无生成记录
  </p>
  <button className="btn-secondary" style={{ marginTop: 'var(--space-3)' }}>
    新建任务
  </button>
</div>
// 禁止：大插画、「开始探索」「赋能开发者」等文案
```

### Drawer（底部历史抽屉）
- `position: fixed; bottom: 0; left: 0; right: 0; max-height: 50vh`
- `background: var(--color-surface); border-top: var(--border-subtle)`
- 动画：`transform: translateY(100%) → translateY(0)，duration: 200ms ease`
- `z-index: 200`

### Modal
- `background: var(--color-surface-raised); border-radius: var(--radius-lg); box-shadow: var(--shadow-pop); max-width: 480px`
- `z-index: 300`
- Overlay：`background: rgba(0,0,0,0.5)`
- 动画：`opacity 0→1 + scale 0.95→1，duration: 150ms`
- Esc 关闭，点击 Overlay 关闭

---

## 第七步：禁止模式核查（改造前后均需检查）

在提交任何文件前，逐条检查以下禁止项，发现则立即修复：

| 类别 | 禁止项 |
|------|--------|
| 视觉 | 渐变大背景（`linear-gradient` / `radial-gradient` 用于 hero/背景区）|
| 视觉 | 毛玻璃（`backdrop-filter: blur`）|
| 视觉 | 装饰性 blob、光晕、背景纹理 |
| 视觉 | 第二种品牌强调色（只允许 `#5B6CF9`）|
| 布局 | 卡片圆角 > 8px（`border-radius > var(--radius-lg)`）|
| 布局 | 主内容区横向滚动 |
| 布局 | 间距非 4 的倍数 |
| 组件 | 可交互元素高度 < 28px |
| 组件 | 下拉选项 > 8 个不分组 |
| 组件 | Toast 堆叠 > 3 条 |
| 内容 | 「欢迎使用」「开始探索」「赋能开发者」「开始你的 AI 之旅」类文案 |
| 内容 | 假数据未标注 `data-mock="true"` 属性 |

---

## 第八步：启动、截图、自审

### 8.1 启动前端

```bash
cd D:\workhome\ai-coding-workbench\frontend
npm install
npm run dev
# 记录本地访问地址，通常为 http://localhost:5173 或 http://localhost:3000
```

### 8.2 截图（使用 Chrome 或 Edge）

依次访问以下页面，在 **1920×1080** 分辨率下截图，并在 **1280px** 宽度下补截一张响应式截图：

| 截图文件名 | 页面 | 备注 |
|---|---|---|
| `screenshot-dashboard-1920.png` | Dashboard | 验证首屏 ≥5 条任务，无大空白 |
| `screenshot-dashboard-1280.png` | Dashboard | 验证 Sidebar 折叠为 40px |
| `screenshot-workbench-1920.png` | Workbench | 验证编辑器直接展示，无欢迎横幅 |
| `screenshot-log-analyzer-1920.png` | Log Analyzer | 验证行高 24px，首屏 ≥30 行 |
| `screenshot-history-1920.png` | Generation History | 验证 SplitPane，无大卡片 |
| `screenshot-templates-1920.png` | Prompt Templates | 验证变量高亮、hover 按钮 |
| `screenshot-ai-panel-collapsed.png` | Workbench | AiPanel 折叠后主区填充正常 |

截图保存至 `docs/screenshots/` 目录。

### 8.3 自审清单（截图后逐条对照）

1. **色彩唯一性**：截图中强调色只有 `#5B6CF9` 一种，状态色仅绿/黄/红/蓝四种
2. **间距格栅**：用开发者工具抽查 3 处相邻元素间距，均为 4 的倍数
3. **圆角合规**：所有卡片/面板 ≤8px，按钮 ≤6px，badge ≤4px（pill 除外）
4. **字号档位**：页面字号不超过 5 个档位，均在变量定义内
5. **无装饰元素**：截图中无渐变 banner、装饰图形、背景纹理
6. **状态灰度可辨**：截图转灰度后，running/success/error 三态通过形状/动态仍可区分
7. **AiPanel 折叠**：折叠后主区扩展，无错位
8. **Dashboard 密度**：首屏 ≥5 条任务，单块空白 <200px 高
9. **响应式**：1280px 宽度下 Sidebar 折叠为 40px icon-only，主内容区正常
10. **交互反馈**：hover 状态背景亮度差 ≥10% 或加边框

---

## 第九步：构建

```bash
cd D:\workhome\ai-coding-workbench\frontend
npm run build
# 构建成功后记录 dist/ 目录大小与耗时
```

构建失败时：读取完整报错信息 → 定位根因 → 修复 → 重新构建，循环直到成功。

---

## 第十步：最终输出

完成所有步骤后，在终端输出以下结构化摘要：

```
=== DevFlow Copilot 前端改造完成报告 ===

【已修改文件】
- frontend/src/styles/variables.css   （CSS 变量注入）
- frontend/src/components/Layout.tsx  （根布局）
- frontend/src/components/Sidebar.tsx （侧边导航）
- frontend/src/components/Topbar.tsx  （顶栏）
- frontend/src/components/AiPanel.tsx （AI 助手面板）
- frontend/src/pages/Dashboard.tsx    （首页改造）
- frontend/src/pages/Workbench.tsx    （工作台改造）
- frontend/src/pages/LogAnalyzer.tsx  （日志分析改造）
- frontend/src/pages/GenerationHistory.tsx（生成历史改造）
- frontend/src/pages/PromptTemplates.tsx  （提示词模板改造）
... （实际修改文件列表）

【截图路径】
- docs/screenshots/screenshot-dashboard-1920.png
- docs/screenshots/screenshot-dashboard-1280.png
- docs/screenshots/screenshot-workbench-1920.png
- docs/screenshots/screenshot-log-analyzer-1920.png
- docs/screenshots/screenshot-history-1920.png
- docs/screenshots/screenshot-templates-1920.png
- docs/screenshots/screenshot-ai-panel-collapsed.png

【构建结果】
- 状态：成功 / 失败（失败时附错误摘要）
- dist/ 大小：xxx KB
- 构建耗时：xx s

【前端访问地址】
- 开发环境：http://localhost:5173

【自审结果】
- 色彩唯一性：✅ / ❌（问题描述）
- 间距格栅：✅ / ❌
- 圆角合规：✅ / ❌
- 无装饰元素：✅ / ❌
- 状态灰度可辨：✅ / ❌
- AiPanel 折叠：✅ / ❌
- Dashboard 密度：✅ / ❌
- 响应式断点：✅ / ❌
- 交互反馈：✅ / ❌

【V2 待办项】
- 列出本次未实现、建议下一版本处理的功能或优化点
```

---

## 执行规则总结

- 每步执行前先读文件，不猜测现有结构
- 每步执行后验证结果，不跳步
- 发现与现有代码冲突时，优先保留现有 API 调用与数据流，只替换 UI 层
- 自审清单每项明确 ✅ 或 ❌，不允许「基本符合」等模糊描述
- 构建未通过时不提交报告，循环修复直到构建绿灯
```
