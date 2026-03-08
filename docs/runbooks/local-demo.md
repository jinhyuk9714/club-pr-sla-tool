# Local Demo Runbook

## 목표

로컬 머신과 `ngrok` 만으로 임시 데모 세션을 빠르게 다시 띄웁니다.

이 runbook의 목적은 기능 설명보다 `처음 보는 사용자가 어디서 막히는지`를 같은 방식으로 관찰하고 기록하는 것입니다.

## 준비물

- Docker / Docker Compose
- JDK 21
- `ngrok`
- 유효한 `.env`
- GitHub App
- 테스트용 Discord webhook

## 1. 데모 세션 기동

```bash
./scripts/demo-up.sh
```

스크립트가 아래를 자동으로 처리합니다.

- Postgres 포트 선택: `5432`, 충돌 시 `5433`
- 앱 포트 선택: `8080`, 충돌 시 `8081`
- 앱 health 확인
- `ngrok` 공개 URL 발급
- GitHub App에 넣을 4개 URL 출력

## 2. GitHub App URL 반영

`demo-up.sh` 출력에 나온 값을 GitHub App 설정에 그대로 넣습니다.

- Homepage URL
- Callback URL
- Setup URL
- Webhook URL

## 3. 온보딩 데모

1. 출력된 홈 URL 접속
2. `GitHub App 설치`
3. 설치 후 installation 설정 화면 진입
4. Discord webhook 저장
5. Discord 채널에서 연결 확인 메시지 수신

운영 규칙:

- 사용자가 스스로 다음 행동을 찾게 둡니다.
- 막히기 전에는 제품 내부 구조를 먼저 설명하지 않습니다.
- 개입이 필요해진 순간을 바로 기록합니다.
- 세션 직후 [demo-feedback-template.md](demo-feedback-template.md) 를 채웁니다.

## 4. PR 데모

1. 설치 대상 저장소에서 draft PR 생성
2. `Ready for review` 로 전환
3. GitHub PR 화면에서 `Club PR SLA — On track` 확인

## 5. Ops 확인

1. `/ops/login`
2. `/ops`
3. installation / delivery 상태를 빠르게 확인

이 단계는 사용자가 아니라 운영자 관점에서 “설치와 첫 PR 처리까지 정상인지”를 보여주는 용도입니다.

## 6. 상태 확인

```bash
./scripts/demo-status.sh
```

아래를 한 번에 확인할 수 있습니다.

- app port
- db port
- ngrok host
- health 응답
- app/ngrok pid

## 7. 세션 후 기록

1. [demo-feedback-template.md](demo-feedback-template.md) 복사 또는 열기
2. 아래 항목을 바로 적습니다.
   - 데모 대상자 유형
   - 멈춘 단계
   - 직접 한 질문
   - 운영자 개입 순간
   - 기대와 실제의 차이
   - 한 문장 설명 성공 여부
3. 같은 날 세션이 2개 이상이면 [../plans/2026-03-09-demo-feedback-round-1.md](../plans/2026-03-09-demo-feedback-round-1.md) 에 누적합니다.

## 8. 종료

```bash
./scripts/demo-down.sh
```

스크립트가 아래를 정리합니다.

- 백그라운드 앱 프로세스
- `ngrok`
- 로컬 postgres 컨테이너
