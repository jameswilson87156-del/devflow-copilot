# DevFlow Shared Provider Audit

## 1. Audit Scope

This is a read-only audit of the current provider implementation and its configuration surface. No provider request, network call, source-code change, configuration change, test execution, dependency change, commit, push, or pull request was performed. This document intentionally does not contain configuration values, credentials, authorization material, endpoint hosts, or model identifiers.

## 2. Git Baseline

- Working tree was clean before the audit.
- Branch and starting revision matched the requested baseline.
- The branch has no commits ahead of or behind `origin/main`.
- This audit document is the only intended working-tree addition.

## 3. Environment Variable Presence

The six shared variables were checked only for presence. All were present in the current process environment:

| Variable | Presence |
| --- | --- |
| `PORTFOLIO_AI_PROVIDER` | PRESENT |
| `PORTFOLIO_AI_BASE_URL` | PRESENT |
| `PORTFOLIO_AI_MODEL` | PRESENT |
| `PORTFOLIO_AI_API_KEY` | PRESENT |
| `PORTFOLIO_AI_PROTOCOL` | PRESENT |
| `PORTFOLIO_AI_FALLBACK_ENABLED` | PRESENT |

No variable value was read or recorded.

## 4. Current Configuration Model

- Primary configuration is `backend/src/main/resources/application.yml`; profile files under the same directory only override the selected provider for development and production, while the test profile selects the local implementation and leaves the key blank.
- `devflow.ai` binds to `backend/src/main/java/com/devflow/copilot/config/AiProviderProperties.java` through `@ConfigurationProperties`; application bootstrap enables that class explicitly.
- Existing project-specific configuration supports provider selection, base URL, API key, model, timeout seconds, maximum tokens, and fallback-to-local. It has no protocol property and no configuration diagnosis endpoint.
- The project-specific environment variable names use the `DEVFLOW_AI_*` prefix. The safe default selects the local rule implementation; base URL and model both have code/configuration defaults, which are intentionally not reproduced here.
- `AiProviderProperties` uses explicit accessors rather than Lombok `@Data`; it has no generated `toString()`. There is no observed logging of the properties object.

## 5. Current Provider Architecture

- Provider interface: `GenerationProvider` with `key()` and `generate(ProviderRequest)`.
- Local implementation: `LocalRuleGenerationProvider`. It deterministically renders rule-based artifacts and estimates token counts; it does not make an HTTP call.
- External implementation: `OpenAiCompatibleGenerationProvider`.
- Router: `GenerationProviderRouter`, invoked through `LlmGenerateServiceImpl` by `LocalRuleGenerateService` (the workflow service name does not constrain it to local execution).
- Routing is exact-string selection: the local key calls the local implementation; the supported external key calls the external implementation; every other selected value fails as unsupported before the fallback block.

## 6. Current HTTP Protocol

- The external implementation uses Spring `RestClient` backed by `SimpleClientHttpRequestFactory`.
- It removes one trailing slash from the configured base URL and issues an HTTP `POST` to `/chat/completions`; therefore the configured base URL is expected to include the API version prefix where required. It does **not** use `/responses`.
- Request body is a map containing `model`, one user `messages` item from the rendered prompt, `temperature`, and `max_tokens`.
- Response is deserialized as Jackson `JsonNode`; content is extracted from `choices[0].message.content`. Usage reads the three conventional token counters when present.
- Connect and read timeouts both derive from the same timeout-seconds property (minimum one second). Maximum token count comes from the configured maximum-tokens property.
- Authorization is set as a default HTTP header inside the `RestClient` builder. No request or response DTO class is used: maps are used for the request and `JsonNode` for the response.

## 7. Current Provider Routing

- Provider selection occurs in `GenerationProviderRouter` from `AiProviderProperties`.
- The configured default is local and is safe when no project-specific AI environment variables are set.
- The router only falls back when the selected external implementation throws a `RuntimeException` and `fallbackToLocal` is true.
- Fallback returns a result whose provider/model fields describe the actual local result and whose fallback reason is the original exception message.
- Unsupported provider selection, including a spelling/case mismatch, occurs outside the fallback `try` block and therefore becomes a failed generation.

## 8. Current Fallback Behavior

