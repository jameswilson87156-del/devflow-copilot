# Trace Freeze Known Issues

## Local Demo API data limits

- The existing public local API safely generated multiple Local Demo Runs with `WAITING_REVIEW`, `SAVED`, and `CONFIRMED` states, Tool Calls, Knowledge References, and a real `HUMAN_REVIEW` AgentStep.
- The current API did not expose a safe frontend-only way to create a failed/error AgentRun or to append additional AgentSteps without writing directly to the temporary H2 database.
- The selected formal demo Run contains 5 real AgentSteps from the backend workflow. Phase 1.2 did not fabricate extra steps in the frontend, because the Trace page must render only real `trace.steps`.

Decision: keep the real API output and document the limitation instead of faking failure states or synthetic steps.
