# Club PR SLA Tool

설치형 GitHub App 기반 PR 리뷰 SLA 서비스입니다. 현재 운영 모델은 `single VPS + Docker Compose + Caddy + Postgres` 기준의 private beta이며, 사용자는 서비스 홈에서 GitHub App을 설치하고 installation별 Discord webhook 한 개만 저장하면 바로 PR 리뷰 지연 추적을 시작할 수 있습니다.

## 사용자 흐름

1. 서비스 홈 `/` 접속
2. `GitHub App 설치`
3. GitHub App `setup_url` 로 `/app/installations/setup?installation_id=...` 복귀
4. installation 설정 페이지 `/app/installations/{installationId}` 에서 Discord webhook 저장
5. 이후 `ready_for_review` PR부터 SLA 추적, Discord 알림, GitHub Check 상태 갱신

이미 설치를 끝낸 사용자는 홈의 `설치 후 설정 계속하기` 에 installation ID를 넣어 설정 화면으로 다시 진입할 수 있습니다.

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

## GitHub App 설정

- Homepage URL: `https://<your-host>/`
- Callback URL: `https://<your-host>/auth/github/callback`
- Setup URL: `https://<your-host>/app/installations/setup`
- Webhook URL: `https://<your-host>/api/webhooks/github`
- Permissions
  - `Pull requests`: Read-only
  - `Checks`: Read and write
  - `Metadata`: Read-only
- Subscribe to events
  - `Pull request`
  - `Pull request review`
  - `Installation`
  - `Installation repositories`

자세한 체크리스트는 [docs/runbooks/github-app-checklist.md](docs/runbooks/github-app-checklist.md) 를 참고하세요.

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
- `BETA_ALLOWED_GITHUB_LOGINS`

선택:

- `BETA_ALLOWED_GITHUB_ACCOUNTS`
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
3. 로컬 인프라 실행
   - `./scripts/dev-up.sh`
4. 테스트
   - `./gradlew clean test`
5. 앱 실행
   - 기본: `./gradlew bootRun`
   - 포트 충돌 시: `SERVER_PORT=8081 ./gradlew bootRun`
6. 헬스체크
   - 기본: `curl http://localhost:8080/api/health`
   - 포트 변경 시: `curl http://localhost:8081/api/health`

실제 GitHub App 설치와 webhook 수신까지 로컬에서 검증하려면 `ngrok` 같은 public tunnel이 필요합니다. 자세한 절차는 [docs/runbooks/local-setup.md](docs/runbooks/local-setup.md) 를 참고하세요.

## VPS 배포

private beta 배포는 `docker compose` 기준으로 고정합니다.
`docker-compose.beta.yml` 은 `BETA_ENV_FILE` 값을 보고 앱 컨테이너에 주입할 env 파일을 결정합니다. 운영 기본값은 `.env.beta` 입니다.

1. 배포용 env 작성
   - `cp .env.beta.example .env.beta`
2. 이미지/서비스 기동
   - `docker compose --env-file .env.beta -f docker-compose.beta.yml up -d --build`
3. health 확인
   - `curl https://<your-host>/api/health`

자세한 절차는 아래 문서를 참고하세요.

- 배포: [docs/runbooks/private-beta-launch.md](docs/runbooks/private-beta-launch.md)
- VPS 준비: [docs/runbooks/vps-bootstrap.md](docs/runbooks/vps-bootstrap.md)
- 디자인 파트너 온보딩: [docs/runbooks/design-partner-onboarding-checklist.md](docs/runbooks/design-partner-onboarding-checklist.md)
- 운영 점검: [docs/runbooks/operator-daily-check.md](docs/runbooks/operator-daily-check.md)
- 장애 복구: [docs/runbooks/incident-triage-replay.md](docs/runbooks/incident-triage-replay.md)

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

- `./gradlew clean test`
- `./scripts/check.sh`
