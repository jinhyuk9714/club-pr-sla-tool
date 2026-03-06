# Local Setup Runbook

## 목표

로컬에서 설치형 GitHub App 흐름을 끝까지 검증합니다.

1. 앱 실행
2. GitHub 로그인
3. GitHub App 설치
4. installation 설정 페이지에서 Discord webhook 저장
5. PR `ready_for_review` 이후 Discord 알림과 GitHub Check 상태 확인

## Prerequisites

- Docker / Docker Compose
- JDK 21
- GitHub App을 만들 수 있는 GitHub 계정
- 테스트용 Discord webhook
- public tunnel 도구 하나
  - 예: `ngrok`, `cloudflared`

## 1. 환경 변수 준비

1. `cp .env.example .env`
2. `.env`에 아래 값을 채웁니다.
   - `GITHUB_WEBHOOK_SECRET`
   - `GITHUB_APP_APP_ID`
   - `GITHUB_APP_PRIVATE_KEY`
   - `GITHUB_APP_CLIENT_ID`
   - `GITHUB_APP_CLIENT_SECRET`
   - `GITHUB_APP_INSTALL_URL`
   - `APP_SECURITY_ENCRYPTION_SECRET`
   - `OPS_ADMIN_API_KEY`
   - `BETA_ALLOWED_GITHUB_LOGINS` 또는 `BETA_ALLOWED_GITHUB_ACCOUNTS`
3. 선택값
   - `SLA_SCANNER_INTERVAL_MS`
   - `SLA_SCANNER_INITIAL_DELAY_MS`
   - `OUTBOUND_JOBS_INTERVAL_MS`
   - `SLA_FALLBACK_36H_ENABLED`
   - `OPS_SECURITY_RATE_LIMIT_MAX_REQUESTS`
   - `OPS_SECURITY_RATE_LIMIT_WINDOW_SECONDS`

## 2. GitHub App 생성

GitHub Developer Settings에서 App을 만듭니다.

- Homepage URL: `https://<public-host>/`
- Callback URL: `https://<public-host>/auth/github/callback`
- Setup URL: `https://<public-host>/app/installations/setup`
- Webhook URL: `https://<public-host>/api/webhooks/github`
- Webhook secret: `.env`의 `GITHUB_WEBHOOK_SECRET`
- Permissions
  - `Pull requests`: Read-only
  - `Checks`: Read and write
  - `Metadata`: Read-only
- Subscribe to events
  - `Pull request`
  - `Pull request review`
  - `Installation`
  - `Installation repositories`

App 생성 후 아래 값을 `.env`와 맞춥니다.

- App ID -> `GITHUB_APP_APP_ID`
- Client ID -> `GITHUB_APP_CLIENT_ID`
- Client Secret -> `GITHUB_APP_CLIENT_SECRET`
- Private Key PEM -> `GITHUB_APP_PRIVATE_KEY`
- Install URL -> `GITHUB_APP_INSTALL_URL`

## 3. 로컬 앱 실행

1. `./scripts/dev-up.sh`
2. `./gradlew clean test`
3. `./gradlew bootRun`
4. 별도 터미널에서 tunnel 실행
   - 예: `ngrok http 8080`
5. health check
   - `curl http://localhost:8080/api/health`
   - 기대 결과: `{"status":"UP","database":"UP","migrations":"UP"}`

## 4. 온보딩 smoke test

0. `.env`의 allowlist에 현재 GitHub 로그인 또는 설치 대상 account login이 들어 있는지 확인합니다.
1. 브라우저에서 `https://<public-host>/` 접속
2. `GitHub으로 시작하기` 클릭
3. GitHub 로그인 완료
4. `GitHub App 설치` 클릭
5. 테스트 repository를 선택해서 설치
6. 설치 완료 후 `/app/installations/setup?installation_id=...` 로 복귀 확인
7. installation 설정 페이지에서 Discord webhook 저장
8. 저장 후 `saved=1` 리다이렉트 확인

## 5. PR 동작 smoke test

1. 테스트 repository에서 draft PR 생성
2. PR을 `Ready for review` 로 전환
3. 기대 결과
   - DB에 installation / installation repository / installation settings 저장됨
   - PR 상태가 추적 시작됨
   - GitHub Check 이름 `Club PR SLA` 가 `On track` 로 표시됨
4. 12h 이상 경과한 테스트 데이터를 만들려면 ops API 또는 DB를 사용합니다.
5. 12h 경과 후 scanner 실행 시 기대 결과
   - Discord reminder 발송
   - GitHub Check 상태 `At risk`
6. 24h 경과 후 기대 결과
   - Discord escalation 발송
   - GitHub Check 상태 `Breached`
7. 첫 리뷰 제출 후 기대 결과
   - GitHub Check 상태 `Reviewed`
   - 이후 scanner가 추가 알림을 보내지 않음

## 6. Ops UI 점검

1. 브라우저에서 `http://localhost:8080/ops/login` 접속
2. `OPS_ADMIN_API_KEY` 로 로그인
3. `/ops` 에서 아래 카드가 보이는지 확인
   - active installations
   - configured installations
   - misconfigured installations
   - pending outbound jobs
   - dead outbound jobs
   - pending dead letters
4. `/ops/installations` 에서 방금 설치한 installation row가 보이는지 확인
5. `/ops/deliveries` 에서 최근 Discord / GitHub Check job이 보이는지 확인
6. `/ops/dead-letters` 에서 dead letter가 없거나, 있으면 replay 버튼이 보이는지 확인

## 7. 운영 API 점검

- Dashboard
  - `curl http://localhost:8080/api/repositories/<repositoryId>/dashboard`
- Dead letters
  - `curl -H "X-Admin-Api-Key: ${OPS_ADMIN_API_KEY}" "http://localhost:8080/api/admin/dead-letters?status=PENDING&limit=20"`
- Dead letter replay
  - `curl -X POST -H "X-Admin-Api-Key: ${OPS_ADMIN_API_KEY}" http://localhost:8080/api/admin/dead-letters/<id>/replay`
- Manual reevaluate
  - `curl -X POST -H "X-Admin-Api-Key: ${OPS_ADMIN_API_KEY}" http://localhost:8080/api/repositories/<repositoryId>/pull-requests/<prNumber>/sla/re-evaluate`
- Audit logs
  - `curl -H "X-Admin-Api-Key: ${OPS_ADMIN_API_KEY}" "http://localhost:8080/api/admin/audit-logs?limit=20"`
- Prometheus metrics
  - `curl -H "X-Admin-Api-Key: ${OPS_ADMIN_API_KEY}" http://localhost:8080/actuator/prometheus | grep -E "sla_notifications_total|sla_scan_runs_total|sla_scan_failures_total|ops_auth_failures_total|ops_rate_limited_total|outbound_jobs_created_total|outbound_jobs_succeeded_total|outbound_jobs_dead_total|outbound_job_attempt_failures_total"`

## 8. 빠른 실패 확인

- webhook signature가 틀리면 `POST /api/webhooks/github` 는 `401`
- allowlist에 없는 사용자로 installation 설정 페이지에 들어가면 `403`
- installation 설정 전 PR이 들어오면 Discord 전송 없이 GitHub Check만 `App setup required`
- repository가 installation에서 제거되면 dashboard / scanner / reevaluate 대상에서 빠져야 함

## Shutdown

- `./scripts/dev-down.sh`

## Reset Database

- `./scripts/dev-reset-db.sh`
