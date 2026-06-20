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

## Excluded

- Login and multi-user permission
- GitHub OAuth
- IDE plugin
- Automatic code modification
- Automatic Git commit
- Automatic PR creation
- Complex RAG
- Electron desktop app
- Mandatory real LLM API dependency

## AI Strategy

The current MVP uses local rule generation. The backend keeps `LlmGenerateService` as an extension point for later LLM API integration.
