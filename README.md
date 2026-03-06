# Club PR SLA Tool

설치형 GitHub App 기반 PR 리뷰 SLA 서비스입니다. 현재 운영 계약은 `single app container + Postgres` 기준의 private beta이며, 사용자는 서비스 홈에서 GitHub 로그인 후 App을 설치하고 installation별 Discord webhook 한 개만 저장하면 바로 PR 리뷰 지연 추적을 시작할 수 있습니다.

## 사용자 흐름

1. 서비스 홈 `/` 접속
2. `GitHub으로 시작하기`로 로그인
3. GitHub App 설치
4. GitHub App `setup_url`로 `/app/installations/setup?installation_id=...` 복귀
5. 설치별 설정 페이지 `/app/installations/{installationId}` 에서 Discord webhook 저장
6. 이후 `ready_for_review` PR부터 SLA 추적, Discord 알림, GitHub Check 상태 갱신

## 현재 제공 기능

- GitHub App installation / installation_repositories webhook 수집
- installation별 활성 repository 동기화
- installation별 Discord webhook 암호화 저장
- PR `ready_for_review`, `converted_to_draft`, `pull_request_review.submitted` 추적
- 12h reminder / 24h escalation / optional 36h fallback SLA 스캐너
- GitHub Check 상태 동기화
  - `On track`
  - `At risk`
  - `Breached`
  - `Reviewed`
  - `App setup required`
- 운영 API
  - dead letter 조회 / 재전송
  - 단일 PR SLA 재평가
  - audit log 조회
- 내부 ops UI
  - installation 상태 조회
  - outbound delivery queue 조회
  - dead letter replay
- outbound delivery DB 작업 큐
  - `DISCORD_NOTIFICATION`, `GITHUB_CHECK_SYNC`
  - retry backoff `1m -> 5m -> 15m`
  - 3회 실패 후 dead letter 전환

## Private Beta 계약

- 배포 모델은 `앱 1개 컨테이너 + Postgres` 로 고정합니다.
- beta 사용자 제어는 환경 변수 allowlist로만 합니다.
  - `BETA_ALLOWED_GITHUB_LOGINS`
  - `BETA_ALLOWED_GITHUB_ACCOUNTS`
- 외부 전송은 직접 호출하지 않고 `outbound_delivery_jobs` 테이블에 적재한 뒤 워커가 비동기로 처리합니다.
- Prometheus 메트릭은 공개하지 않으며 `X-Admin-Api-Key` 헤더가 있어야 `/actuator/prometheus` 에 접근할 수 있습니다.

## GitHub App 설정

GitHub App을 직접 만들어 이 서비스에 연결해야 합니다.

- Homepage URL: `https://<your-host>/`
- Callback URL: `https://<your-host>/auth/github/callback`
- Setup URL: `https://<your-host>/app/installations/setup`
- Webhook URL: `https://<your-host>/api/webhooks/github`
- Permissions:
  - `Pull requests`: Read-only
  - `Checks`: Read and write
  - `Metadata`: Read-only
- Subscribe to events:
  - `Pull request`
  - `Pull request review`
  - `Installation`
  - `Installation repositories`

자세한 체크리스트는 [docs/runbooks/github-app-checklist.md](/Users/sungjh/club-pr-tool/.worktrees/codex-installable-github-app-v1/docs/runbooks/github-app-checklist.md) 를 참고하세요.

## 필수 환경 변수

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `GITHUB_WEBHOOK_SECRET`
- `GITHUB_APP_APP_ID`
- `GITHUB_APP_PRIVATE_KEY`
- `GITHUB_APP_CLIENT_ID`
- `GITHUB_APP_CLIENT_SECRET`
- `GITHUB_APP_INSTALL_URL`
- `APP_SECURITY_ENCRYPTION_SECRET`
- `OPS_ADMIN_API_KEY`
- `BETA_ALLOWED_GITHUB_LOGINS` 또는 `BETA_ALLOWED_GITHUB_ACCOUNTS`

## 선택 환경 변수

- `SLA_SCANNER_INTERVAL_MS`
- `SLA_SCANNER_INITIAL_DELAY_MS`
- `OUTBOUND_JOBS_INTERVAL_MS`
- `SLA_FALLBACK_36H_ENABLED`
- `OPS_SECURITY_RATE_LIMIT_MAX_REQUESTS`
- `OPS_SECURITY_RATE_LIMIT_WINDOW_SECONDS`

