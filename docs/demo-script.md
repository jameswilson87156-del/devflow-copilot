# DevFlow Copilot Demo Script

## 演示目标

用 5 到 8 分钟展示 DevFlow Copilot 如何把 AI Coding 从聊天式问答变成可追踪的开发工作流。

## 1. 打开 Dashboard

打开前端地址：`http://127.0.0.1:5174`

讲解点：

- 这是 AI Coding 工作流控制台，不是普通后台首页。
- 顶部展示项目数、今日生成、日志分析、Prompt 模板。
- 中间展示 DevFlow Copilot 工作流：Context → Compose → Artifact → Review。
- 下方 Developer Activity 展示最近生成记录。

## 2. 进入 Workbench

点击 `Open Workbench` 或左侧 `Workbench`。

讲解点：

- 左侧是 Project Context，类似 IDE / Agent 上下文面板。
- 中间是 Task Composer，用来输入需求并选择生成类型。
- 右侧是 AI Result Panel，用 Markdown 展示结构化 Artifact。

## 3. 选择 ai-jd-analyzer 演示项目

在左侧项目选择器中选择：`ai-jd-analyzer`。

讲解点：

- 项目上下文会带出技术栈、当前需求和开发约束。
- 这模拟真实 AI Coding 前先给模型上下文。

## 4. 输入需求

输入：

```text
给 AI 求职 Agent 增加投递复盘模块，支持记录面试问题、复盘建议和下一步行动。要求先输出需求拆解，不要直接进入实现。
```

## 5. 运行需求拆解

选择 `Requirement Split`，点击 `Run Workflow`。

讲解点：

- 状态从 Draft 进入 Generating，再进入 Ready for Review。
- 输出不是聊天回复，而是结构化 Markdown Artifact。

## 6. 查看 AI 输出

在右侧查看：

- 需求摘要
- 后端任务
- 前端任务
- 数据库任务
- 测试任务
- 风险点
- 验收标准

讲解点：

- 这类结果可以直接给 Codex / Claude Code 作为任务输入。
- 输出仍然需要人工确认。

## 7. 复制 Codex / Claude 修复 Prompt

切换生成类型为 `Generate Fix Prompt`，输入同样需求或具体问题，点击 `Run Workflow`。

点击右侧 `Copy Result`。

讲解点：

- 这里生成的是可以直接复制到 Codex / Claude Code 的结构化 Prompt。
- Prompt 包含角色、项目背景、修改范围、任务要求和验收标准。

## 8. 保存记录

点击 `Save Record`。

讲解点：

- 保存后记录会进入 Generation History。
- 保存不代表代码已经修改，只代表 Artifact 被沉淀。

## 9. 标记确认

点击 `Mark Confirmed`。

讲解点：

- Human-in-the-loop 是这个项目的核心设计。
- AI 输出必须由开发者确认后再使用。

## 10. 进入 Log Analyzer

点击左侧 `Log Analyzer`。

讲解点：

- 这个页面面向 Java / Spring Boot 报错分析。
- 支持异常识别、原因分析、排查步骤和修复 Prompt。

## 11. 粘贴 Spring Boot 报错

选择 `power-plant-system` 项目。

点击 `Port already in use 示例` 或粘贴：

```text
***************************
APPLICATION FAILED TO START
***************************

Web server failed to start. Port 8080 was already in use.
```

点击 `Analyze Log`。

## 12. 查看异常原因和排查步骤

展示：

- Exception Type：Port already in use
- Risk Level
- 可能原因
- 排查步骤
- Codex / Claude Fix Prompt

讲解点：

- 日志分析结果不是单纯解释，而是可执行排查路径。
- 修复 Prompt 可以复制给 AI Coding 工具继续处理。

## 13. 进入 Generation History

点击左侧 `Generation History`。

讲解点：

- 历史记录像开发者 Activity 流。
- 可以按项目和类型筛选。
- 每条记录展示类型、项目、状态、耗时、确认状态和时间。

## 14. 展示历史记录

筛选项目：`ai-jd-analyzer`。

点击历史记录，右侧展示 Markdown 输出。

讲解点：

- 所有输出可复制、可确认、可复盘。
- 这解决了 AI Coding 输出不可追踪的问题。

## 15. 进入 Agent Run Trace

点击左侧 `Agent Run Trace`。

讲解点：

- 这里展示一次生成任务的真实运行记录。
- 步骤包括任务接收、Prompt 渲染、Knowledge 检索、Provider 生成和人工确认。
- Tool Call 只记录工具名、输入摘要、输出摘要、状态和耗时，不展示或伪造模型思维链。

## 16. 进入 Knowledge Base

点击左侧 `Knowledge Base`。

讲解点：

- 可以创建知识文档，后端会自动切成 chunks。
- 当前检索是关键词/简单相似度，不是向量数据库。
- Workbench 生成时可以带 `knowledgeQuery`，响应会返回命中的 chunk 引用。

## 收尾总结

可以这样收尾：

```text
DevFlow Copilot 的重点不是替代 IDE，也不是做聊天机器人，而是把 AI Coding 前后的需求拆解、Knowledge 引用、Provider 调用、Agent Trace、日志分析、Prompt 生成和历史确认流程产品化。MVP 默认用本地规则跑通工作流，真实 LLM 只通过环境变量启用。
```
