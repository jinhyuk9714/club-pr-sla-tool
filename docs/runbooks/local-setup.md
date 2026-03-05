# Local Setup Runbook

## Prerequisites

- Docker / Docker Compose
- JDK 21

## Setup

1. `cp .env.example .env`
2. `./scripts/dev-up.sh`
3. Set required environment variables in `.env`:
   - `GITHUB_WEBHOOK_SECRET`
   - `DISCORD_WEBHOOK_URL`
   - `OPS_ADMIN_API_KEY`
   - Optional: `SLA_SCANNER_INTERVAL_MS` (default `300000`)
   - Optional: `SLA_SCANNER_INITIAL_DELAY_MS` (default `300000`)
   - Optional: `SLA_FALLBACK_36H_ENABLED` (default `false`)
   - Optional: `OPS_SECURITY_RATE_LIMIT_MAX_REQUESTS` (default `60`)
   - Optional: `OPS_SECURITY_RATE_LIMIT_WINDOW_SECONDS` (default `60`)
4. `./gradlew clean test`
5. `./gradlew bootRun`
6. Validate with `curl http://localhost:8080/api/health`
7. (Webhook local test) call:
   - `curl -i -X POST http://localhost:8080/api/webhooks/github \\`
   - `  -H "X-Hub-Signature-256: sha256=dummy" \\`
   - `  -H "X-GitHub-Delivery: local-delivery-1" \\`
   - `  -H "X-GitHub-Event: pull_request" \\`
   - `  -H "Content-Type: application/json" \\`
   - `  -d '{"action":"ready_for_review","repository":{"id":1},"pull_request":{"number":1,"user":{"login":"alice"}}}'`
   - Expected with invalid signature: `401`
8. (Scanner local check) lower `SLA_SCANNER_INTERVAL_MS` to `10000` and verify logs every 10 seconds.
9. (Dashboard API check):
   - `curl http://localhost:8080/api/repositories/1/dashboard`
10. (Prometheus metrics check):
   - `curl http://localhost:8080/actuator/prometheus | grep -E "sla_notifications_total|sla_scan_runs_total|sla_scan_failures_total"`
11. (Dead letter admin check):
   - `curl -H "X-Admin-Api-Key: ${OPS_ADMIN_API_KEY}" "http://localhost:8080/api/admin/dead-letters?status=PENDING&limit=20"`
   - `curl -X POST -H "X-Admin-Api-Key: ${OPS_ADMIN_API_KEY}" http://localhost:8080/api/admin/dead-letters/{id}/replay`
12. (Manual SLA re-evaluate check):
   - `curl -X POST -H "X-Admin-Api-Key: ${OPS_ADMIN_API_KEY}" http://localhost:8080/api/repositories/{repositoryId}/pull-requests/{prNumber}/sla/re-evaluate`
13. (Admin audit logs check):
   - `curl -H "X-Admin-Api-Key: ${OPS_ADMIN_API_KEY}" "http://localhost:8080/api/admin/audit-logs?limit=20"`
14. (Ops security metrics check):
   - `curl http://localhost:8080/actuator/prometheus | grep -E "ops_auth_failures_total|ops_rate_limited_total"`
15. (Rate limit quick validation):
   - `for i in {1..65}; do curl -s -o /dev/null -w "%{http_code}\n" -H "X-Admin-Api-Key: ${OPS_ADMIN_API_KEY}" "http://localhost:8080/api/admin/dead-letters?status=PENDING&limit=1"; done`
   - Expects `429` when request count exceeds configured window.

## Shutdown

- `./scripts/dev-down.sh`

## Reset Database

- `./scripts/dev-reset-db.sh`