- Fallback is a real, configurable behavior and defaults to enabled.
- Missing API key is checked before client construction. With fallback enabled it yields a successful local artifact; with fallback disabled it fails.
- Connection failures, timeouts, HTTP status failures, non-JSON/invalid JSON conversion failures, missing `choices`, missing/blank content, and unknown response shapes all reach the broad external-provider exception path and therefore fall back when enabled.
- On fallback, `GenerationRecord` and `GenerationTrace` receive the actual local provider/model returned by the router. The fallback reason is stored as the record error message and trace error message even though the record is successful.
- `AgentRun` is created before generation from configured provider/model and is not updated after fallback. It can therefore describe the requested provider while the record and trace describe the actual provider. The generation tool-call output records the fallback reason.

## 9. Current Error Handling

- The external provider converts all non-provider exceptions into `LlmProviderException`; existing provider exceptions pass through unchanged.
- With fallback disabled, failures propagate to `LocalRuleGenerateService`, which persists a `FAILED` generation record, failed agent step/tool call, trace, and agent-run transition, then rethrows.
- With fallback enabled, the router absorbs all external-provider runtime failures listed above. A local result is persisted as successful, with the original reason retained in the error-message fields.
- HTTP 401, 403, 429, and 5xx statuses are not classified individually; they depend on `RestClient` exception behavior and are handled by the same broad fallback/failure path.
- `safeMessage` returns raw exception messages. This preserves useful causes but can include upstream diagnostic text.

## 10. Trace and Persistence Flow

1. `LocalRuleGenerateService` renders a prompt and creates `generation_record` in `GENERATING` state, including provider/model configuration, prompt-template metadata, and rendered prompt.
2. It creates `agent_run` linked by `generation_record_id`, then writes decomposition/prompt/retrieval steps and related tool-call summaries.
3. On success it stores returned provider/model, token counts, latency, output, and fallback reason in `generation_record`; it then writes `generation_trace` with prompt version, input variables, prompt summary, provider/model, status, latency, and error message.
4. It records the provider tool call, creates a pending human review, and transitions the record to `READY_FOR_REVIEW`; the agent run becomes `WAITING_REVIEW`.
5. `generation_record` is linked to `agent_run`; `agent_step.run_id` links steps to a run; `tool_call_record` links to both run and optional step; `human_review` links to both run and generation record. Initial human-review status is `PENDING`.

Persistence locations:

- Provider/model: `generation_record`, `generation_trace`, and `agent_run`.
- Latency: `generation_record.cost_time_ms`, `generation_trace.latency_ms`, `agent_run.latency_ms` (defined but not populated by the observed start-run flow), `agent_step.latency_ms`, and `tool_call_record.latency_ms`.
- Error/fallback reason: `generation_record.error_message`, `generation_trace.error_message`, provider tool-call output summary, and failed-step summary.
- Prompt version: `generation_record.prompt_template_version` and `generation_trace.prompt_version`.

## 11. Secret Exposure Risks

- No direct logging of the properties object, API key, authorization header, full provider request, or full provider response was found in provider code.
- API-key values are not intentionally written to the observed entities or migrations. The trace test explicitly asserts that its input-variable JSON does not contain the project API-key variable name.
- Risk: raw upstream exception messages flow through `safeMessage` into API business-error responses (when no fallback), `generation_record.error_message`, `generation_trace.error_message`, agent-step summary, and tool-call output summary. Depending on the HTTP client/upstream response, these may disclose endpoint information, upstream HTML/error bodies, or request-adjacent data.
- Risk: `GlobalExceptionHandler` logs an exception stack trace for unhandled exceptions. Provider exceptions normally use the business-exception branch, but unexpected wrapping paths still need careful review.
- Risk: prompt content, input variables, output summaries, and tool-call summaries are persisted. They are not credential fields by design, but secrets supplied in user input could be stored.

## 12. Existing Test Coverage

- `ProviderAndDiagnosisTest` covers missing API key rejection by the external provider and router fallback to the local provider when the key is absent.
- `GenerationWorkflowIntegrationTest` covers local generation, persisted workflow metadata, history lookup, and record state transitions.
- `AgenticWorkflowIntegrationTest` covers generation trace, agent run/steps, tool calls, pending/confirmed human review, knowledge references, and a negative assertion for the project API-key variable name in trace input variables.
- Not covered: actual HTTP request construction/protocol compatibility, endpoint joining, timeout behavior, 401/403/429/5xx classification, non-JSON/invalid JSON/empty body behavior, fallback disabled, unsupported provider selection, sensitive-error redaction, or the fallback mismatch between `AgentRun` and actual provider metadata.
- Tests were read but not run; no real network test was performed.

