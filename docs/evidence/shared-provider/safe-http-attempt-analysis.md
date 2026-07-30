# Safe HTTP Attempt Analysis

> Note: This document records the intermediate judgment available at the time. A later V6 safe observability audit corrected the current state to `BLOCKED_UPSTREAM_5XX_UNCLASSIFIED`; there is no retained 429 evidence, so the failure must not be described as rate limiting.

1. **Confirmed stage:** `HTTP_STATUS_RECEIVED`. The controlled retry reached the HTTP-status handling path.
2. **HTTP status code:** not captured. The existing persisted evidence does not retain an exact numeric status.
3. **Response body:** no usable body is available in the retained evidence.
4. **JSON parsing:** not started; no usable body reached the response parser.
5. **Classifier:** the ProviderErrorClassifier was triggered through the HTTP response-exception path.
6. **Classification basis:** controlled exception type and status-family mapping, not exception-message text.
7. **Redaction limit:** exact status, content type, response length, retry-after presence, upstream request identifier presence, and request duration were not safely persisted. This prevents distinguishing individual server-error statuses.
8. **Rate-limit evidence:** there is currently insufficient evidence to classify this failure as `RATE_LIMITED`; no controlled 429 evidence is retained.
9. **Most accurate block:** `BLOCKED_PROVIDER_RATE_LIMIT_OR_OUTAGE`, based on the retained server-error family. This status does not identify a specific upstream cause.
10. **Safe observability to add before a future authorized call:** exact numeric status; status family; content-type presence/category; body-presence and length bucket; retry-after presence; upstream request-identifier presence; controlled exception category; failure stage; and duration bucket. Persist only these bounded fields, never header values, endpoints, credentials, request bodies, or response bodies.
