# Design Partner Onboarding Checklist

## 목표

디자인 파트너 팀 1개를 개발자 개입 없이 private beta 환경에 붙입니다.

## 사전 준비

1. 파트너의 GitHub user login 또는 installation 대상 account login을 확인합니다.
2. 운영 환경 변수에 allowlist를 반영합니다.
   - `BETA_ALLOWED_GITHUB_LOGINS`
   - `BETA_ALLOWED_GITHUB_ACCOUNTS`
3. GitHub App 설정이 [github-app-checklist.md](/Users/sungjh/club-pr-tool/.worktrees/codex-installable-github-app-v1/docs/runbooks/github-app-checklist.md) 와 일치하는지 확인합니다.
4. 파트너가 사용할 Discord webhook을 받습니다.
5. `OPS_ADMIN_API_KEY` 로 `/ops/login` 접근이 되는지 확인합니다.

## 온보딩 진행

1. 파트너에게 서비스 홈 URL을 전달합니다.
2. 파트너가 `GitHub으로 시작하기` 로 로그인하게 합니다.
3. 파트너가 GitHub App 설치 화면에서 대상 repository를 선택하게 합니다.
4. 설치 후 `/app/installations/setup?installation_id=...` 로 복귀하는지 확인합니다.
5. installation 설정 페이지에서 Discord webhook 저장을 안내합니다.
6. 저장 직후 `/ops/installations` 에서 installation이 `configured=true` 로 보이는지 확인합니다.

## 완료 확인

1. 파트너 repository에서 draft PR 하나를 `Ready for review` 로 전환합니다.
2. `/ops/deliveries` 에서 해당 installation의 `GITHUB_CHECK_SYNC` job이 보이는지 확인합니다.
3. GitHub PR에서 `Club PR SLA` Check가 `On track` 인지 확인합니다.
4. Discord 채널에 테스트 알림이 필요한 경우 수동 replay 또는 시간 경과 테스트를 진행합니다.

## 실패 시 우선 확인

1. allowlist 값에 오타가 없는지 확인합니다.
2. `/ops/installations` 에서 installation status가 `ACTIVE` 인지 확인합니다.
3. `/ops/deliveries?status=DEAD` 와 `/ops/dead-letters?status=PENDING` 를 확인합니다.
4. 복구 절차는 [incident-triage-replay.md](/Users/sungjh/club-pr-tool/.worktrees/codex-installable-github-app-v1/docs/runbooks/incident-triage-replay.md) 를 따릅니다.