## 13. Shared Variable Mapping Proposal

Design only; do not implement in this audit round.

| Setting | Resolution order | Safe fallback / rule |
| --- | --- | --- |
| Provider | project-specific variable, then shared variable | local rule implementation |
| Base URL | project-specific variable, then shared variable | retain the current safe configured default without exposing it |
| Model | project-specific variable, then shared variable | retain the current safe configured default without exposing it |
| API key | project-specific variable, then shared variable | empty value |
| Protocol | project-specific variable, then shared variable | only the currently implemented chat-completions-compatible protocol |
| Fallback flag | project-specific variable, then shared variable | enabled |

- Spring nested placeholders are suitable only if they preserve the required precedence and retain the existing safe default. Verify this with configuration-binding tests that assert presence/selection only and never print values.
- A protocol enum is recommended before supporting more than one protocol. Its only supported value should map to the current implementation; responses-style and any other values should be rejected as unsupported rather than inferred from provider name.
- A configuration class need not be added if nested property resolution remains readable and testable. A dedicated resolver is preferable if it is needed to avoid duplicated precedence logic or to produce a safe redacted diagnosis.
- Configuration validation should check presence/blankness and supported protocol/provider combinations without reading, printing, persisting, or returning the API-key value.

## 14. Minimal Files Requiring Changes

For the subsequent implementation phase, the smallest expected set is:

1. `backend/src/main/resources/application.yml` for precedence-aware shared-variable placeholders and protocol configuration.
2. `backend/src/main/java/com/devflow/copilot/config/AiProviderProperties.java` for protocol binding/validation if an enum or explicit resolver is selected.
3. `backend/src/main/java/com/devflow/copilot/service/provider/GenerationProviderRouter.java` for safe protocol/provider validation and explicit fallback semantics.
4. `backend/src/main/java/com/devflow/copilot/service/provider/OpenAiCompatibleGenerationProvider.java` for protocol dispatch only if additional supported protocols are actually implemented, plus error redaction.
5. `backend/src/main/java/com/devflow/copilot/service/impl/LocalRuleGenerateService.java` and/or trace persistence code to distinguish requested from actual provider and keep fallback evidence safely.
6. Provider-focused tests, including configuration precedence and error-redaction cases.

## 15. Files That Must Not Be Changed

In this audit round, do not change Java source, YAML, tests, frontend code, README, existing design documents, database migrations, package manifests/lockfiles, provider implementations, Codex/Claude configuration, or `OPENAI_API_KEY`. Do not add dependencies, perform provider calls, store provider responses, commit, push, or create a pull request.

## 16. Recommended Implementation Order

1. Add tested, value-safe configuration resolution with project-specific variables taking precedence over shared variables and local behavior as the final default.
2. Add an explicit protocol enum/validation that recognizes only the existing supported protocol; fail safely for unsupported values.
3. Make router fallback criteria explicit and preserve requested-provider versus actual-provider metadata separately.
4. Redact and bound provider exception text before API, trace, record, agent-step, and tool-call persistence.
5. Add isolated tests for missing-key, fallback enabled/disabled, unsupported provider/protocol, HTTP error categories, malformed/empty responses, timeout configuration, metadata consistency, and redaction. Keep all such tests offline.
6. Run the targeted offline test suite only after those changes are approved.

## 17. Current Limitations and Claims Boundary

- This audit establishes only the behavior visible in the checked-in code at the baseline revision; it does not claim a production provider integration is stable or validated.
- No external/shared environment values were read, and no external endpoint or model identifier is recorded here.
- No provider was invoked and no real response was obtained.
- The only implemented external request form is chat-completions-compatible. Other protocols are unsupported by current code and must not be claimed as available.
- The initial shared-variable mapping proposal has now been implemented as described below; it has only offline test evidence.

## 18. Implemented Configuration Mapping

Sections 1-17 are the pre-implementation audit snapshot. Sections 18 onward record the approved offline implementation and verification work.

