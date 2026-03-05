# Club PR SLA Tool

Spring Boot 기반 PR 리뷰 SLA 운영 도구입니다. GitHub PR 이벤트를 받아 12h/24h/36h 기준으로 리마인드/에스컬레이션을 자동화합니다.

## Tech Stack

- Java 21
- Spring Boot 3.x
- Gradle (Wrapper)
- PostgreSQL
- Redis
- Flyway
- JUnit 5 + Testcontainers

## Branch Strategy

- Trunk 기반으로 `master` 브랜치를 기준으로 PR 머지합니다.
- 기능 브랜치는 `codex/*` 또는 `feature/*` 패턴을 사용합니다.
- `master`는 보호 브랜치로 설정하고 CI green일 때만 머지합니다.

## Quick Start (10min)

1. 환경 파일 준비
   - `cp .env.example .env`
2. 로컬 인프라 기동
   - `./scripts/dev-up.sh`
3. 테스트 실행
   - `./gradlew clean test`
4. 앱 실행
   - `./gradlew bootRun`
5. 헬스체크
   - `curl http://localhost:8080/api/health`

## Required Commands

- `./scripts/dev-up.sh`
- `./scripts/dev-down.sh`
- `./scripts/dev-reset-db.sh`
- `./scripts/check.sh`
- `./gradlew test`
- `./gradlew check`
- `./gradlew bootRun`

## Webhook Endpoint (MVP 1)

- `POST /api/webhooks/github`
- Required headers:
  - `X-Hub-Signature-256`
  - `X-GitHub-Delivery`
  - `X-GitHub-Event`
- Configure secret:
  - `GITHUB_WEBHOOK_SECRET` in `.env`

## Dashboard Endpoint (MVP 4)

- `GET /api/repositories/{repositoryId}/dashboard`
- Example:
  - `curl http://localhost:8080/api/repositories/1/dashboard`
- Response:
  - `{"onTrack":0,"atRisk":0,"breached":0}`

## Metrics (MVP 4)

- Exposed via `GET /actuator/prometheus`
- SLA metrics:
  - `sla_notifications_total{stage="REMIND_12H|ESCALATE_24H|FALLBACK_36H"}`
  - `sla_scan_runs_total`
  - `sla_scan_failures_total`
- Ops security metrics:
  - `ops_auth_failures_total{endpoint="..."}`
  - `ops_rate_limited_total{endpoint="..."}`

## Ops APIs (MVP 5)

- Required header for all ops APIs:
  - `X-Admin-Api-Key: <OPS_ADMIN_API_KEY>`

- Dead letter list:
  - `GET /api/admin/dead-letters?status=PENDING&limit=50`
- Dead letter replay:
  - `POST /api/admin/dead-letters/{id}/replay`
  - Status codes:
    - `200` replay success
    - `404` dead letter not found
    - `409` already replayed
    - `422` legacy row without `repoId/prNumber/stage`
    - `502` replay delivery failed
- Admin audit logs:
  - `GET /api/admin/audit-logs?operation=SLA_REEVALUATE&limit=50`
- Manual single-PR reevaluation:
  - `POST /api/repositories/{repositoryId}/pull-requests/{prNumber}/sla/re-evaluate`
  - Status codes:
    - `200` reevaluated
    - `404` PR state not found
- Shared security status codes:
  - `401` missing/invalid `X-Admin-Api-Key`
  - `429` rate limit exceeded (`Retry-After` response header)

## Branch Protection Guide

GitHub 저장소에서 아래 설정을 권장합니다.

- Require a pull request before merging
- Require status checks to pass before merging
- Required checks: `build-and-test`
- Dismiss stale pull request approvals when new commits are pushed

