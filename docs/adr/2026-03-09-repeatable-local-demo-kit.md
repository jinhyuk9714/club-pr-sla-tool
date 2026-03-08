# ADR: Repeatable Local Demo Kit

## Context

- private beta 전 실제 운영 배포는 아직 막혀 있다.
- 대신 로컬 머신과 `ngrok` 으로 반복 데모를 자주 다시 띄워야 한다.
- 지금까지는 포트 충돌, 앱 실행, tunnel 발급, GitHub App URL 갱신, 종료 절차가 모두 수동이었다.

## Decision

- 로컬 데모 운영 경로를 `scripts/demo-up.sh`, `scripts/demo-status.sh`, `scripts/demo-down.sh` 로 고정한다.
- `docker-compose.yml` 의 Postgres host port는 `POSTGRES_PORT` env 로 덮어쓸 수 있게 한다.
- 데모 세션 상태는 repo 내부의 추적 제외 디렉터리 `.demo/` 에 저장한다.
- 제품 기능과 공개 API는 바꾸지 않고, 데모 운영 절차만 자동화한다.

## Consequences

- 운영자는 10분 안에 데모 세션을 다시 띄울 수 있다.
- 포트 충돌이 있어도 `5432 -> 5433`, `8080 -> 8081` 정책으로 회피할 수 있다.
- `ngrok` URL과 GitHub App URL 4개를 스크립트 출력으로 바로 복사할 수 있다.
- 이 경로는 임시 데모용이며, 안정적인 외부 운영 배포 계약을 대체하지 않는다.
