# Operator Daily Check

## 목표

1주 디자인 파트너 beta 동안 운영자가 매일 확인해야 할 최소 점검 절차입니다.

## 1. 세션 시작

1. `/ops/login` 접속
2. `OPS_ADMIN_API_KEY` 입력
3. `/ops` 대시보드 진입

## 2. Overview 확인

아래 값이 전일 대비 급증하지 않았는지 봅니다.

- active installations
- misconfigured installations
- pending outbound jobs
- dead outbound jobs
- pending dead letters

이상 징후 기준:

- `misconfigured installations > 0`
- `dead outbound jobs > 0`
- `pending dead letters > 0`

## 3. Installations 확인

`/ops/installations` 에서 다음을 봅니다.

- 새 설치가 들어왔는지
- configured가 `false` 인 installation이 남아 있는지
- active repository count가 예상과 다르지 않은지
- removed 또는 suspended installation이 있는지

## 4. Deliveries 확인

`/ops/deliveries` 에서 최소 두 번 확인합니다.

1. `status=PENDING`
   - 오래 쌓여 있는 job이 있는지 확인
2. `status=DEAD`
   - 새로 죽은 Discord / GitHub Check job이 있는지 확인

필터 권장:

- `jobType=DISCORD_NOTIFICATION`
- `jobType=GITHUB_CHECK_SYNC`
- 필요 시 `installationId=<id>`

## 5. Dead Letters 확인

`/ops/dead-letters?status=PENDING` 에서 다음을 봅니다.

- reason
- repoId / prNumber
- stage
- lastError

재현 가능한 일시 장애로 보이면 replay를 시도합니다. 절차는 [incident-triage-replay.md](/Users/sungjh/club-pr-tool/.worktrees/codex-installable-github-app-v1/docs/runbooks/incident-triage-replay.md) 를 따릅니다.

## 6. 마감 체크

1. replay 후 `/ops/deliveries` 에 새 job이 enqueue 되었는지 확인합니다.
2. 필요한 경우 `/api/admin/audit-logs` 로 ops UI login / replay 기록을 확인합니다.
3. 문제 없으면 `/ops/logout` 으로 세션을 닫습니다.
