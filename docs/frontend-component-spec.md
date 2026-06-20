先阅读设计规范，再生成组件文档。

根据 design.md，现在生成 `component-spec.md`：

```markdown
# component-spec.md

## 组件规格表

| 组件 | 用途 | 主要内容 | 视觉重点 | 交互状态 | 实现注意事项 |
| --- | --- | --- | --- | --- | --- |
| **Sidebar** | 全局页面导航 | 顶部 Logo 区(24px 图标)；导航分组标题(text-xs 大写 color-text-disabled)；导航项列表；底部用户头像+版本号 | 宽 200px，背景 `color-bg`；激活项：`color-accent-muted` 背景 + 左边框 `2px solid color-accent` + `color-text-primary`；普通项：`color-text-secondary` | default / hover(`color-surface` 背景) / active / collapsed(40px icon-only) | 折叠通过 `transition: width 200ms ease`；图标上限 16px；嵌套不超过 2 层；1280px 断点自动折叠为 40px |
| **Topbar** | 全局顶栏：定位 + 工具入口 | Logo(24px) + 面包屑(text-sm) + 右侧工具条(通知/主题切换/用户头像，间距 space-2) | 高度固定 40px；`position: sticky; top:0; z-index:100`；背景 `color-surface`；底部 `border-subtle` | sticky 滚动不遮挡内容；右侧图标 hover 背景 `color-surface-raised` | 面包屑最后一段 `color-text-primary`，前段 `color-text-secondary`；整体高度不可超过 40px |
| **Metric** | 内联展示单一数值（详情面板/Meta 条，非大卡片） | 数值(`text-lg font-mono`) + 标签(`text-xs color-text-secondary`) + 可选趋势箭头(16px icon) | 无独立卡片容器；嵌入 Meta 信息条横排；标签与数值垂直排列，行间距 `leading-tight` | 无交互；数值变更时 `opacity 0→1 150ms` 过渡 | 禁止单独大卡片形式；Token 数/耗时/时间戳场景使用；数值超 6 位用 K/M 缩写 |
| **Workflow Stepper** | 展示 AI 任务执行阶段进度 | 步骤节点(16px 圆点) + 步骤标签(`text-xs`) + 连接线 + 当前步骤高亮 | 横向排布；已完成节点 `color-success`；进行中节点 `color-running` + `@keyframes pulse`；待执行节点 `color-border` | running / success / error / pending；error 节点 `color-error` + 悬停显示 tooltip 错误摘要 | 节点间连接线 `1px solid color-border`；完成段变 `color-success`；最多 8 步，超出折叠为 `+N` |
| **Artifact Panel** | 展示 AI 生成产出物（代码/文本） | 顶部 Meta 条(32px：模型名+Token+耗时) + 内容区(CodeMirror/Monaco 或富文本) + 底部操作栏(复制/下载/对比) | 背景 `color-bg`；代码区 `font-mono text-sm leading-code`；顶部 Meta 条背景 `color-surface`，`border-bottom: border-subtle` | 内容加载中：骨架占位(shimmer，`color-surface-raised`)；hover 操作栏浮现 | 操作栏固定底部，`position: sticky; bottom:0`；不显示空态插画；diff 视图支持 unified/split 切换 |
| **Log Diagnosis Panel** | 结构化日志流展示与过滤 | 顶部工具栏(搜索 240px + 级别 chipgroup + 时间范围 + 实时开关) + 日志行列表 | 行高 24px(紧凑)；`font-mono text-sm`；列：时间戳 120px \| 级别 badge 56px \| 来源 100px \| 消息 flex-grow \| 耗时 64px；ERROR 行背景 `color-error` 10% 透明度 | 实时滚动自动追底；手动滚动时停止追底并显示「↓ 跳到最新」badge；搜索命中行黄色文字高亮 | 级别 badge 颜色：ERROR=`color-error`，WARN=`color-warning`，INFO=默认，DEBUG=`color-text-disabled`；搜索框支持正则，右侧显示匹配数 |
| **History Timeline** | 展示历史任务执行记录 | 状态点(8px) + 任务名(`text-sm`) + 时间戳 + 耗时；列表项高度 36px | 状态点色同 Status 变量；running 状态配 pulse 动画；背景 `color-surface` | 点击列表项跳转对应详情；hover 背景 `color-surface-raised` | 作为底部 Drawer 实现，最大高度 50vh；不占主布局列；按时间倒序排列 |
| **Prompt Template Card** | 模板库列表项展示与快捷操作 | 模板名(`text-sm color-text-primary`) + 最多 2 个场景 badge + 最后修改时间(`text-xs color-text-secondary`) | 列表项高度 48px；badge 高 18px，`border-radius: radius-sm`；整体背景 `color-surface` | hover 时右侧浮现「使用」「编辑」两个 icon 按钮(16px icon，28px 点击区)；active/选中 `color-accent-muted` 背景 | 不展开卡片；icon 按钮仅 hover 显示；badge 超出 2 个截断 |
| **Action Button** | 触发主要/次要操作 | Primary：品牌色填充 + 文字；Secondary：透明背景 + `border-default` + 文字；Icon-only：仅图标 | 高度 32px；`padding: 0 space-4`；`border-radius: radius-md`；Primary 背景 `color-accent`，hover `color-accent-hover`；快捷键标注在按钮右侧(`text-xs color-text-disabled`) | default / hover(加深 10%) / active(scale 0.98) / loading(spinner 替换文字) / disabled(`opacity:0.4 cursor:not-allowed`) | 最小点击区高度 28px；loading 时禁止二次点击；⌘↩ 快捷键绑定 Primary 提交按钮 |
| **Empty State** | 无数据时的占位提示 | 单行文字说明(`text-sm color-text-secondary`) + 可选 CTA 按钮；禁止大插画 | 插图上限 120px；垂直居中；无营销文案；文字简洁如「暂无任务」「尚无日志」 | CTA 按钮遵循 Action Button 规范 | 禁止「开始探索」「赋能开发者」类文案；禁止大背景插画；日志/历史等纯展示页可省略 CTA |
| **Drawer / Modal** | 底部历史抽屉 / 轻量确认弹窗 | Drawer：底部滑出，最大 50vh，内含 History Timeline；Modal：标题(`text-md`) + 内容 + 操作按钮组(右对齐) | Drawer 背景 `color-surface`，顶部 `border-subtle`；Modal 背景 `color-surface-raised`，`shadow-pop`，`radius-lg`；Overlay `rgba(0,0,0,0.5)` | Drawer open/close `transform: translateY 200ms ease`；Modal open `opacity+scale 150ms`；Esc 关闭；点击 Overlay 关闭 | Drawer 不占主布局列；Modal 宽度上限 480px；禁止在 Modal 内嵌套 Drawer；`z-index` Drawer=200，Modal=300 |

---

## 页面组件组合

### Dashboard
- 布局：两列（左 60% + 右 40%），无侧边 AiPanel
- 左列：**History Timeline**（当前进行中任务，最多 5 条，行高 48px，嵌入 **Workflow Stepper** 单步状态点）
- 右列：**History Timeline**（最近活动，最近 10 条，36px 行高）
- 顶部：**Topbar** + **Sidebar**；无大 **Metric** 卡片，数值仅以内联 Metric 嵌入列表行

### Workbench
- 布局：三列（**Sidebar** 200px + 编辑区 flex-grow + **AiPanel** 320px）
- 编辑区上半（60%）：Prompt 编辑器（**Artifact Panel** 编辑模式，CodeMirror）
- 编辑区下半（40%）：参数紧凑表单，label 120px 固定宽
- 顶部操作栏：**Action Button**（Primary 运行，⌘↩ 标注）+ **Workflow Stepper**（任务阶段）
- 分割线可拖拽；禁止欢迎横幅与空态插画

### Log Analyzer
- 布局：全宽，无 **AiPanel**，无侧边折叠
- 主体：**Log Diagnosis Panel**（全宽日志列表 + 顶部工具栏）
- 底部可呼出 **Drawer** 展示关联任务的 **History Timeline**
- 无 **Metric** 大卡片；匹配数通过搜索框右侧内联数字展示

### Generation History
- 布局：SplitPane（左 380px 列表 + 右 flex-grow 详情）
- 左：**Prompt Template Card** 样式的历史列表项（高度 56px，两行文字）+ **History Timeline** 状态点
- 右：顶部 **Metric** Meta 条（模型/参数/Token/时间）+ **Artifact Panel**（diff 视图）
- 点击左列项加载右侧详情，不跳转页面

### Prompt Templates
- 布局：两列（左模板列表 + 右编辑器）
- 左：搜索框 + 标签 chipgroup + **Prompt Template Card** 列表
- 右：编辑器区（`{{变量名}}` 语法高亮 `color-warning`）+ 右上角「插入变量」下拉 + 「测试运行」**Action Button**
- 新建/删除确认通过 **Modal**；变量管理通过 **Drawer** 或内联下拉

---

## 验收重点

1. **Sidebar 折叠**：1280px 宽度时自动折叠为 40px icon-only，主内容区无错位，`transition` 动画流畅。
2. **状态点可辨性**：灰度截图下，running(pulse 动画形态) / success(静止) / error(停止+不同形状) 三态可通过形状或动态区分，不依赖颜色。
3. **Workbench 无装饰**：截图中不出现欢迎横幅、空态插画、营销文案，首屏直接展示编辑器。
4. **Log Analyzer 密度**：1920×1080 下日志列表行高 24px，首屏可见 ≥30 行，无大面积空白（单块 <200px）。
5. **Metric 形态**：全局截图中不存在独立大数字统计卡片，数值仅内联于 Meta 条或列表行。
6. **Action Button 最小尺寸**：任意可交互按钮高度 ≥28px，开发者工具测量确认。
7. **Empty State 合规**：空态区无超过 120px 的图形，无营销文案，仅单行说明文字 + 可选 CTA。
8. **Badge 规格**：Dashboard 任务行与列表项中 badge 高度 18px，圆角 ≤4px（pill 类除外），字号 text-xs。
9. **Drawer z-index 层叠**：底部 Drawer 打开时不遮挡 Modal（z-index Drawer=200 < Modal=300），Overlay 正确隔离交互。
10. **强调色唯一**：全局截图中品牌色仅 `#5B6CF9` 一种，状态色限绿/黄/红/蓝四种，无其他彩色元素。
```
