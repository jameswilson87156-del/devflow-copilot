# DevFlow Copilot 部署方案

## 目标

提供可执行的前端、后端、数据库部署方案，并保留无 API Key 的游客演示模式。本文是计划，不代表当前已经生产上线。

## 方案 A：单机 Docker Compose

适用：个人作品集演示、面试前临时部署、低成本云服务器。

组件：

- `frontend`：Vite 构建后由 Nginx 提供静态资源，并反向代理 `/api` 到后端。
- `backend`：Spring Boot 3 应用，运行 `prod` profile。
- `mysql`：MySQL 8，使用 volume 保存数据。

步骤：

```bash
cp .env.example .env
# 手动填写 DB_PASSWORD、MYSQL_ROOT_PASSWORD
# 如需真实模型，再填写 DEVFLOW_AI_PROVIDER=openai-compatible 和 DEVFLOW_AI_API_KEY
docker compose up --build -d
```

当前仓库还没有提交 `.env.example`；上线前应新增只含占位符的模板文件，不能提交真实 `.env`。

## 方案 B：前后端分离部署

适用：前端放 Vercel/Netlify/静态站点，后端和数据库放云服务器或容器平台。

前端：

```bash
cd frontend
npm ci
npm run build
```

将 `frontend/dist` 部署到静态托管平台。平台需要配置 `/api` 反向代理，或在构建时设置后端 API 地址。

后端：

```bash
cd backend
mvn -B clean package
java -jar target/devflow-copilot-backend-0.0.1-SNAPSHOT.jar
```

生产环境变量：

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://<host>:3306/devflow_copilot?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
DB_USERNAME=devflow
DB_PASSWORD=<strong-password>
DEVFLOW_AI_PROVIDER=local-rule
DEVFLOW_AI_BASE_URL=https://api.openai.com/v1
DEVFLOW_AI_MODEL=gpt-4.1-mini
DEVFLOW_AI_FALLBACK_TO_LOCAL=true
```

只有需要真实模型演示时才设置：

```text
DEVFLOW_AI_PROVIDER=openai-compatible
DEVFLOW_AI_API_KEY=<secret>
```

## API Key 保护

- API Key 只通过环境变量或云平台 Secret Manager 注入。
- 不把 `.env`、`.env.*`、日志、截图中的 Key、控制台输出中的 Key 提交进 Git。
- 后端返回 Trace 时只记录 provider、model、status、latency、错误摘要，不记录 Authorization Header 或 API Key。
- 公开游客演示默认使用 `local-rule`，避免不受控成本。

## 游客演示模式

推荐默认配置：

```text
DEVFLOW_AI_PROVIDER=local-rule
DEVFLOW_AI_FALLBACK_TO_LOCAL=true
```

游客可以演示：

- Prompt Studio 试运行
- Generation Trace
- Agent Run Trace
- Knowledge Base 文档创建、切片、检索
- Human Review 保存和确认

游客模式不能演示真实模型质量，只演示工程闭环和控制台能力。README 和简历中应明确说明。

## 数据库迁移

Flyway 是正式表结构来源，当前迁移包含：

- `V1__create_core_schema.sql`
- `V2__seed_demo_data.sql`
- `V3__agentic_workflow_schema.sql`
- `V4__seed_agentic_workflow_data.sql`

部署前先在测试库验证 migration；生产库不手工改表。

## 上线前检查清单

- `cd backend && mvn test`
- `cd frontend && npm run build`
- `git diff --check`
- 检查仓库中没有 `.env`、真实 Key、`node_modules/`、`dist/`、`target/`、日志文件。
- 如果使用 Docker：先运行 `docker compose config`，再运行 `docker compose up --build -d`。
- 访问 `/api/dashboard/stats` 验证后端；访问前端 Dashboard、Agent Run Trace、Prompt Studio、Knowledge Base 验证页面。

## 后续可选增强

- 加只读游客账号和后台管理账号。
- 接入对象存储保存截图或导出审计报告。
- 接入 embedding 模型和向量数据库，将当前 `embedding_model`、`embedding_vector` 预留字段替换为真实索引。
- 将长耗时生成改为异步任务，并使用 SSE 推送 Agent Step 状态。
