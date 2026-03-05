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
   - Optional: `SLA_SCANNER_INTERVAL_MS` (default `300000`)
   - Optional: `SLA_SCANNER_INITIAL_DELAY_MS` (default `300000`)
   - Optional: `SLA_FALLBACK_36H_ENABLED` (default `false`)
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

## Shutdown

- `./scripts/dev-down.sh`

## Reset Database

- `./scripts/dev-reset-db.sh`