- `application.yml` now resolves project-specific AI settings first, then the corresponding shared setting, then the established safe default. This covers provider, base URL, model, API key, protocol, and fallback.
- Development and production profile provider overrides use the same project-specific then shared precedence, so profile activation does not bypass shared configuration.
- Project-specific variable names are unchanged. No configuration value was read, emitted, persisted, or added to this document.
- `AiProviderProperties` binds the explicit protocol field using ordinary Spring configuration binding and still has no generated or custom `toString()` that could expose the API key.

## 19. Protocol Compatibility Gate

- The external provider validates the configured protocol before checking credentials, constructing a client, or issuing an HTTP request.
- Only the currently implemented chat-completions-compatible form passes the gate and continues to the existing `POST /chat/completions` implementation.
- An unsupported protocol is classified as `PROTOCOL_UNSUPPORTED`; with fallback enabled it produces a local result, and with fallback disabled it fails without an HTTP request.
- Unknown provider selection is classified as `UNKNOWN_PROVIDER_ERROR` and remains a failure without an HTTP request or silent provider substitution.

## 20. Error Sanitization Strategy

- Provider failures now use the bounded `ProviderErrorType` categories: `PROTOCOL_UNSUPPORTED`, `API_KEY_MISSING`, `TIMEOUT`, `AUTHENTICATION_FAILED`, `RATE_LIMITED`, `UPSTREAM_SERVER_ERROR`, `INVALID_RESPONSE`, `EMPTY_CONTENT`, `CONNECTION_FAILED`, and `UNKNOWN_PROVIDER_ERROR`.
- The external provider preserves the original cause only in memory. Its outward and persisted message is the category plus a fixed safe summary; it never copies raw upstream exception text.
- Fallback reasons, generation-record errors, generation-trace errors, failed step summaries, failed tool-call summaries, and API business errors use that safe message.
- The global fallback exception logger now logs only an exception class name, not an exception stack trace or message.

## 21. Offline Test Evidence

- `SharedProviderConfigurationMappingTest` verifies project-specific precedence, shared fallback, safe defaults, and fallback-setting precedence against the checked-in YAML placeholders.
- `ProviderRoutingSafetyTest` verifies the protocol gate, enabled/disabled fallback behavior, missing-key no-request path, and unknown-provider no-request path.
- `ProviderErrorClassifierTest` verifies offline classification for timeout, connection failure, authentication failure, rate limit, upstream server error, and invalid response.
- `ProviderErrorSanitizationIntegrationTest` injects an offline synthetic provider failure and verifies that the returned exception, generation record, generation trace, failed step, and failed tool-call output retain only the safe classification.
- The complete backend test suite passed offline. No real provider or network call was made by these tests.

## 22. Remaining Work Before Real Validation

- The existing persistence schema records actual provider/model after a successful result, including fallback, and the agent run is now updated to that actual metadata. It does not separately persist requested provider/model or a dedicated fallback flag; the safe fallback reason remains the compatible evidence field.
- Real endpoint reachability, provider-specific response variations, credential validity, and production-network behavior remain unvalidated and must be tested only in an approved later phase.
- Before any real validation, retain the protocol allowlist, keep error messages bounded, and confirm operational logging/configuration never exposes credentials or upstream payloads.

## 23. Implementation Diff Review

- Reviewed every changed and added backend, configuration, test, and audit file against the baseline. No frontend, README, screenshot, frozen Trace Workspace, binary, migration, or unrelated business file was changed.
- Configuration precedence is implemented through nested Spring placeholders. Development and production provider profile overrides preserve the same precedence.
- `SharedProviderConfigurationMappingTest` was corrected to create a Spring context, bind `AiProviderProperties`, and remove host system property/environment sources. It now validates binding rather than only expanding YAML text.
- All Spring integration tests explicitly force the offline local provider, blank test credential, supported protocol, and enabled fallback through test properties. This prevents host shared/provider settings from selecting an external provider during tests.

## 24. Confirmed Runtime Semantics

- The external provider validates the protocol before credential handling, HTTP-client construction, header construction, or request execution. The request path remains `POST /chat/completions`.
- The router selects once. It catches only `LlmProviderException`, so only classified provider failures can trigger one local fallback. It neither recurses nor treats arbitrary runtime, database, or application defects as fallback candidates.
- Missing credentials, unsupported protocol, and unknown provider selection cannot create or send an external request. Unsupported protocol and missing credentials can fall back once when enabled; unknown provider remains a classified failure.
- Base-URL normalization removes a trailing duplicate chat-completions suffix before the fixed request path is appended. Response validation now distinguishes malformed/missing required fields from intentionally empty content.
- HTTP status-based classification occurs from response status codes without parsing English exception messages. Offline loopback tests cover authentication failures, rate limit, server error, invalid JSON, missing response fields, empty content, timeout, and connection failure through the provider path.

