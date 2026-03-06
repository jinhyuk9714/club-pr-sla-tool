# ADR: MVP 6 Ops API Security Hardening

## Status

Accepted

## Context

- MVP 5 introduced operational APIs for dead letter replay and manual SLA re-evaluation.
- Those endpoints were intentionally unauthenticated under internal-network trust assumptions.
- Operational risk remained:
  - anyone with network access could call replay/re-evaluate APIs,
  - no app-level throttling for repeated admin calls,
  - no persistent audit trail for operator actions.

## Decision

- Add `X-Admin-Api-Key` authentication for ops endpoints:
  - `GET/POST /api/admin/**`
  - `POST /api/repositories/{repositoryId}/pull-requests/{prNumber}/sla/re-evaluate`
- Keep non-ops endpoints unchanged:
  - `/api/webhooks/**`, dashboard, health, and Prometheus endpoint remain accessible as before.
- Add in-memory rate limiting (single instance):
  - default `60 req / 60 sec` per `apiKey + endpointGroup`.
  - return `429` with `Retry-After` header when exceeded.
- Add ops security metrics:
  - `ops_auth_failures_total{endpoint}`
  - `ops_rate_limited_total{endpoint}`
- Add `admin_audit_logs` persistence and query API:
  - `GET /api/admin/audit-logs`
  - record outcomes for dead-letter list/replay and SLA re-evaluate calls.

## Consequences

- Positive:
  - Immediate protection against unauthenticated ops API invocation.
  - Basic abuse protection and observability for admin request bursts.
  - Operator actions become traceable in database-backed audit logs.
- Negative:
  - API key is a shared secret and not identity-based access control.
  - Rate limiter is process-local and not globally enforced across instances.

## Follow-ups

- Replace shared API key with identity-aware authn/authz (JWT/OAuth2).
- Move rate limit state to distributed store when multi-instance deployment starts.
- Add alerting thresholds over ops security metrics and audit anomalies.
