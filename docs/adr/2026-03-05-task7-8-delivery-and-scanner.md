# ADR: Task 7~8 Discord Delivery and SLA Scanner

## Status

- Accepted (2026-03-05)

## Context

- SLA core(Task 5~6) is complete with `SlaEngine` and deduplicated `sla_event_logs`.
- We need outbound delivery and periodic evaluation wiring for MVP.
- Duplicate scanner execution must be prevented in multi-instance deployment.

## Decision

### 1. Lock strategy

- Use PostgreSQL advisory lock with `pg_try_advisory_lock` and `pg_advisory_unlock`.
- Lock name is fixed as `sla-scan`.
- Lock key is derived from a stable hash of the lock name.

### 2. Notification delivery failure handling

- `DiscordNotificationAdapter` implements `NotificationPort`.
- Delivery retries immediately up to 3 attempts.
- If all attempts fail, store a dead letter row in `dead_letters` and rethrow.
- Scanner catches per-PR dispatch failure and continues with next PR.

### 3. SLA event log write timing

- `sla_event_logs` are written only after successful `NotificationPort.send`.
- Failed outbound delivery does not consume the stage, allowing future retries.

### 4. Scanner policy

- Schedule uses fixed delay: `sla.scanner.interval-ms` (default `300000`).
- Scan target is PRs that are `READY`, `readyAt != null`, `firstReviewAt == null`.
- Per PR flow:
  - Load sent stages from `sla_event_logs`.
  - Evaluate via `SlaEngine`.
  - Dispatch only when evaluation returns action.

## Consequences

- Pros:
  - No extra lock infrastructure dependency.
  - Failed notifications are auditable via dead letters.
  - Scanner remains resilient to partial failures.
- Cons:
  - Advisory lock requires careful connection lifecycle handling.
  - Dead letters require later operational replay policy (out of current scope).
