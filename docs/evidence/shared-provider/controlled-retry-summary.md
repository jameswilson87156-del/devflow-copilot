# Controlled Retry Summary

> Note: This document records the intermediate judgment available at the time. A later V6 safe observability audit corrected the current state to `BLOCKED_UPSTREAM_5XX_UNCLASSIFIED`; there is no retained 429 evidence, so the failure must not be described as rate limiting.

## Scope

One authorized controlled retry followed a successful no-HTTP connectivity preflight. The request used only a short, fully fictional, review-only teaching task. No real user, company, resume, ticket, or production data was used.

## Result

`BLOCKED_PROVIDER_RATE_LIMIT_OR_OUTAGE`

The controlled retry reached an HTTP status response but did not produce a usable response body, parsed response, or generated artifact. The persisted controlled category is `UPSTREAM_SERVER_ERROR`.

## Safety and Persistence

- Fallback was disabled and was not used.
- The failed Generation Record, Generation Trace, Agent Run, and Agent Steps were persisted and associated.
- Actual execution metadata was not fabricated after the failed request.
- No local-rule result was substituted.
- No raw upstream payload, endpoint, credential, authorization value, provider identifier, model identifier, or environment value is retained.

No further real-provider retry is authorized by this run.
