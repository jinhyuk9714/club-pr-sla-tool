# Design Partner Onboarding Checklist

## 목표

디자인 파트너 팀 1개를 개발자 개입 없이 private beta 환경에 붙입니다.

## 사전 준비

1. 파트너 GitHub login을 확인하고 `BETA_ALLOWED_GITHUB_LOGINS` 에 반영합니다.
2. GitHub App 설정이 [github-app-checklist.md](github-app-checklist.md) 와 일치하는지 확인합니다.
3. `OPS_ADMIN_API_KEY` 로 `/ops/login` 이 되는지 확인합니다.
4. 파트너가 저장할 Discord webhook을 준비하게 합니다.

## 온보딩 진행

1. 파트너에게 서비스 홈 URL 전달
2. 홈에서 `GitHub App 설치` 진행
3. 설치 완료 후 `/app/installations/setup?installation_id=...` 복귀 확인
4. installation 설정 페이지에서 Discord webhook 저장
5. `/ops/installations` 에서 installation이 `configured=true` 인지 확인

## 완료 확인

1. 파트너 저장소에서 draft PR 하나를 `Ready for review` 로 전환
2. `/ops/deliveries` 에서 해당 installation의 `GITHUB_CHECK_SYNC` job 확인
3. GitHub PR에서 `Club PR SLA` Check가 `On track` 인지 확인

## 실패 시 우선 확인

1. `BETA_ALLOWED_GITHUB_LOGINS` 오타 여부
2. `/api/health`
3. `/ops/deliveries?status=DEAD`
4. `/ops/dead-letters?status=PENDING`
5. 복구가 필요하면 [incident-triage-replay.md](incident-triage-replay.md) 절차 수행
