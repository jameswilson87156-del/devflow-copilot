# Real Provider Verification

This document describes how to verify the OpenAI-compatible Provider path without committing secrets. The default demo path remains `local-rule`, so the project can run and be screened without an API key.

## Environment Variables

Configure real provider access only through environment variables:

```powershell
$env:DEVFLOW_AI_PROVIDER = "openai-compatible"
$env:DEVFLOW_AI_BASE_URL = "https://your-compatible-provider.example/v1"
$env:DEVFLOW_AI_API_KEY = "<set-in-shell-only>"
$env:DEVFLOW_AI_MODEL = "your-model-name"
$env:DEVFLOW_AI_TIMEOUT_SECONDS = "60"
$env:DEVFLOW_AI_MAX_TOKENS = "2048"
$env:DEVFLOW_AI_FALLBACK_TO_LOCAL = "true"
$env:SERVER_PORT = "18081"
```

Do not write the real key into `.env`, source code, README, tests, screenshots, logs, or issue text. If you need local convenience, use an ignored `.env.local` outside Git tracking and load it in your shell before starting the backend.

## Start Backend

```powershell
cd backend
mvn spring-boot:run
```

When the backend starts, verify the API is reachable:

```powershell
Invoke-WebRequest -UseBasicParsing http://127.0.0.1:18081/api/dashboard/stats
```

## Send One Minimal Request

Use a short request to keep cost low:

```powershell
$body = @{
  projectId = 1
  input = "Generate a short review-only implementation plan for DevFlow Copilot provider verification."
  extraContext = "Manual real provider smoke test. Do not include secrets."
  knowledgeQuery = "provider fallback generation trace"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri http://127.0.0.1:18081/api/ai/requirement-split `
  -ContentType "application/json" `
  -Body $body
```

A successful real-provider response should include:

- `recordId`
- `generationType`
- `outputContent`
- `status`, normally `READY_FOR_REVIEW`
- `providerName`, expected `openai-compatible`
- `modelName`, expected to match `DEVFLOW_AI_MODEL`
- `costTimeMs`
- token fields if the provider returns `usage`
- `agentRunId`
- `knowledgeReferences` when retrieval matched chunks

After the request, inspect traces without exposing secrets:

```powershell
Invoke-RestMethod "http://127.0.0.1:18081/api/generation-traces?generationRecordId=<recordId>"
Invoke-RestMethod "http://127.0.0.1:18081/api/agent-runs?generationRecordId=<recordId>"
```

## Fallback Behavior

If `DEVFLOW_AI_PROVIDER=openai-compatible` but `DEVFLOW_AI_API_KEY` is missing, blank, expired, or rejected by the provider, the backend should fall back to `local-rule` when `DEVFLOW_AI_FALLBACK_TO_LOCAL=true`.

Expected fallback evidence:

- `providerName` becomes `local-rule`
- `modelName` becomes `local-rule-mvp`
- `status` remains reviewable if local generation succeeds
- `errorMessage` or fallback reason records the provider failure summary
- no API key appears in `generation_record`, `generation_trace`, `agent_run`, `tool_call_record`, frontend response, or logs

Set `DEVFLOW_AI_FALLBACK_TO_LOCAL=false` only when you intentionally want the request to fail instead of falling back.

## Secret Leakage Check

Before committing or sharing verification results, run:

```powershell
git diff -- . ":(exclude)backend/target" ":(exclude)frontend/dist"
git ls-files | Select-String "\.env|node_modules|dist|target|\.log$"
rg -n "sk-[A-Za-z0-9_-]{20,}|DEVFLOW_AI_API_KEY\s*=|OPENAI_API_KEY\s*=|Authorization:\s*Bearer" .
```

The grep may match documentation examples such as `DEVFLOW_AI_API_KEY = "<set-in-shell-only>"`; it must not match a real secret value.

## Current Verification Status

For this portfolio evidence pass, no real API key was present in the current shell, so no paid or external model request was executed. The verified local path is:

- `local-rule` generation works without secrets
- Generation Trace is written
- Agent Run Trace is written
- Knowledge Base references are returned
- Human Review can move from pending to saved and confirmed

The record below supersedes the earlier pending status for this machine: one local `openai-compatible` smoke test has now completed on the already-running backend.

## Local Verification Record - 2026-06-26

- Backend port: `8080`.
- Health/API reachability: `GET http://127.0.0.1:8080/api/dashboard/stats` returned HTTP `200` with wrapper `code=0`.
- API Key detection: the Codex verification shell did not contain `DEVFLOW_AI_API_KEY`; the already-running backend process nevertheless completed a real `openai-compatible` call, which means its own startup environment had Provider configuration available. The key value was not read, printed, logged, or written to this document.
- Minimal generation request: `POST http://127.0.0.1:8080/api/ai/requirement-split` with prompt `请用一句话说明 DevFlow Copilot 是什么。`.
- Real Provider result: success.
  - `recordId`: `13`
  - `providerName`: `openai-compatible`
  - `modelName`: `gpt-5.5`
  - `costTimeMs`: `22937`
  - `status`: `READY_FOR_REVIEW`
  - `errorMessage`: `null`
  - `agentRunId`: `5`
- Generation Trace verification: `GET /api/generation-traces?generationRecordId=13` returned HTTP `200`, one trace row, `providerName=openai-compatible`, `modelName=gpt-5.5`, `latencyMs=22937`, `status=READY_FOR_REVIEW`, `errorMessage=null`.
- Agent Workflow verification: `GET /api/agent-runs?generationRecordId=13` returned HTTP `200` with one run. `GET /api/agent-runs/5/trace` returned HTTP `200` with `5` steps, `3` tool calls, and `1` human review.
- Knowledge Reference verification: the generation response returned `3` knowledge references, and `GET /api/knowledge/references?generationRecordId=13` returned HTTP `200` with `3` references.
- Fallback verification: source and tests confirm missing API Key fallback. `OpenAiCompatibleGenerationProvider` rejects blank `DEVFLOW_AI_API_KEY`; `GenerationProviderRouter` falls back to `local-rule` when `DEVFLOW_AI_FALLBACK_TO_LOCAL=true`; `ProviderAndDiagnosisTest.routerFallsBackToLocalRuleWhenOpenAiConfigIsMissing` covers this behavior. No backend restart was performed for an additional no-key runtime test.
- Secret handling: no full API Key was requested from the user, inspected from the backend process, printed to console, or committed to files.
