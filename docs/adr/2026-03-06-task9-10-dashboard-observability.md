# ADR: Task 9~10 Dashboard and Observability

## Status

- Accepted (2026-03-06)

## Context

- SLA core and scanner are in place, but operators cannot view current SLA health by repository.
- Prometheus endpoint exists, but SLA-specific counters for scanner/notification outcomes were not exposed.
- Need end-to-end verification for reminder/escalation/review-stop lifecycle.

## Decision

### 1. Dashboard scope

- Add `GET /api/repositories/{repositoryId}/dashboard`.
- Response model: `DashboardSummaryDto(onTrack, atRisk, breached)`.
- Classification rule:
  - `firstReviewAt != null`: `onTrack`
  - elapsed `<12h`: `onTrack`
  - elapsed `>=12h && <24h`: `atRisk`
  - elapsed `>=24h`: `breached`
- Scope is repository-level only; workspace model is out of scope for this phase.

### 2. Metrics scope

- Add three counters:
  - `sla_notifications_total{stage}`
  - `sla_scan_runs_total`
  - `sla_scan_failures_total`
- Notification counter increments only after successful `NotificationPort.send`.
- Scan run counter increments once when lock is acquired and scan starts.
- Scan failure counter increments on top-level scan exception.

### 3. E2E strategy

- Add Spring Boot + Testcontainers E2E flow test.
- Replace external notification sender with in-memory `NotificationPort` test bean.
- Validate sequence: ready(13h) -> remind -> dedup -> ready(25h) -> escalate -> review -> stop.

## Consequences

- Pros:
  - Admin visibility for repository SLA state.
  - Actionable scanner/notification metrics in Prometheus.
  - Regression safety for core SLA lifecycle through E2E coverage.
- Cons:
  - Dashboard currently lacks workspace/team aggregation.
  - E2E test runtime increases due containerized integration path.
