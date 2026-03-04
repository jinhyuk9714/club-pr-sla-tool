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

## Shutdown

- `./scripts/dev-down.sh`

## Reset Database

- `./scripts/dev-reset-db.sh`
