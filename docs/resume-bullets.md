# DevFlow Copilot 简历项目描述

## 项目名称

DevFlow Copilot — AI Coding 工作流控制台

## 可直接使用的项目描述

- 独立开发面向 Java 开发者的 AI Coding 工作流控制台，基于 Spring Boot 3、Vue 3 与 TypeScript 实现项目上下文、Prompt 模板、Java 日志诊断、生成历史及人工确认闭环。
- 抽象统一 Generation Provider，支持 OpenAI-compatible `/v1/chat/completions` 调用和无 API Key 的 local-rule 降级模式，记录 Provider、模型、Token、延迟及失败原因。
- 使用 MyBatis-Plus、Flyway 与 H2 / MySQL profiles 完成核心数据持久化，将生成状态约束为 `GENERATING → READY_FOR_REVIEW → SAVED → CONFIRMED`，拒绝非法状态流转。
- 实现 Prompt 模板变量校验、默认模板选择、渲染和版本追踪，使生成历史可回溯实际 Prompt 与模板版本。
- 编写 15 个 JUnit 5 / MockMvc 集成测试，并配置 Docker Compose 与 GitHub Actions 完成后端测试和前端构建验证。

## 30 秒面试介绍

我做了一个 AI Coding 工作流控制台，它不是聊天套壳，而是把项目上下文、Prompt 模板、模型生成、历史追踪和人工确认组织成固定流程。后端使用 Spring Boot、MyBatis-Plus 和 Flyway，支持 H2 与 MySQL；AI 层支持 OpenAI-compatible Provider 和本地规则降级；每次生成都会记录模型、Token、延迟和模板版本，并通过状态机限制保存和确认操作。项目目前有 15 个后端测试，以及 Docker Compose 和 GitHub Actions。

## 使用边界

只有在真实配置 API Key 并完成模型调用演示后，才建议在简历中写“调用真实大模型”。当前代码实现了 OpenAI-compatible 适配，但默认演示使用 local-rule。
