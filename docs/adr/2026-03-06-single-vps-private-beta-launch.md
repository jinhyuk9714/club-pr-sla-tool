# ADR: Single-VPS Private Beta Launch Contract

## Status

Accepted

## Context

- 설치형 GitHub App 기능은 구현 완료됐지만 실제 private beta 운영 계약은 아직 느슨했다.
- 홈의 `GitHub으로 시작하기` 는 로그인 후 다시 홈으로 돌아와 사용자에게 no-op처럼 보였다.
- README와 runbook에는 worktree 절대 경로 링크와 로컬 포트 가정이 남아 있었다.

## Decision

- private beta 배포 모델은 `single VPS + Docker Compose + Caddy + Postgres` 로 고정한다.
- 배포용 자산은 `docker-compose.beta.yml`, `Caddyfile`, `.env.beta.example` 을 기준으로 운영한다.
- 홈 `/` 는 `GitHub App 설치` 와 `설치 후 설정 계속하기` 동선으로 단순화한다.
- setup/callback 실패는 빈 화면이나 기본 에러 페이지 대신 명시적 onboarding error 화면으로 안내한다.
- beta 운영자는 `BETA_ALLOWED_GITHUB_LOGINS` 기반으로 1개 팀 파일럿을 먼저 연다.

## Consequences

- private beta 운영 절차가 repo 안의 runbook만으로 재현 가능해진다.
- HTTPS termination과 공개 진입점이 Caddy로 표준화된다.
- 사용자는 홈에서 no-op 로그인 흐름 대신 설치/재진입 흐름을 명확히 이해할 수 있다.
- self-serve public onboarding, multi-instance, managed queue는 이번 단계 범위 밖으로 유지된다.
