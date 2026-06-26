# MVP Scope

DevFlow Copilot MVP focuses on AI Coding workflow organization, not automatic code execution.

## Included

- Project context input
- Requirement split
- Code change plan
- Java / Spring Boot log analysis
- README / Commit Message generation
- Codex / Claude fix prompt generation
- Generation history and human confirmation
- Generation Trace
- Agent Run Trace / Agent Step / Tool Call / Human Review records
- Lightweight Knowledge Base document chunking, keyword retrieval, and generation references

## Excluded

- Login and multi-user permission
- GitHub OAuth
- IDE plugin
- Automatic code modification
- Automatic Git commit
- Automatic PR creation
- Vector database / production RAG ranking
- Complex multi-agent runtime or autonomous tool execution
- Electron desktop app
- Mandatory real LLM API dependency

## AI Strategy

The current MVP uses local rule generation by default. The backend also includes an OpenAI-compatible provider path that reads API Key from environment variables and falls back to `local-rule` when configured to do so. Knowledge Base retrieval is keyword/simple-similarity based; `embedding_model` and `embedding_vector` fields are reserved for later embedding integration.
