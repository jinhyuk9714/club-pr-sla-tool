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

- Trunk 기반으로 `main` 브랜치를 기준으로 PR 머지합니다.
- 기능 브랜치는 `codex/*` 또는 `feature/*` 패턴을 사용합니다.
- `main`은 보호 브랜치로 설정하고 CI green일 때만 머지합니다.

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

## Branch Protection Guide

GitHub 저장소에서 아래 설정을 권장합니다.

- Require a pull request before merging
- Require status checks to pass before merging
- Required checks: `build-and-test`
- Dismiss stale pull request approvals when new commits are pushed
