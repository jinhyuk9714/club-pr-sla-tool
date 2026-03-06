# ADR: Installable GitHub App V1

## Status

Accepted

## Context

- 기존 프로젝트는 webhook ingestion, SLA scanner, dashboard, ops API까지는 있었지만 사용자가 어떻게 시작해야 하는지 제품 입구가 없었다.
- `DISCORD_WEBHOOK_URL` 단일 전역 설정만으로는 여러 repository / installation을 한 서비스에서 안전하게 운영하기 어려웠다.
- 목표는 공용 서비스 한 대에 GitHub App을 올려 두고, 사용자는 App 설치와 Discord 연결만으로 쓰게 만드는 것이다.

## Decision

- 제품 형태를 `설치형 GitHub App`으로 전환한다.
- 서버 렌더 기반 온보딩 UI를 추가한다.
  - `GET /`
  - `GET /login/github`
  - `GET /auth/github/callback`
  - `GET /app/installations/setup`
  - `GET /app/installations/{installationId}`
  - `POST /app/installations/{installationId}/settings`
- installation 단위 저장 모델을 도입한다.
  - `github_installations`
  - `github_installation_repository_entries`
  - `installation_settings`
- Discord webhook은 installation 단위로 저장하고 DB에는 암호화된 값만 남긴다.
- SLA 추적은 `활성 installation repository + configured installation settings` 조합에서만 시작한다.
- GitHub App 상태 표시는 PR 코멘트 대신 고정된 Check 이름 `Club PR SLA` 로 제공한다.

## Consequences

- 장점
  - 사용자는 코드와 인프라를 이해하지 않고도 설치 흐름으로 시작할 수 있다.
  - 하나의 서비스 인스턴스로 여러 GitHub repository를 분리된 installation 단위로 운영할 수 있다.
  - Discord webhook과 추적 범위가 installation 단위로 묶여 운영 경계가 분명해진다.
- 단점
  - GitHub OAuth, App JWT, installation token, setup callback까지 운영해야 할 비밀정보와 외부 연동이 늘어난다.
  - GitHub Check 동기화 실패는 별도 재시도 없이 best-effort로 처리되므로 가시성 보강이 추가로 필요하다.
  - Slack, repository별 세부 SLA, 조직 단위 대시보드는 아직 범위 밖이다.
