# DevFlow Copilot 面试项目说明

## 项目定位

DevFlow Copilot 是面向 Java 开发者的 AI Coding 工作流控制台。它解决需求拆解不稳定、Prompt 难复用、日志诊断缺少结构、生成结果无法追踪以及 AI 输出缺少人工确认边界的问题。

产品不是聊天机器人，而是以项目上下文、Prompt 模板、Artifact、生成历史和状态机为核心。

## 技术栈

- Java 17、Spring Boot 3.3、MyBatis-Plus、Flyway、H2、MySQL
- Vue 3、TypeScript、Vite、Element Plus
- JUnit 5、Spring Boot Test、MockMvc
- Docker Compose、Nginx、GitHub Actions

## 核心功能

1. 管理项目技术栈、目录结构、当前需求和开发约束。
2. 使用 Prompt 模板生成需求拆解、修改计划、README、Commit Message 和修复 Prompt。
3. 识别常见 Java / Spring Boot 日志并输出排查路径。
4. 保存每次生成的输入、输出、Provider、模型、Token、延迟、模板版本和失败原因。
5. 要求生成记录经过保存和人工确认后进入终态。

## 架构与生成链路

前端通过 Axios 调用 Spring Boot REST API。Controller 负责参数校验和响应封装，Service 负责业务编排，Mapper 负责数据库 CRUD。Flyway 是唯一正式表结构来源。

```text
Workbench 请求
  → 读取项目上下文
  → 选择指定模板或默认启用模板
  → 校验并渲染 {{variables}}
  → 创建 GENERATING 记录
  → Provider Router 选择 local-rule / openai-compatible
  → 保存模型、Token、延迟与输出
  → READY_FOR_REVIEW → SAVED → CONFIRMED
```

## 数据持久化

开发环境使用文件型 H2，生产环境使用 MySQL。两种环境共用 Flyway migration 和 MyBatis-Plus Mapper。`InMemoryStore` 只在 `memory-demo` profile 下可用，不再承担正式存储。

生成历史、Prompt 模板、项目上下文和日志诊断均真实落库。已经通过创建数据、重启后端和重新查询验证 H2 持久化。

## LLM Provider

- `local-rule`：不需要 API Key，生成稳定、结构化的演示内容。
- `openai-compatible`：调用兼容 `/v1/chat/completions` 的服务，配置 base URL、API Key、model、timeout 和 max tokens。

真实 Provider 失败时可以配置是否降级到 local-rule。降级原因会保存在生成历史，避免静默吞掉错误。

## Prompt 模板

模板支持 `{{projectName}}`、`{{techStack}}`、`{{requirement}}`、`{{context}}`、`{{codingRules}}` 和 `{{errorLog}}` 等变量。

请求可以携带 `templateId`；未携带时按生成类型选择默认启用模板。保存模板会递增版本，生成记录保存模板名称、版本和最终渲染 Prompt。

## 状态机

```text
GENERATING → READY_FOR_REVIEW
GENERATING → FAILED
READY_FOR_REVIEW → SAVED
SAVED → CONFIRMED
```

`CONFIRMED` 和 `FAILED` 是终态。后端拒绝越级确认或终态回退，前端也只在允许状态显示对应按钮。

## 测试和 CI

项目包含 15 个后端测试，覆盖模板渲染、必填变量校验、Provider 降级、生成历史、合法与非法状态流转、Controller 参数校验、Mapper CRUD 和日志规则诊断。

GitHub Actions 分别执行 `mvn verify` 和 `npm ci && npm run build`。Docker Compose 提供前端、后端和 MySQL 三个服务。

## 常见面试追问

### 为什么不直接让 AI 修改代码？

项目强调 Human-in-the-loop。模型输出具有不确定性，当前版本将其保存为 review-only Artifact，由开发者保存并确认，避免自动执行带来的误改和安全风险。

### local-rule 算不算 AI？

local-rule 是无 Key 演示和故障降级 Provider，不冒充真实模型。系统同时实现 OpenAI-compatible 调用适配，真实调用是否启用由环境配置决定。

### 为什么同时使用 H2 和 MySQL？

H2 降低本地演示门槛，MySQL 对应生产部署。两者共用 Flyway migration 和 Mapper 业务代码，减少环境差异。

### 如何保证 Prompt 修改可追溯？

模板保存时递增版本，生成记录冗余保存模板 ID、名称、版本和最终渲染 Prompt，即使模板后续变化，历史生成仍可复盘。

### 为什么不用微服务、Redis 或 Kafka？

当前规模下单体分层足够清晰。增加中间件不会解决核心问题，只会提高部署和解释成本，因此保持最小必要架构。

### 还有什么没有完成？

当前没有 SSE 流式输出、RAG、登录权限和自动代码执行。后续若增加长耗时模型调用，可将生成任务异步化并通过 SSE 推送状态。
