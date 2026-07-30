# Failed Real Validation Forensics

## Controlled Result

`SYNTHETIC_REAL_PROVIDER_VALIDATION_FAILED`

Exactly one synthetic real-provider request was attempted in the prior validation. This diagnostic run made no external-provider request and performed no retry.

## Persisted Evidence

- Generation Record ID: 53; status: `FAILED`.
- Generation Trace ID: 45; associated with the failed record; status: `FAILED`.
- Agent Run ID: 45; associated with the failed record; status: `FAILED`.
- Four Agent Steps were persisted: three completed workflow steps and one failed generation step.
- Requested provider/model metadata is present. Actual provider/model metadata is absent. Fallback is false and no fallback reason is present.

## Confirmed Category and Stage

- Controlled failure category: `TIMEOUT`.
- Persisted safe summary: `Provider request timed out.`
- Failure stage: `UNKNOWN_STAGE`.

The safe category proves that external dispatch reached the provider transport path and timed out. It does not safely distinguish connection establishment, TLS negotiation, request transmission, or waiting for a response. No HTTP response, response body, or response parsing evidence is available.

## Offline Reproduction and Repair Decision

Existing loopback tests reproduce timeout classification through the actual provider code path and also cover status failures, malformed responses, empty content, endpoint joining, and connection failure. The complete offline suite passes.

No code defect is confirmed. Runtime configuration binding, protocol support, provider routing, URI validity, and endpoint-join checks are valid without exposing configuration values. The sanitised evidence cannot distinguish a provider-side delay from an intermediate network/TLS/transport delay, so no code or configuration repair is applied.

## Recommended State

`INSUFFICIENT_SANITIZED_EVIDENCE`

No second real-provider call was made. No environment-variable value, credential, endpoint, header, prompt body, response body, or raw exception is retained in this diagnosis.
