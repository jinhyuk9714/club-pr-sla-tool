# Local Setup Runbook

## Prerequisites

- Docker / Docker Compose
- JDK 21

## Setup

1. `cp .env.example .env`
2. `./scripts/dev-up.sh`
3. `./gradlew clean test`
4. `./gradlew bootRun`
5. Validate with `curl http://localhost:8080/api/health`
6. (Webhook local test) call:
   - `curl -i -X POST http://localhost:8080/api/webhooks/github \\`
   - `  -H "X-Hub-Signature-256: sha256=dummy" \\`
   - `  -H "X-GitHub-Delivery: local-delivery-1" \\`
   - `  -H "X-GitHub-Event: pull_request" \\`
   - `  -H "Content-Type: application/json" \\`
   - `  -d '{"action":"ready_for_review","repository":{"id":1},"pull_request":{"number":1,"user":{"login":"alice"}}}'`
   - Expected with invalid signature: `401`

## Shutdown

- `./scripts/dev-down.sh`

## Reset Database

- `./scripts/dev-reset-db.sh`