## 25. Corrected Defects

- **P0 corrected:** Spring integration tests could inherit host provider environment values because profile YAML has lower property precedence than the environment. Explicit test properties now isolate the test provider configuration.
- **P1 corrected:** the router's broad `RuntimeException` catch could have downgraded programming or persistence defects to local fallback. It now catches only classified provider exceptions.
- **P1 corrected:** a configured base URL ending in the request endpoint could produce a duplicated endpoint path. The provider normalizes that suffix before appending the fixed path.
- **P1 corrected:** missing response fields were previously treated as empty content. They now produce `INVALID_RESPONSE`; genuinely blank content remains `EMPTY_CONTENT`.
- **P2 corrected:** global exception logging no longer suppresses normal internal exception diagnostics. Only exception chains containing a provider exception are reduced to a safe type-only log entry.

## 26. Database Compatibility

- No entity field, mapper, migration, table, or historical data was changed. H2 and MySQL schema compatibility therefore remains unchanged.
- Existing columns persist actual provider/model after a completed result, and the agent run is updated to the same actual execution metadata. Safe fallback/error evidence remains in existing error-message and tool/step summary fields.
- The schema does **not** separately persist requested provider, requested model, actual provider, actual model, fallback-used, and fallback reason as distinct immutable fields. In particular, the original requested provider/model may be overwritten by actual execution metadata after fallback. This is a documented limitation, not a claim of full requested/actual auditability.
- No frozen Trace frontend contract was changed. Tool-call records retain their real run/step associations and human-review behavior is unchanged.

## 27. Real Validation Readiness

`BLOCKED_BEFORE_REAL_PROVIDER_VALIDATION`

Blocking reason: the current schema lacks separate, immutable requested/actual provider and model fields plus a dedicated fallback-used field. A future approved migration and compatible trace/API design are required before claiming complete requested-versus-actual evidence for real validation. This status does not indicate a production outage and does not authorize a real provider call.

## 28. Provider Execution Metadata Model

The blocking persistence gap described in sections 22, 26, and 27 is now resolved by the approved minimal schema change below.

- `requested_provider`: the initially selected provider at generation start; it is never overwritten by the execution outcome.
- `requested_model`: the initially selected model at generation start; direct local execution records its controlled local model rather than an unrelated external-model configuration. It is never overwritten by the execution outcome.
- Existing `provider_name` and `model_name` on `generation_record` now explicitly mean **actual** provider and model for records created after this migration. They remain null when no provider completed the generation.
- `fallback_used`: true only when the selected external provider produced a classified provider failure and the router actually completed one local fallback.
- `fallback_reason`: nullable, controlled `ProviderErrorType` name used only for a successful fallback. It does not contain a raw exception, endpoint, authorization data, request body, or response body.

`AiGenerateResponse` retains the existing provider/model fields for API compatibility and adds requested/actual aliases plus the two fallback fields. No existing JSON field was removed or renamed.

## 29. Canonical Persistence Source

- `generation_record` is the canonical source for all requested/actual/fallback execution metadata.
- `generation_trace` and `agent_run` continue to carry their existing provider/model columns as operational copies of the completed actual execution only. They obtain complete requested/fallback context through their existing `generation_record_id` association; no redundant columns were added to all three tables.
- At generation start, the record contains requested metadata and no fabricated actual execution metadata. A successful result writes actual metadata; a failed, non-fallback result leaves actual metadata null. `GenerationTrace` and `AgentRun` therefore remain consistent with the canonical record for actual execution.

## 30. Flyway Migration Evidence

