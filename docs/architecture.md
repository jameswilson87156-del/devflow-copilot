# DevFlow Copilot 架构

## 系统结构

```mermaid
flowchart LR
  FE["Vue 3 + TypeScript"] --> API["Spring Boot REST API"]
  API --> VALID["Validation + Exception Handler"]
  API --> SERVICE["Service 编排层"]
  SERVICE --> MAPPER["MyBatis-Plus Mapper"]
  MAPPER --> DB[("H2 dev / MySQL prod")]
  FLYWAY["Flyway Migration"] --> DB

  SERVICE --> RENDER["PromptTemplateRenderService"]
  RENDER --> ROUTER["GenerationProviderRouter"]
  ROUTER --> LOCAL["local-rule"]
  ROUTER --> OPENAI["openai-compatible"]
```

## 生成流程

```mermaid
sequenceDiagram
  participant UI as Workbench
  participant API as AiGenerationController
  participant T as Prompt Renderer
  participant P as Provider Router
  participant DB as GenerationRecord

  UI->>API: generationType + projectId + templateId + input
  API->>T: 选择模板并校验变量
  T-->>API: renderedPrompt + templateVersion
  API->>DB: 保存 GENERATING
  API->>P: 调用选定 Provider
  alt 真实 Provider 成功
    P-->>API: content + usage
  else 失败且允许降级
    P->>P: local-rule fallback
    P-->>API: content + fallbackReason
  end
  API->>DB: READY_FOR_REVIEW + Provider/模型/Token/延迟
  API-->>UI: 结构化 Artifact
```

## 状态机

```mermaid
stateDiagram-v2
  [*] --> GENERATING
  GENERATING --> READY_FOR_REVIEW: 生成成功
  GENERATING --> FAILED: 生成失败
  READY_FOR_REVIEW --> SAVED: 保存记录
  SAVED --> CONFIRMED: 人工确认
  CONFIRMED --> [*]
  FAILED --> [*]
```

任何未列出的状态流转都会由后端拒绝。

## 数据库策略

- `dev`：文件型 H2，开箱即用并支持服务重启后保留数据。
- `test`：内存 H2，每次测试隔离执行。
- `prod`：MySQL 8，通过环境变量配置连接信息。
- Flyway migration 是正式表结构来源；Service 不再使用 `InMemoryStore` 作为主存储。

## 边界

当前没有实现 SSE、自动代码修改、Git 提交、RAG 和登录权限。OpenAI-compatible 适配已实现，但默认运行模式是无需 Key 的 local-rule。
