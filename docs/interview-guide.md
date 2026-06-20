# DevFlow Copilot Interview Guide

## 30 秒项目介绍

DevFlow Copilot 是一个面向 Java 开发者的 AI Coding 工作流控制台。它不是聊天机器人，而是把项目上下文、需求拆解、日志分析、代码修改计划、修复 Prompt 和生成历史组织成可追踪的 Human-in-the-loop 流程。当前 MVP 使用本地规则生成结构化 Markdown 输出，后续可以接入真实 LLM API。

## 1 分钟项目介绍

这个项目解决的是 AI Coding 使用过程中的混乱问题：需求拆不清、报错不会分析、Prompt 不稳定、AI 输出难追踪。DevFlow Copilot 通过 Dashboard、Workbench、Log Analyzer、Prompt Templates 和 Generation History 五个页面，把一次 AI Coding 行为变成固定流程：选择项目上下文，选择生成类型，输入需求或日志，生成结构化 Artifact，保存历史并人工确认。后端使用 Spring Boot 3，前端使用 Vue3 + TypeScript + Element Plus，MVP 先用本地规则模拟 AI 输出，预留 LLM API 扩展接口。

## 3 分钟架构讲解

前端是 Vue3 + Vite + TypeScript，页面包括 Dashboard、Workbench、Log Analyzer、Prompt Templates 和 Generation History。Workbench 是核心页面，采用左中右三栏：左侧是项目上下文，中间是任务输入和工作流状态，右侧是 Markdown Artifact 输出区。

后端是 Java 17 + Spring Boot 3，包结构按 controller、service、service.impl、dto、entity、mapper 分层。Controller 提供项目上下文、AI 生成、日志分析、模板、历史记录和 Dashboard 接口。Service 层里抽象了 `AiGenerateService`，当前实现是 `LocalRuleGenerateService`，用于本地规则生成；同时保留 `LlmGenerateService`，后续可以替换成真实模型调用。

数据模型包括 `project_context`、`prompt_template`、`generation_record`、`log_analysis` 和 `ai_task`。其中 `generation_record` 是追踪 AI 输出的核心表，保存输入摘要、输出内容、生成类型、状态、耗时和人工确认状态。

## 项目难点

- 产品边界：既要体现 AI Coding，又不能做成普通聊天机器人。
- 前端体验：Workbench 要像开发者工具，而不是 Element Plus 默认后台。
- 结构化输出：同一套生成服务要支持需求拆解、修改计划、README、Commit Message、修复 Prompt。
- 日志诊断：需要识别常见 Java / Spring Boot 异常，并输出可执行排查路径。
- Human-in-the-loop：所有输出都要可复制、可保存、可确认，而不是直接执行。

## 为什么不是聊天机器人

聊天机器人通常以对话为中心，输出容易散落在多轮消息中。DevFlow Copilot 以工作流和 Artifact 为中心，每次生成都有项目上下文、任务类型、输入摘要、结构化输出、状态和历史记录，更适合 AI Coding 场景复盘。

## 为什么不自动改代码

MVP 阶段的目标是辅助开发决策，而不是替代开发者执行操作。自动改代码会引入安全、可控性和误操作风险。项目强调 review-only artifact，让开发者把输出复制给 Codex、Claude Code 或自己手动执行，并在确认后保存记录。

## 为什么 MVP 先用本地规则

本地规则可以让项目在没有 API Key、没有模型额度的情况下稳定演示，也方便面试时展示完整流程。它验证的是产品流程、数据模型、前后端架构和扩展点。真实 LLM API 可以在 V2 接入，不影响 MVP 的结构。

## 后续如何接真实 LLM API

可以保留 `AiGenerateService` 接口，把 `LocalRuleGenerateService` 换成或扩展为 LLM Provider：

1. 新增 `OpenAiGenerateService` 或 `SpringAiGenerateService`。
2. 从 `prompt_template` 读取模板并渲染变量。
3. 将项目上下文、用户输入和模板拼成 Prompt。
4. 调用 LLM API 获取 Markdown 输出。
5. 保存到 `generation_record`，状态仍然是 `Ready for Review`。
6. 保持人工确认流程不变。

## Java 后端体现在哪里

- Spring Boot 3 REST API 分层设计
- DTO、Entity、Service、Controller 分离
- MyBatis-Plus Mapper 和 MySQL 表设计
- 统一响应结构和异常处理
- 本地规则生成服务和 LLM 扩展接口抽象
- 日志诊断规则引擎
- 生成历史和状态流管理

## AI Coding 体现在哪里

- 面向 Codex、Claude Code、Cursor 等工具的修复 Prompt 生成
- 需求拆解和代码修改计划生成
- Java / Spring Boot 报错日志诊断
- Prompt 模板管理
- AI 输出 Artifact 化和可追踪历史
- Human-in-the-loop 状态流

## 面试官追问 Q&A

### Q1：这个项目和 ChatGPT 套壳有什么区别？

A：它不是以聊天为中心，而是以 AI Coding 工作流为中心。每次生成都有项目上下文、任务类型、结构化 Artifact、状态和历史记录，可以复盘和确认。

### Q2：为什么不直接让 AI 改代码？

A：MVP 面向实习项目和个人开发者，重点是提高 AI Coding 的可控性。自动执行会引入误改和安全风险，所以当前只生成可复制的 Prompt 和修改计划，由开发者确认后使用。

### Q3：本地规则是不是不够 AI？

A：MVP 先用本地规则验证产品闭环，避免依赖 API Key 和模型额度。架构上已经抽象了 `AiGenerateService` 和 `LlmGenerateService`，后续可以无缝替换成真实 LLM 调用。

### Q4：数据库中最核心的表是什么？

A：`generation_record`，因为它记录了每次 AI 输出的项目、类型、输入、输出、状态、耗时和确认状态，是可追踪性的核心。

### Q5：如果接真实 LLM，怎么保证输出质量？

A：通过 Prompt 模板、项目上下文、输出结构约束和人工确认。后续还可以加入模板版本、输出评分和重试优化。

### Q6：你在前端上做了什么设计？

A：我没有用普通后台模板，而是做了深色开发者工具风格。Workbench 采用三栏布局，左侧像 IDE 上下文，中间像任务编排器，右侧像 Artifact 输出区。

### Q7：项目两周能完成吗？

A：可以。MVP 范围克制，不做登录、权限、GitHub、IDE 插件和自动执行，重点实现 AI Coding 工作流、日志分析、模板、历史和前端产品感。