- The audited highest existing migration was V4, so the minimal next migration is `V5__add_provider_execution_metadata.sql`.
- V5 adds bounded `VARCHAR(64)` / `VARCHAR(128)` request fields, `BOOLEAN NOT NULL DEFAULT FALSE` for fallback usage, and nullable `VARCHAR(64)` fallback reason. These types are compatible with the project H2 MySQL-mode test database and the production MySQL schema style.
- Existing records are conservatively backfilled by copying the historical provider/model fields into the new requested fields. Their existing provider/model values are not rewritten, fallback usage remains false by default, and no fallback reason is invented. Historical rows consequently retain their prior ambiguity rather than receiving fabricated execution evidence.
- `ProviderExecutionMetadataMigrationTest` starts from the Flyway-managed H2 schema, verifies the V5 columns and defaults, and verifies that a seeded legacy row remains readable with conservative request metadata.

## 31. Requested and Actual Semantics

- Direct local execution: requested and actual metadata identify the local implementation; fallback fields are false/null.
- External success: requested metadata identifies the selected external execution and actual metadata is written only from the returned provider result; fallback fields are false/null.
- Classified external failure with fallback enabled: requested metadata remains unchanged, actual metadata identifies the local completed result, `fallback_used` is true, and `fallback_reason` is the controlled error category.
- Classified external failure with fallback disabled: the record transitions to `FAILED`, requested metadata remains available, actual metadata remains null, fallback fields are false/null, and `error_message` retains only the safe error category and fixed summary.
- Unknown provider: no HTTP request and no fallback occur; the failed record has requested metadata but no actual metadata.

## 32. Fallback Persistence Evidence

- `ProviderResult` now carries an explicit fallback-used boolean. The router sets it only in its single classified-exception fallback branch and stores only the `ProviderErrorType` name as the fallback reason.
- `LocalRuleGenerateService` creates the canonical requested metadata before dispatch, persists actual metadata only after a result, and preserves null actual metadata on failure. Successful fallback no longer abuses `error_message` as fallback evidence.
- Offline tests cover direct local execution, synthetic external success, missing-credential fallback, disabled fallback failure, unknown-provider failure, safe error persistence, migration/backfill behavior, and trace/agent-run actual-metadata consistency. Test-profile configuration is fixed to local/offline settings so host shared-provider values cannot select an external endpoint.

## 33. Synthetic Real Validation Readiness

`READY_FOR_SYNTHETIC_REAL_PROVIDER_VALIDATION`

This conclusion is limited to offline synthetic validation. It is supported by the migration test and targeted persistence scenarios, all run without a real provider, model, external network endpoint, or remote database. It does not claim that credentials, endpoint reachability, provider compatibility, model behavior, production observability, or production availability has been validated.

## 34. Synthetic Real Provider Validation

One permitted real-provider request was attempted through the normal DevFlow Generation API and workflow with a fully fictional, review-only library-lending planning task. Fallback was disabled for the validation process, no retry was made, and no local-rule result was substituted.

`SYNTHETIC_REAL_PROVIDER_VALIDATION_FAILED`

The request did not produce a successful response artifact. This result does not indicate production availability or provider stability.

## 35. Real Request Evidence

- Exactly one external request was attempted.
- The failure persisted a controlled error classification and fixed safe summary only.
- Provider-side receipt cannot be confirmed from the safely retained local evidence; no raw upstream response or endpoint is retained.
- The synthetic input contained no real user, company, commercial, resume, ticket, or production information.

## 36. Persistence Evidence

- A failed Generation Record was persisted with requested metadata retained.
- Actual provider/model metadata remained null rather than being fabricated after the failure.
- `fallback_used` remained false and the fallback reason remained null.
- The Generation Trace and Agent Run are associated with the failed record. No additional successful trace or local-rule execution evidence was created.

## 37. Secret Scan Evidence

- A precise persisted-data scan found no raw credential, authorization value, credential-token value, or endpoint.
- One generic technical phrase occurred in the rendered prompt. It is not a secret or endpoint and is intentionally not reproduced in this document.
- Evidence files contain no configuration values, headers, endpoints, provider identifiers, model identifiers, or upstream response bodies.

## 38. Final Validation Conclusion

`SYNTHETIC_REAL_PROVIDER_VALIDATION_FAILED`

No further real-provider request is authorized by this validation run. The failure has safe persistence evidence, but real endpoint reachability and successful provider compatibility remain unverified.

## 39. Failed Real Validation Forensics

The failed synthetic real-provider validation is supported by the persisted Generation Record, Generation Trace, Agent Run, and Agent Step evidence. The record is `FAILED`; requested metadata remains present, actual metadata remains absent, fallback remains false, and no fallback reason exists. This preserves the actual failed-execution fact without fabricating successful execution metadata.

