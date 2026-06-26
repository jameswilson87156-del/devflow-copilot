# DevFlow Copilot API Draft

Base URL:

- Default backend: `http://localhost:8080/api`
- Demo backend used by screenshots: `http://127.0.0.1:18090/api`
- Frontend dev proxy: `http://127.0.0.1:5174/api`

## Response Wrapper

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

## Project Context

- `POST /projects`
- `GET /projects`
- `GET /projects/{id}`
- `PUT /projects/{id}`
- `DELETE /projects/{id}`

Create or update request example:

```json
{
  "projectName": "ai-jd-analyzer",
  "techStack": "Java 17, Spring Boot 3, MyBatis-Plus, MySQL, Vue 3",
  "readmeContent": "AI 求职 Agent，用于 JD 匹配、简历优化、投递记录和面试复盘。",
  "directoryStructure": "backend/src/main/java/com/jd/agent\nfrontend/src/views/review",
  "currentRequirement": "给 AI 求职 Agent 增加投递复盘模块，支持记录面试问题和复盘建议。",
  "codingRules": "所有 AI 建议需要人工确认后使用。"
}
```

## AI Generation

- `POST /ai/requirement-split`
- `POST /ai/code-plan`
- `POST /ai/readme-generate`
- `POST /ai/commit-message`
- `POST /ai/fix-prompt`

Request example:

```json
{
  "projectId": 2,
  "input": "给 AI 求职 Agent 增加投递复盘模块，支持记录面试问题、复盘建议和下一步行动。",
  "extraContext": "review-only artifact，所有输出人工确认"
}
```

Response data example:

```json
{
  "recordId": 7,
  "generationType": "requirement-split",
  "outputContent": "## 需求摘要\n\n为 ai-jd-analyzer 增加投递复盘模块...",
  "status": "Ready for Review",
  "modelName": "local-rule-mvp",
  "costTimeMs": 120
}
```

## Log Analysis

- `POST /logs/analyze`
- `GET /logs/history`

Request example:

```json
{
  "projectId": 3,
  "rawLog": "APPLICATION FAILED TO START. Web server failed to start. Port 8080 was already in use."
}
```

## Prompt Templates

- `GET /prompts`
- `POST /prompts`
- `PUT /prompts/{id}`

## Generation History

- `GET /generations`
- `GET /generations/{id}`
- `POST /generations/{id}/save`
- `POST /generations/{id}/confirm`

## Generation Trace

- `GET /generation-traces`
- `GET /generation-traces?generationRecordId={id}`
- `GET /generation-traces/{id}`

Response data example:

```json
{
  "id": 1,
  "generationRecordId": 7,
  "promptVersion": 2,
  "inputVariables": "{\"generationType\":\"code-plan\"}",
  "renderedPromptSummary": "项目：DevFlow Copilot；需求：增加 Agent Run Trace...",
  "providerName": "local-rule",
  "modelName": "local-rule-mvp",
  "status": "READY_FOR_REVIEW",
  "latencyMs": 12,
  "errorMessage": null
}
```

## Agent Run Trace

- `GET /agent-runs`
- `GET /agent-runs?projectId={id}`
- `GET /agent-runs?generationRecordId={id}`
- `GET /agent-runs/{id}/trace`

Trace response includes:

- `run`
- `steps`
- `toolCalls`
- `humanReviews`

## Knowledge Base

- `GET /knowledge/documents`
- `POST /knowledge/documents`
- `GET /knowledge/documents/{id}/chunks`
- `POST /knowledge/search`
- `GET /knowledge/references?generationRecordId={id}`

Create document request example:

```json
{
  "title": "DevFlow Copilot 项目边界",
  "sourceType": "manual",
  "sourceUri": "docs/mvp-scope.md",
  "content": "默认使用 local-rule，本地演示不需要 API Key...",
  "embeddingModel": "",
  "metadata": "demo=true"
}
```

Search request example:

```json
{
  "query": "local-rule provider API Key",
  "topK": 5
}
```

## Dashboard

- `GET /dashboard/stats`

## Notes

- MVP 使用本地规则生成结构化 Markdown，不强依赖真实 LLM API。
- 所有 AI 输出都是 review-only artifact，需要开发者人工确认后再使用。
- Knowledge Base 当前使用关键词/简单相似度检索，不是向量数据库；embedding 字段是后续扩展点。
- Agent Run Trace 是可解释的单次 workflow 记录闭环，不是复杂多 Agent 调度 Runtime。
