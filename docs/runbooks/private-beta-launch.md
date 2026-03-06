# Private Beta Launch Runbook

## 목표

설치형 GitHub App 서비스를 `single VPS + Docker Compose + Caddy + Postgres` 기준으로 배포하고, 1개 팀 파일럿이 실제로 온보딩을 끝낼 수 있는 상태를 만듭니다.

## 1. VPS 준비

먼저 [vps-bootstrap.md](vps-bootstrap.md) 절차를 완료합니다.

준비물이 끝나면 아래 파일이 있어야 합니다.

- `docker-compose.beta.yml`
- `Caddyfile`
- `.env.beta`

## 2. `.env.beta` 작성

```bash
cp .env.beta.example .env.beta
```

`.env.beta` 안의 `BETA_ENV_FILE=.env.beta` 값은 그대로 둡니다.

필수로 채워야 하는 값:

- `PUBLIC_HOSTNAME`
- `GITHUB_WEBHOOK_SECRET`
- `GITHUB_APP_APP_ID`
- `GITHUB_APP_PRIVATE_KEY`
- `GITHUB_APP_CLIENT_ID`
- `GITHUB_APP_CLIENT_SECRET`
- `GITHUB_APP_INSTALL_URL`
- `APP_SECURITY_ENCRYPTION_SECRET`
- `OPS_ADMIN_API_KEY`
- `BETA_ALLOWED_GITHUB_LOGINS`

운영 기본값:

- `SLA_SCANNER_INTERVAL_MS=300000`
- `SLA_SCANNER_INITIAL_DELAY_MS=300000`
- `OUTBOUND_JOBS_INTERVAL_MS=60000`

## 3. 서비스 기동

```bash
docker compose --env-file .env.beta -f docker-compose.beta.yml up -d --build
```

확인:

```bash
docker compose -f docker-compose.beta.yml ps
curl https://<PUBLIC_HOSTNAME>/api/health
```

기대 결과:

```json
{"status":"UP","database":"UP","migrations":"UP"}
```

## 4. GitHub App URL 설정

[github-app-checklist.md](github-app-checklist.md) 기준으로 아래 URL을 맞춥니다.

- Homepage URL: `https://<PUBLIC_HOSTNAME>/`
- Callback URL: `https://<PUBLIC_HOSTNAME>/auth/github/callback`
- Setup URL: `https://<PUBLIC_HOSTNAME>/app/installations/setup`
- Webhook URL: `https://<PUBLIC_HOSTNAME>/api/webhooks/github`

## 5. 배포 smoke test

1. `https://<PUBLIC_HOSTNAME>/` 접속
2. `GitHub App 설치`
3. 설치 완료 후 `/app/installations/setup?installation_id=...` 복귀 확인
4. Discord webhook 저장
5. draft PR을 `Ready for review` 로 전환
6. GitHub PR에서 `Club PR SLA` Check가 `On track` 인지 확인

## 6. Daily ops

운영자는 아래 4곳만 먼저 봅니다.

- `/api/health`
- `/ops`
- `/ops/deliveries?status=DEAD`
- `/ops/dead-letters?status=PENDING`

자세한 점검은 [operator-daily-check.md](operator-daily-check.md) 를 따릅니다.

## 7. Rollback / restore

애플리케이션만 롤백할 때:

```bash
docker compose --env-file .env.beta -f docker-compose.beta.yml down
git checkout <previous-release-commit>
docker compose --env-file .env.beta -f docker-compose.beta.yml up -d --build
```

Postgres 백업:

```bash
docker compose -f docker-compose.beta.yml exec postgres \
  pg_dump -U club_sla club_sla > club_sla_backup.sql
```

복구:

```bash
docker compose -f docker-compose.beta.yml exec -T postgres \
  psql -U club_sla -d club_sla < club_sla_backup.sql
```

비밀값 복구:

- `.env.beta` 를 안전한 위치에 백업합니다.
- GitHub App private key / client secret / webhook secret / Discord webhook은 운영자가 재주입할 수 있어야 합니다.

## 8. 디자인 파트너 온보딩

실제 파트너 온보딩은 [design-partner-onboarding-checklist.md](design-partner-onboarding-checklist.md) 를 따릅니다.
