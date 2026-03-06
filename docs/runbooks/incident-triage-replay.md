# Incident Triage and Replay

## 목표

Discord 알림 또는 GitHub Check 동기화 실패가 발생했을 때 운영자가 우선 확인하고 복구하는 절차입니다.

## 1. 장애 분류

먼저 `/ops/deliveries?status=DEAD` 와 `/ops/dead-letters?status=PENDING` 를 확인합니다.

- `DISCORD_NOTIFICATION`
  - Discord webhook 삭제, 권한 문제, 4xx/5xx 응답 가능성
- `GITHUB_CHECK_SYNC`
  - installation token, 권한, 대상 PR SHA 불일치, GitHub API 일시 장애 가능성

## 2. 빠른 판별

1. 특정 installation 하나만 영향 받는지 확인합니다.
2. 같은 repository / PR 조합에서 반복 실패하는지 확인합니다.
3. `lastError` 가 인증 문제인지 일시 장애인지 구분합니다.

즉시 replay 하면 안 되는 경우:

- installation이 `REMOVED` 또는 `SUSPENDED`
- Discord webhook이 이미 무효
- allowlist 또는 설정이 아직 완료되지 않음

## 3. 복구 절차

1. 원인 수정
   - Discord webhook 재설정
   - GitHub App 권한 또는 installation 상태 점검
   - partner 설정 누락 보완
2. `/ops/dead-letters?status=PENDING` 에서 대상 row 확인
3. `Replay` 버튼 실행
4. flash message가 성공인지 확인
5. `/ops/deliveries` 에 새 outbound job이 생성됐는지 확인
6. 최종적으로 GitHub Check 또는 Discord 채널 반영 여부를 확인

## 4. UI replay 실패 시

1. `/api/admin/dead-letters/<id>/replay` JSON API로 동일 작업을 시도합니다.
2. 그래도 실패하면 `/api/admin/audit-logs` 에서 replay 기록을 확인합니다.
3. 반복적으로 같은 유형이 죽으면 신규 incident로 기록하고 code-level fix 대상으로 분리합니다.

## 5. 운영 메모

- replay는 외부 전송을 즉시 다시 호출하지 않고 새 outbound job을 enqueue 합니다.
- dedupe key가 살아 있으면 동일 작업이 중복 생성되지 않을 수 있습니다.
- legacy dead letter는 replay가 불가능할 수 있으며 UI에 별도 에러 메시지로 표시됩니다.
