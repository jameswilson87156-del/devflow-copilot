# DevFlow Copilot

DevFlow Copilot 是一个面向 Java 开发者的 AI Coding 工作流控制台。它以项目上下文、Prompt 模板、结构化生成、Java 日志诊断、生成历史和人工确认状态为核心，而不是模拟聊天界面。

项目默认使用 `local-rule` Provider，不依赖 API Key 即可完整演示；配置 `openai-compatible` 后可调用兼容 `/v1/chat/completions` 的模型服务。所有输出都是 review-only Artifact，不会自动修改代码或提交 Git。

## 技术栈

- 后端：Java 17、Spring Boot 3.3、MyBatis-Plus、Flyway、H2 / MySQL、JUnit 5、MockMvc
- 前端：Vue 3、TypeScript、Vite、Element Plus、Axios、Markdown-it
- 工程化：Docker Compose、Nginx、GitHub Actions

## 核心能力

- 项目上下文 CRUD 与数据库持久化
- 需求拆解、代码修改计划、README、Commit Message、修复 Prompt
- Java / Spring Boot 日志规则诊断
- Prompt 模板变量渲染、默认模板选择和版本追踪
- `local-rule` 与 `openai-compatible` 双 Provider
- Provider、模型、Token、延迟、模板版本和失败原因追踪
- 受约束状态机：`GENERATING → READY_FOR_REVIEW → SAVED → CONFIRMED`
- H2 开发环境、MySQL 生产配置与 Flyway 迁移
- 15 个后端自动化测试和前后端 CI 构建

## 本地开发启动

后端默认启用 `dev` profile，使用文件型 H2，数据保存在 `backend/data`，服务重启后仍保留。

```bash
cd backend
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm ci
npm run dev
```

访问 `http://localhost:5173`，后端端口为 `8080`。H2 控制台位于 `http://localhost:8080/h2-console`。

## 测试与构建

```bash
cd backend
mvn test

cd ../frontend
npm run build
```

后端当前包含 15 个有效测试，覆盖 Prompt 渲染、缺少变量、Provider 降级、状态机、持久化、Mapper CRUD、Controller 校验和日志诊断。

## LLM Provider 配置

默认配置无需 API Key：

```text
DEVFLOW_AI_PROVIDER=local-rule
```

接入 OpenAI-compatible 服务：

```text
DEVFLOW_AI_PROVIDER=openai-compatible
DEVFLOW_AI_BASE_URL=https://api.openai.com/v1
DEVFLOW_AI_API_KEY=your-key
DEVFLOW_AI_MODEL=gpt-4.1-mini
DEVFLOW_AI_TIMEOUT_SECONDS=60
DEVFLOW_AI_MAX_TOKENS=2048
DEVFLOW_AI_FALLBACK_TO_LOCAL=true
```

API Key 只通过环境变量注入，不应写入代码或提交到仓库。真实 Provider 调用失败时，默认降级到 local-rule，并在生成记录的 `errorMessage` 中保存降级原因。

## MySQL 生产配置

使用 `prod` profile：

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://localhost:3306/devflow_copilot?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
DB_USERNAME=devflow
DB_PASSWORD=your-password
```

Flyway 会自动执行：

- `backend/src/main/resources/db/migration/V1__create_core_schema.sql`
- `backend/src/main/resources/db/migration/V2__seed_demo_data.sql`

`backend/src/main/resources/sql/init.sql` 仅作为早期 MySQL 初始化参考，正式结构以 Flyway migration 为准。

## Docker Compose

```bash
docker compose up --build
```

服务：前端 `http://localhost:5173`、后端 `http://localhost:8080`、MySQL `localhost:3306`。

## 主要 API

- `POST /api/projects`、`GET /api/projects`
- `POST /api/ai/requirement-split`
- `POST /api/ai/code-plan`
- `POST /api/ai/readme-generate`
- `POST /api/ai/commit-message`
- `POST /api/ai/fix-prompt`
- `POST /api/logs/analyze`
- `GET /api/prompts`
- `GET /api/generations`
- `POST /api/generations/{id}/save`
- `POST /api/generations/{id}/confirm`

## 项目边界

当前版本没有实现登录权限、自动修改代码、Git 提交、PR 创建、RAG 或 SSE 流式输出。OpenAI-compatible Provider 已完成配置和调用适配，但是否调用真实模型取决于使用者提供的服务地址和 API Key。

更多面试说明见 [docs/interview-ready-project-summary.md](docs/interview-ready-project-summary.md)，简历表述见 [docs/resume-bullets.md](docs/resume-bullets.md)。