## 40. Confirmed Failure Stage

`UNKNOWN_STAGE`

The controlled category proves that the provider transport path was entered, but safe evidence does not distinguish connection establishment, TLS negotiation, request transmission, or waiting for a response. No HTTP response, response body, or response parsing evidence was retained. The precise lower-level stage is therefore intentionally not inferred.

## 41. Confirmed Failure Category

`TIMEOUT`

The latest failed record stores the controlled timeout category and its fixed safe summary. This is consistent with the provider classifier and does not expose the original transport exception.

## 42. Offline Reproduction

Loopback tests reproduce timeout handling through the actual provider path and verify classification of status failures, malformed/empty responses, URI joining, and connection failure. The complete offline suite passes. These tests establish that the code safely classifies the observed class of failure; they cannot establish the remote cause of this one real timeout.

## 43. Repair Decision

No code repair is applied. Configuration binding, protocol support, router recognition, URI parsing, and request-path joining validate without exposing runtime values. No reproducible code defect has been found, and the sanitised timeout evidence cannot distinguish provider delay from network or transport delay. Altering code or a configuration value would therefore be speculative.

## 44. Controlled Retry Readiness

`INSUFFICIENT_SANITIZED_EVIDENCE`

No second real-provider request was made. A later controlled retry requires separate authorization and additional safe observability that can distinguish transport stages without retaining secrets or raw upstream payloads.

## 45. Connectivity Preflight

The authorized preflight validated URI parsing, DNS resolution, TCP connection, and TLS handshake. Each completed in the shortest recorded duration band. It sent no HTTP request and used no authorization material. The preflight does not retain endpoint, host, address, port, certificate, or path details.

## 46. Controlled Retry Configuration

The controlled retry ran with fallback disabled, a bounded provider timeout, and a limited generation budget. These controls applied only to the validation process; no permanent environment variable or application configuration file was changed. Runtime checks confirmed all controls were active before the request.

## 47. Controlled Retry Result

One authorized retry was attempted after the successful preflight. It received an HTTP status response, but no usable response body or parsed artifact. The controlled category is `UPSTREAM_SERVER_ERROR`. The failed Generation Record, Trace, Run, and Steps were persisted; fallback was not used and actual execution metadata was not fabricated.

## 48. Final Shared Provider Status

`BLOCKED_UPSTREAM_5XX_UNCLASSIFIED`

The preflight rules out the tested local URI/DNS/TCP/TLS path. The controlled HTTP-status failure is classified as an upstream service failure. No further real request is authorized. This does not claim production availability, provider stability, SLA verification, full model compatibility, or complete real-world error coverage.

## 49. Safe HTTP Observability Defect

The second controlled request safely established an HTTP 5xx family response, but the previous implementation reduced the response exception immediately to `UPSTREAM_SERVER_ERROR`. It discarded the exact status code and other non-content diagnostic facts before the failure reached the canonical Generation Record. There is no 429 evidence, so rate limiting is not the current conclusion.

## 50. Provider Failure Metadata Model

`ProviderFailureMetadata` is a deliberately content-free immutable model carried by `LlmProviderException`. It contains only the controlled error type, failure stage, status code/family, body-presence and size bucket, content-type category, header-presence booleans, generated client request ID, and duration bucket. It excludes response bodies, URL/host data, credentials, provider/model values, and all header values. Exception JSON serialization and `toString()` exclude the cause.

## 51. Exact HTTP Status Persistence

V6 adds nullable provider-failure observability columns only to `generation_record`, the canonical execution source. Existing records are untouched and retain null observability values. On a classified provider failure, the record persists the exact numeric HTTP status when one exists, its `4XX` or `5XX` family, and the remaining safe metadata. Trace and agent-run records continue to obtain this context through their existing record association; no redundant fields were added to all workflow tables.

## 52. Offline 5xx Classification Evidence

Loopback-only tests execute the real provider path and retain exact status metadata for 500, 502, 503, and 504, all classified as `UPSTREAM_SERVER_ERROR`. A 429 remains `RATE_LIMITED` with an exact 429 status and a `4XX` family, proving the two categories stay distinct. HTML and JSON error bodies are reduced to presence, size, and content-type category only; retry and upstream-request headers are represented solely as presence booleans.

