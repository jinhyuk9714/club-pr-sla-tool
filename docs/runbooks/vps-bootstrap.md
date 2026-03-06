# VPS Bootstrap Runbook

## 목표

단일 VPS에서 private beta 서비스를 올릴 최소 운영 기반을 준비합니다.

## 1. 시스템 준비

1. Ubuntu 24.04 LTS 또는 동급 Linux 1대 준비
2. DNS에서 `PUBLIC_HOSTNAME` 을 VPS 공인 IP로 연결
3. 방화벽에서 `80`, `443` 만 공개
4. SSH는 키 기반 인증만 허용

## 2. Docker / Compose 설치

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker
docker compose version
```

## 3. 애플리케이션 배치

```bash
git clone <repo-url> club-pr-tool
cd club-pr-tool
cp .env.beta.example .env.beta
```

`.env.beta` 에 아래 값을 실제 값으로 채웁니다.

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

`BETA_ENV_FILE=.env.beta` 값은 유지합니다.

## 4. 기동

```bash
docker compose --env-file .env.beta -f docker-compose.beta.yml up -d --build
```

## 5. 확인

```bash
docker compose -f docker-compose.beta.yml ps
curl https://<PUBLIC_HOSTNAME>/api/health
docker compose -f docker-compose.beta.yml logs app --tail=100
docker compose -f docker-compose.beta.yml logs caddy --tail=100
```

기대 결과:

- `postgres`, `app`, `caddy` 가 모두 `Up`
- `/api/health` 가 `{"status":"UP","database":"UP","migrations":"UP"}`

## 6. 정리

- 배포 이후 GitHub App URL을 [github-app-checklist.md](github-app-checklist.md) 기준으로 맞춥니다.
- 다음 단계는 [private-beta-launch.md](private-beta-launch.md) 를 따라 smoke test를 완료합니다.
