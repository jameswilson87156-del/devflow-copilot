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

## Dashboard

- `GET /dashboard/stats`

## Notes

- MVP 使用本地规则生成结构化 Markdown，不强依赖真实 LLM API。
- 所有 AI 输出都是 review-only artifact，需要开发者人工确认后再使用。
