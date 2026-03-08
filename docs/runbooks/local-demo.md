# Local Demo Runbook

## 목표

로컬 머신과 `ngrok` 만으로 임시 데모 세션을 빠르게 다시 띄웁니다.

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

## 4. PR 데모

1. 설치 대상 저장소에서 draft PR 생성
2. `Ready for review` 로 전환
3. GitHub PR 화면에서 `Club PR SLA — On track` 확인

## 5. 상태 확인

```bash
./scripts/demo-status.sh
```

아래를 한 번에 확인할 수 있습니다.

- app port
- db port
- ngrok host
- health 응답
- app/ngrok pid

## 6. 종료

```bash
./scripts/demo-down.sh
```

스크립트가 아래를 정리합니다.

- 백그라운드 앱 프로세스
- `ngrok`
- 로컬 postgres 컨테이너