## 53. Remaining Compatibility Hypotheses

The current evidence does not establish the cause of the observed 5xx family response. Pending hypotheses are: temporary upstream server failure, gateway model-routing failure, chat-completions compatibility, temperature compatibility, token-limit compatibility, and a non-standard upstream error response. None is treated as a confirmed cause, and no request compatibility strategy was changed in this round.

## 54. Capability Probe Readiness

`READY_FOR_SINGLE_MODEL_CAPABILITY_PROBE`

The observability implementation and its offline evidence are complete. The current accurate blocking description before any separately authorized probe is `BLOCKED_UPSTREAM_5XX_UNCLASSIFIED`: no 429 evidence exists, the exact previous status code was not retained, and the observed 5xx family does not establish outage, routing, model compatibility, or parameter compatibility. This round made no network request. Any next network operation must be one separately authorized, single safe Capability Probe.

## 55. Final Single Chat Path Attempt

One final minimal real Chat Completions validation was executed. The current-round external request count is 1, and the historical cumulative real request count is 3. No second current-round request was made, no automatic retry was executed, and GET /models was not executed. The validation used fallbackDisabled=true, timeoutConfigured=true, and maxTokensLimited=true only for the validation process.

## 56. Exact Real HTTP Evidence

The DevFlow Generation API returned an HTTP 200 result for the final call, and the provider success path produced a parsed non-empty generation artifact. The safe final evidence records HTTP status code 200, status family `2XX`, responseBodyPresent=true, responseParsingCompleted=true, and responseContentPresent=true. No raw request body, raw response body, endpoint, host, credential, provider value, model value, or client request ID value is retained.

## 57. Final Persistence Evidence

The final attempt persisted the Generation Record, Generation Trace, Agent Run, and Agent Step chain. Requested provider/model metadata is present, actual provider/model metadata is present, requested and actual provider/model metadata match in-memory, fallbackUsed=false, and fallbackReason is null. Success-path failure observability fields remain unset because no provider failure was classified. The persisted content exists, but sanitized keyword checks did not confirm relation to the fictional book-borrowing review task.

## 58. DevFlow Provider Integration Closure

`REAL_PROVIDER_TRANSPORT_AND_PERSISTENCE_VALIDATED`

The real provider transport and persistence integration reached a successful HTTP and parsing path. The earlier relevance result was based on a narrow sanitized checker and did not include the persisted output text in the review bundle. No automatic code fix, additional provider request, GET /models probe, commit, push, or PR was performed.

## 59. Offline Semantic Review of Persisted Output

No new external request, provider call, model call, retry, or GET /models probe was performed. The latest persisted Generation Record was read offline from the local database copy. It is successful, has non-empty output, has a rendered prompt, retains token usage, and remains associated with Generation Trace, Agent Run, and Agent Step records. Requested and actual provider/model metadata are present and match, but their values are intentionally not recorded.

The rendered prompt contains the synthetic Spring Boot teaching task, the exactly-three-items constraint, the transaction-boundary requirement, the inventory-concurrency requirement, the idempotency/testing requirement, and the review-only constraints. The prompt itself was not exported.

## 60. Relevance Checker Determination

`SANITIZED_RELEVANCE_CHECK_FALSE_NEGATIVE`

The already persisted output was checked offline using Chinese keywords, English synonyms, structure checks, and manually explainable semantic rules. It covers transaction boundary or transaction handling; inventory concurrency, locking, atomic decrement, or over-borrow control; and idempotency, duplicate-request protection, or testing advice. It is review-only, contains no full implementation, contains no real data, and has exactly three items. The earlier generatedContentRelated=false result is therefore treated as a false negative, not a provider compatibility or persistence failure.

## 61. Final Provider Integration Verdict

`SYNTHETIC_REAL_PROVIDER_VALIDATION_PASSED`

The final provider integration is validated for the synthetic single Chat Completions path: one current-round external request, three historical real requests total, no GET /models, fallback disabled and unused, HTTP 200 retained, response body received, JSON parsed, non-empty related output persisted, token counts parsed, and the DevFlow Generation Record / Trace / Run / Step chain persisted. This does not claim production availability, long-term provider stability, SLA characteristics, or broad model compatibility.

Recommended next action: `READY_TO_COMMIT_AND_FREEZE`.
