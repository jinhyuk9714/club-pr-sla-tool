# Club PR SLA Tool

PR 리뷰 지연을 GitHub Check와 Discord로 추적하는 설치형 GitHub App입니다.  
저장소에 App을 설치하고 Discord webhook을 한 번 연결하면, PR이 `Ready for review` 상태가 되는 순간부터 리뷰 SLA를 추적합니다.

## 프로젝트 소개

이 프로젝트는 "리뷰가 얼마나 밀렸는지"를 사람이 직접 기억하지 않아도 되게 만드는 도구입니다.  
핵심은 단순한 알림 봇이 아니라, GitHub 안에서 상태를 확인하고 운영자도 실패를 관리할 수 있는 설치형 서비스로 만드는 것이었습니다.

## 해결하려는 문제

PR 리뷰 지연은 흔하지만 추적은 생각보다 허술합니다. 메시지로 한 번 공지하고 끝내면 금방 묻히고, 저장소가 여러 개면 누가 어떤 PR을 봐야 하는지도 흐려집니다.  
이 프로젝트는 그 문제를 GitHub App 설치, Discord 연결, 자동 상태 추적이라는 짧은 흐름으로 풀었습니다.

## 핵심 기능

사용자는 서비스 홈에서 GitHub App을 설치하고 installation별 Discord webhook만 저장하면 됩니다.  
그 이후에는 PR이 `Ready for review`가 될 때 SLA 추적이 시작되고, 상태가 GitHub Check와 Discord에 반영됩니다.

- GitHub Check 상태 표시: `On track`, `At risk`, `Breached`, `Reviewed`
- Discord reminder / escalation 전송
- installation별 저장소 동기화
- `/ops` 화면에서 installation, delivery queue, dead letter 확인

## 구현 포인트

설계는 installation 중심으로 잡았습니다. 저장소별 설정이 아니라 GitHub App installation 단위로 상태와 Discord webhook을 관리하도록 만들었습니다.  
외부 전송은 webhook 처리 코드 안에서 바로 보내지 않고 DB 작업 큐로 분리했습니다. 그래서 retry, dead letter, replay 같은 운영 기능을 붙일 수 있었습니다.

사용성도 같이 손봤습니다. 홈에서 installation ID를 직접 입력하지 않게 바꾸고, 설치 후에는 `/app/installations` 경로를 통해 자신의 설정 화면으로 다시 들어오게 정리했습니다.  
결과적으로 사용자 흐름과 운영자 흐름을 분리했고, GitHub Check를 중심으로 현재 상태를 바로 읽을 수 있게 만들었습니다.

## 검증과 참고 문서

기본 검증은 `./gradlew clean test` 와 `./scripts/check.sh` 로 돌립니다.  
수동 검증은 GitHub App 설치, Discord webhook 저장, draft PR을 `Ready for review`로 바꾼 뒤 `Club PR SLA — On track` 가 보이는지 확인하는 방식으로 진행했습니다.

상세 실행 절차는 아래 문서를 참고하면 됩니다.

- 로컬 실행: [docs/runbooks/local-setup.md](docs/runbooks/local-setup.md)
- 반복 데모: [docs/runbooks/local-demo.md](docs/runbooks/local-demo.md)
- GitHub App 설정: [docs/runbooks/github-app-checklist.md](docs/runbooks/github-app-checklist.md)
- private beta 배포: [docs/runbooks/private-beta-launch.md](docs/runbooks/private-beta-launch.md)
- 운영 점검: [docs/runbooks/operator-daily-check.md](docs/runbooks/operator-daily-check.md)

아직 public product 수준까지 간 것은 아닙니다. 현재 단계는 private beta에 가깝고, 다음 과제는 온보딩 단순화와 실제 사용자 피드백 수집입니다.
