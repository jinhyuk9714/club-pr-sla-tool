# ADR: MVP 5 Ops Stabilization Scope Lock

## Status

Accepted

## Context

- MVP 1~4 are merged on `master` and running with webhook ingestion, SLA scanner, dashboard summary, and metrics.
- Documentation still had mixed references for branch naming (`main` vs `master`) and dashboard scope (`workspace` vs `repository`).
- Operations currently had dead letter persistence, but no retrieval/replay flow and no manual single-PR re-evaluation path.

## Decision

- Lock MVP 5 scope to repository-level operations only.
- Standardize docs to `master` branch naming and `GET /api/repositories/{repositoryId}/dashboard`.
- Add operational APIs:
  - `GET /api/admin/dead-letters`
  - `POST /api/admin/dead-letters/{id}/replay`
  - `POST /api/repositories/{repositoryId}/pull-requests/{prNumber}/sla/re-evaluate`
- Keep admin APIs unauthenticated for now under internal-network trust assumptions.

## Consequences

- Positive:
  - Operators can recover failed notifications without DB-level manual intervention.
  - Operators can force SLA re-evaluation for a single PR without waiting for scanner interval.
  - Documentation and implementation stay aligned for branch strategy and dashboard scope.
- Negative:
  - Workspace/team aggregate operations remain unavailable in this phase.
  - API authn/authz must be handled by internal network controls until a later security phase.