## 로컬 실행

1. `.env` 생성
   - `cp .env.example .env`
2. 필수 값 설정
   - `GITHUB_WEBHOOK_SECRET`
   - `GITHUB_APP_APP_ID`
   - `GITHUB_APP_PRIVATE_KEY`
   - `GITHUB_APP_CLIENT_ID`
   - `GITHUB_APP_CLIENT_SECRET`
   - `GITHUB_APP_INSTALL_URL`
   - `APP_SECURITY_ENCRYPTION_SECRET`
   - `OPS_ADMIN_API_KEY`
   - `BETA_ALLOWED_GITHUB_LOGINS` 또는 `BETA_ALLOWED_GITHUB_ACCOUNTS`
3. 로컬 인프라 실행
   - `./scripts/dev-up.sh`
4. 테스트
   - `./gradlew clean test`
5. 앱 실행
   - `./gradlew bootRun`
6. 헬스체크
   - `curl http://localhost:8080/api/health`

실제 GitHub App 설치와 webhook 수신까지 로컬에서 검증하려면 `ngrok` 같은 public tunnel이 필요합니다. 자세한 절차는 [docs/runbooks/local-setup.md](/Users/sungjh/club-pr-tool/.worktrees/codex-installable-github-app-v1/docs/runbooks/local-setup.md) 를 참고하세요.
디자인 파트너 온보딩과 운영 점검 절차는 [docs/runbooks/design-partner-onboarding-checklist.md](/Users/sungjh/club-pr-tool/.worktrees/codex-installable-github-app-v1/docs/runbooks/design-partner-onboarding-checklist.md), [docs/runbooks/operator-daily-check.md](/Users/sungjh/club-pr-tool/.worktrees/codex-installable-github-app-v1/docs/runbooks/operator-daily-check.md), [docs/runbooks/incident-triage-replay.md](/Users/sungjh/club-pr-tool/.worktrees/codex-installable-github-app-v1/docs/runbooks/incident-triage-replay.md) 에 정리했습니다.

## Docker 이미지

- 이미지 빌드
  - `docker build -t club-pr-sla:beta .`
- 컨테이너 실행
  - `docker run --env-file .env -p 8080:8080 club-pr-sla:beta`

실제 private beta 배포 절차는 [docs/runbooks/private-beta-launch.md](/Users/sungjh/club-pr-tool/.worktrees/codex-installable-github-app-v1/docs/runbooks/private-beta-launch.md) 에 정리했습니다.

## 주요 라우트

- 사용자 UI
  - `GET /`
  - `GET /login/github`
  - `GET /auth/github/callback`
  - `GET /app/installations/setup`
  - `GET /app/installations/{installationId}`
  - `POST /app/installations/{installationId}/settings`
- GitHub webhook
  - `POST /api/webhooks/github`
- Dashboard
  - `GET /api/repositories/{repositoryId}/dashboard`
- Ops UI
  - `GET /ops/login`
  - `POST /ops/login`
  - `POST /ops/logout`
  - `GET /ops`
  - `GET /ops/installations`
  - `GET /ops/deliveries`
  - `GET /ops/dead-letters`
  - `POST /ops/dead-letters/{id}/replay`
- Ops API
  - `GET /api/admin/dead-letters`
  - `POST /api/admin/dead-letters/{id}/replay`
  - `POST /api/repositories/{repositoryId}/pull-requests/{prNumber}/sla/re-evaluate`
  - `GET /api/admin/audit-logs`

## 운영 메모

- Discord webhook은 installation 단위로 저장되며 DB에는 암호화된 값만 남습니다.
- installation이 비설정 상태면 PR 이벤트는 수집되더라도 Discord 알림과 SLA 추적이 시작되지 않고 GitHub Check에 설정 필요 상태만 표시합니다.
- repository가 installation에서 제거되면 해당 repository는 스캔/대시보드/수동 재평가 대상에서 제외됩니다.
- `/api/health` 는 DB 연결과 Flyway migration 상태를 기준으로 `200` 또는 `503` 을 반환합니다.
- `/actuator/prometheus` 는 `X-Admin-Api-Key` 헤더가 필요합니다.
- `/ops/**` 는 별도 ops 세션이 필요하고, 세션은 `/ops/login` 에서 `OPS_ADMIN_API_KEY` 로 엽니다.

## 검증 명령

- `./gradlew test`
- `./scripts/check.sh`

