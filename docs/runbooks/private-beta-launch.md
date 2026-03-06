# Private Beta Launch Runbook

## 목표

설치형 GitHub App 서비스를 `단일 앱 컨테이너 + Postgres` 형태로 private beta 운영 환경에 배포합니다.

## 1. 필수 준비물

- TLS가 적용된 공개 도메인 1개
- Postgres 16 이상
- GitHub App 1개
- Discord webhook을 연결할 테스트 서버 1개
- 환경 변수 파일 1개

## 2. 환경 변수 계약

필수 값:

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
- `BETA_ALLOWED_GITHUB_LOGINS` 또는 `BETA_ALLOWED_GITHUB_ACCOUNTS`

선택 값:

- `SLA_SCANNER_INTERVAL_MS`
- `SLA_SCANNER_INITIAL_DELAY_MS`
- `OUTBOUND_JOBS_INTERVAL_MS`
- `SLA_FALLBACK_36H_ENABLED`
- `OPS_SECURITY_RATE_LIMIT_MAX_REQUESTS`
- `OPS_SECURITY_RATE_LIMIT_WINDOW_SECONDS`

## 3. 이미지 빌드

```bash
docker build -t club-pr-sla:beta .
```

## 4. 컨테이너 실행

```bash
docker run -d \
  --name club-pr-sla \
  --env-file .env.beta \
  -p 8080:8080 \
  club-pr-sla:beta
```

앱은 내부적으로 Flyway migration을 실행한 뒤 시작합니다. 별도 Redis는 필요하지 않습니다.

## 5. 배포 직후 확인

```bash
curl http://<host>/api/health
```

기대 결과:

```json
{"status":"UP","database":"UP","migrations":"UP"}
```

Prometheus 확인:

```bash
curl -H "X-Admin-Api-Key: ${OPS_ADMIN_API_KEY}" http://<host>/actuator/prometheus
```

Ops UI 확인:

1. `http://<host>/ops/login` 접속
2. `OPS_ADMIN_API_KEY` 입력
3. `/ops` overview 진입 확인

## 6. GitHub App 연결 확인

GitHub App 설정은 [github-app-checklist.md](/Users/sungjh/club-pr-tool/.worktrees/codex-installable-github-app-v1/docs/runbooks/github-app-checklist.md) 를 기준으로 점검합니다.

최소 smoke sequence:

1. 홈 `/` 접속
2. GitHub 로그인
3. App 설치
4. setup redirect 확인
5. Discord webhook 저장
6. `ready_for_review` PR 생성
7. GitHub Check `On track` 확인

디자인 파트너 온보딩 절차는 [design-partner-onboarding-checklist.md](/Users/sungjh/club-pr-tool/.worktrees/codex-installable-github-app-v1/docs/runbooks/design-partner-onboarding-checklist.md) 를 따릅니다.

## 7. 운영 중 실패 복구

Dead letters 조회:

```bash
curl -H "X-Admin-Api-Key: ${OPS_ADMIN_API_KEY}" \
  "http://<host>/api/admin/dead-letters?status=PENDING&limit=20"
```

Replay:

```bash
curl -X POST -H "X-Admin-Api-Key: ${OPS_ADMIN_API_KEY}" \
  http://<host>/api/admin/dead-letters/<id>/replay
```

의미:

- replay는 외부 API를 직접 다시 호출하지 않고 동일 payload로 새 outbound job을 enqueue 합니다.
- 동일한 dedupe key가 살아 있으면 중복 job은 생성되지 않습니다.

운영자가 매일 보는 체크 절차는 [operator-daily-check.md](/Users/sungjh/club-pr-tool/.worktrees/codex-installable-github-app-v1/docs/runbooks/operator-daily-check.md) 에 있고, 장애 triage와 replay 판단 기준은 [incident-triage-replay.md](/Users/sungjh/club-pr-tool/.worktrees/codex-installable-github-app-v1/docs/runbooks/incident-triage-replay.md) 에 정리했습니다.

## 8. 배포 제한 사항

- 단일 인스턴스 전제입니다.
- 외부 전송 큐는 DB 테이블 기반입니다.
- Slack, 과금, 멀티 워크스페이스, 저장소별 세부 SLA는 이번 단계 범위 밖입니다.
