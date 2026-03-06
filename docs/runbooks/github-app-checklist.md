# GitHub App Checklist

## URLs

- Homepage URL: `https://<host>/`
- Callback URL: `https://<host>/auth/github/callback`
- Setup URL: `https://<host>/app/installations/setup`
- Webhook URL: `https://<host>/api/webhooks/github`

## Webhook

- Webhook secret은 `GITHUB_WEBHOOK_SECRET` 와 동일해야 합니다.
- App이 설치된 뒤 `installation` 과 `installation_repositories` 이벤트가 실제로 들어오는지 확인합니다.

## Permissions

- `Pull requests`: Read-only
- `Checks`: Read and write
- `Metadata`: Read-only

## Subscribed events

- `Pull request`
- `Pull request review`
- `Installation`
- `Installation repositories`

## OAuth / App values

- App ID -> `GITHUB_APP_APP_ID`
- Client ID -> `GITHUB_APP_CLIENT_ID`
- Client Secret -> `GITHUB_APP_CLIENT_SECRET`
- Private Key PEM -> `GITHUB_APP_PRIVATE_KEY`
- Install URL -> `GITHUB_APP_INSTALL_URL`

## Private beta prerequisites

- 기본 운영은 `BETA_ALLOWED_GITHUB_LOGINS` 기준입니다.
- account 단위 허용이 꼭 필요할 때만 `BETA_ALLOWED_GITHUB_ACCOUNTS` 를 추가합니다.

## Post-install smoke checks

1. setup redirect가 `/app/installations/setup?installation_id=...` 로 돌아온다.
2. installation 설정 페이지 접근이 된다.
3. Discord webhook 저장이 성공한다.
4. `ready_for_review` PR에서 `Club PR SLA` Check가 `On track` 으로 보인다.
5. `pull_request_review.submitted` 이후 Check가 `Reviewed` 로 바뀐다.
