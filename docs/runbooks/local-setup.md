# Local Setup Runbook

## 목표

로컬에서 설치형 GitHub App 흐름을 끝까지 검증합니다.

1. 앱 실행
2. GitHub App 설치
3. installation 설정 페이지에서 Discord webhook 저장
4. PR `ready_for_review` 이후 GitHub Check 상태 확인

## Prerequisites

- Docker / Docker Compose
- JDK 21
- GitHub App을 만들 수 있는 GitHub 계정
- 테스트용 Discord webhook
- public tunnel 도구 하나
  - 예: `ngrok`, `cloudflared`

## 1. 환경 변수 준비

1. `cp .env.example .env`
2. `.env` 에 아래 값을 채웁니다.
   - `GITHUB_WEBHOOK_SECRET`
   - `GITHUB_APP_APP_ID`
   - `GITHUB_APP_PRIVATE_KEY`
   - `GITHUB_APP_CLIENT_ID`
   - `GITHUB_APP_CLIENT_SECRET`
   - `GITHUB_APP_INSTALL_URL`
   - `APP_SECURITY_ENCRYPTION_SECRET`
   - `OPS_ADMIN_API_KEY`
   - `BETA_ALLOWED_GITHUB_LOGINS`

## 2. GitHub App 생성

[github-app-checklist.md](github-app-checklist.md) 기준으로 App을 만듭니다.

## 3. 로컬 앱 실행

1. `./scripts/dev-up.sh`
2. `./gradlew clean test`
3. 앱 실행
   - 기본: `./gradlew bootRun`
   - 포트 충돌 시: `SERVER_PORT=8081 ./gradlew bootRun`
4. 별도 터미널에서 tunnel 실행
   - 예: `ngrok http 8080`
   - 포트 변경 시: `ngrok http 8081`
5. health check
   - 기본: `curl http://localhost:8080/api/health`
   - 포트 변경 시: `curl http://localhost:8081/api/health`

`docker-compose.beta.yml` 로 beta 배포 구성을 로컬에서 검증할 때는 아래처럼 별도 env 파일을 씁니다.

```bash
cp .env .env.beta.local
echo "PUBLIC_HOSTNAME=localhost" >> .env.beta.local
echo "BETA_ENV_FILE=.env.beta.local" >> .env.beta.local
docker compose --env-file .env.beta.local -f docker-compose.beta.yml config -q
```

## 4. 온보딩 smoke test

1. 브라우저에서 `https://<public-host>/` 접속
2. `GitHub App 설치` 클릭
3. 테스트 repository를 선택해서 설치
4. 설치 완료 후 `/app/installations/setup?installation_id=...` 로 복귀 확인
5. installation 설정 페이지에서 Discord webhook 저장
6. 홈으로 돌아가더라도 설치를 끝낸 사용자는 `설치 후 설정 계속하기` 에 installation ID를 넣어 다시 들어갈 수 있어야 합니다

## 5. PR 동작 smoke test

1. 테스트 repository에서 draft PR 생성
2. PR을 `Ready for review` 로 전환
3. 기대 결과
   - PR 상태가 추적 시작됨
   - GitHub Check 이름 `Club PR SLA` 가 `On track` 로 표시됨

## 6. Ops UI 점검

1. 브라우저에서 `http://localhost:8080/ops/login` 접속
2. 포트 변경 시 `http://localhost:8081/ops/login` 접속
3. `OPS_ADMIN_API_KEY` 로 로그인
4. `/ops` 확인
5. `/ops/installations` 확인
6. `/ops/deliveries` 확인
7. `/ops/dead-letters` 확인

## 7. 운영 API 점검

- Dashboard
  - `curl http://localhost:<port>/api/repositories/<repositoryId>/dashboard`
- Dead letters
  - `curl -H "X-Admin-Api-Key: ${OPS_ADMIN_API_KEY}" "http://localhost:<port>/api/admin/dead-letters?status=PENDING&limit=20"`
- Dead letter replay
  - `curl -X POST -H "X-Admin-Api-Key: ${OPS_ADMIN_API_KEY}" http://localhost:<port>/api/admin/dead-letters/<id>/replay`
- Manual reevaluate
  - `curl -X POST -H "X-Admin-Api-Key: ${OPS_ADMIN_API_KEY}" http://localhost:<port>/api/repositories/<repositoryId>/pull-requests/<prNumber>/sla/re-evaluate`
- Audit logs
  - `curl -H "X-Admin-Api-Key: ${OPS_ADMIN_API_KEY}" "http://localhost:<port>/api/admin/audit-logs?limit=20"`

## Shutdown

- `./scripts/dev-down.sh`

## Reset Database

- `./scripts/dev-reset-db.sh`
