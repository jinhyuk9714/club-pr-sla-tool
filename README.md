# Club PR SLA Tool

PR 리뷰가 늦어지는 순간을 GitHub Check와 Discord로 알려주는 설치형 GitHub App입니다.  
처음 목표는 단순했습니다. 팀 안에서 "누가 리뷰해야 하는지", "얼마나 늦었는지", "운영자가 어디를 봐야 하는지"를 사람 기억에 맡기지 않는 도구를 만드는 것이었습니다.

이 프로젝트는 그 문제를 다음 흐름으로 풀었습니다.

1. 저장소에 GitHub App을 설치합니다.
2. installation별 Discord webhook을 한 번 연결합니다.
3. PR이 `Ready for review` 상태가 되면 SLA 추적을 시작합니다.
4. 리뷰가 늦어지면 Discord와 GitHub Check에 상태를 반영합니다.
5. 운영자는 `/ops` 화면에서 설치 상태, 작업 큐, dead letter를 확인합니다.

## 왜 만들었는가

PR 리뷰 지연은 흔한 문제지만, 실제로는 추적이 잘 안 됩니다.  
슬랙이나 디스코드에 사람이 직접 올리면 누락되기 쉽고, 저장소가 늘어나면 누가 어떤 PR을 봐야 하는지도 금방 흐려집니다.

그래서 이 프로젝트는 "리뷰 SLA를 운영 가능한 형태로 자동화하는 서비스"를 목표로 잡았습니다.

- 사용자는 GitHub App 설치와 Discord webhook 저장만 하면 됩니다.
- PR 화면에서는 `Club PR SLA` Check로 현재 상태를 바로 볼 수 있습니다.
- 운영자는 별도 `/ops` 화면에서 실패한 알림과 dead letter까지 관리할 수 있습니다.

핵심은 알림 봇 하나를 만드는 것이 아니라, 설치형 제품처럼 써볼 수 있는 운영 도구를 만드는 것이었습니다.

## 무엇을 만들었는가

사용자 기준 흐름은 아주 짧습니다.

1. 서비스 홈에 들어갑니다.
2. `GitHub App 설치`를 누릅니다.
3. 설치 후 설정 화면에서 Discord webhook을 저장합니다.
4. 이후 draft PR을 `Ready for review`로 바꾸면 추적이 시작됩니다.

이후 시스템은 다음을 자동으로 처리합니다.

- GitHub PR Check 상태 표시
  - `On track`
  - `At risk`
  - `Breached`
  - `Reviewed`
  - `App setup required`
- Discord reminder / escalation 전송
- installation별 활성 저장소 동기화
- 실패한 외부 전송의 retry / dead letter 처리
- 운영자용 설치 상태, delivery queue, replay UI 제공

## 설계를 어떻게 가져갔는가

### 1. 설치형 GitHub App을 기준으로 잡았다

처음부터 라이브러리나 GitHub Action보다 "설치형 GitHub App"에 가깝게 가는 편이 맞다고 봤습니다.  
이 방식이 사용자 입장에서 가장 단순합니다. 저장소에 코드를 추가하지 않아도 되고, 설치 후 Discord webhook 한 번만 연결하면 되기 때문입니다.

그래서 도메인도 installation 중심으로 잡았습니다.

- `github_installations`
- `github_installation_repository_entries`
- `installation_settings`

PR 상태는 기존 `pull_request_states`를 유지하되, installation과 repository 활성 여부를 기준으로만 추적하도록 했습니다.

### 2. 알림은 직접 보내지 않고 DB 작업 큐로 보냈다

Discord 알림과 GitHub Check 동기화는 외부 API 호출이라 실패 가능성이 높습니다.  
이걸 webhook 처리나 스캐너 안에서 바로 호출하면, 실패했을 때 복구가 애매해집니다.

그래서 외부 전송은 `outbound_delivery_jobs` 테이블로 분리했습니다.

- `DISCORD_NOTIFICATION`
- `GITHUB_CHECK_SYNC`

그리고 worker가 이 큐를 처리하도록 만들었습니다.

- 재시도 정책: `1m -> 5m -> 15m`
- 최대 3회 실패 시 `DEAD`
- dead letter로 전환 후 운영자 replay 가능

이 구조 덕분에 "보냈는지 안 보냈는지 모르는 상태"를 줄일 수 있었습니다.

### 3. 사용자 흐름과 운영자 흐름을 분리했다

개발하면서 가장 크게 느낀 문제는 기능보다 사용성이었습니다.  
처음에는 installation ID를 직접 넣는 식으로 흐름이 열려 있었는데, 이건 일반 사용자 입장에서 불필요하게 어렵습니다.

그래서 사용자용 재진입 경로를 따로 만들었습니다.

- 홈에서는 installation ID를 직접 입력하지 않습니다.
- `설치 후 설정 계속하기`는 `/app/installations`로 들어갑니다.
- 로그인한 사용자가 접근 가능한 installation이
  - 0개면 빈 상태 안내
  - 1개면 바로 설정 화면
  - 여러 개면 선택 화면

반대로 운영자는 `/ops` 아래에서만 설치, queue, dead letter를 보도록 분리했습니다.

## 구현하면서 중요했던 포인트

### GitHub Check는 "보여주기용"이 아니라 상태 모델이어야 했다

이 프로젝트에서 GitHub Check는 단순한 부가 기능이 아닙니다.  
사용자가 가장 먼저 보는 결과물이기 때문에, SLA 상태를 GitHub PR 화면에서 바로 이해할 수 있어야 했습니다.

그래서 `ready_for_review`, `review submitted`, scanner 결과에 따라 Check 상태를 계속 동기화하도록 만들었습니다.

### 실패한 전송은 로그가 아니라 운영 대상이어야 했다

알림 실패를 warning log로만 남기면 나중에 운영자가 놓치기 쉽습니다.  
그래서 실패는 queue 상태와 dead letter로 남기고, ops UI에서 replay 가능한 구조로 만들었습니다.

### 데모와 실제 사용은 생각보다 다르다

직접 여러 번 데모를 돌려보니, 기술적으로는 동작해도 사용자가 막히는 지점이 분명했습니다.

- 홈에서 다음 행동이 명확한지
- 설치 후 설정 화면으로 자연스럽게 이어지는지
- "이 서비스가 지금 뭘 해줬는지"를 사용자가 바로 이해하는지

그래서 최근에는 기능 추가보다 온보딩 단순화와 데모 키트 자동화에 더 많은 시간을 썼습니다.

## 기술 스택

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- Thymeleaf
- Docker / Docker Compose
- Caddy
- GitHub App / GitHub Checks API
- Discord Webhook

## 검증은 어떻게 했는가

문서만 쌓아두지 않고 실제 흐름을 여러 번 검증했습니다.

자동 검증:

- `./gradlew clean test`
- `./scripts/check.sh`

수동 검증:

- GitHub App 설치
- setup callback 진입
- Discord webhook 저장
- draft PR -> `Ready for review`
- GitHub PR에서 `Club PR SLA — On track` 확인
- `/ops` 화면에서 installation / deliveries / dead letters 확인

추가로 로컬 데모를 반복하기 쉽게 만들기 위해 아래 스크립트를 넣었습니다.

- `./scripts/demo-up.sh`
- `./scripts/demo-status.sh`
- `./scripts/demo-down.sh`

이 스크립트는 포트 충돌이 있어도 `8080/8081`, `5432/5433` 중 가능한 값을 골라 데모 세션을 자동으로 띄우고 정리합니다.

## 아쉬운 점

아직 "누가 봐도 쉬운 제품"이라고 하긴 어렵습니다.

- GitHub App 자체를 운영자가 먼저 준비해야 합니다.
- 설치형 제품으로는 입구가 더 단순해져야 합니다.
- 아직은 private beta 수준이고, self-serve public product로 보기엔 부족합니다.
- 관측과 운영 자동화도 더 보강할 부분이 남아 있습니다.

즉, 현재 평가는 이렇습니다.

- 엔진: 꽤 단단해졌음
- 운영 도구로서의 구조: 실사용 가능
- 일반 사용자에게 쉬운 제품성: 더 개선 필요

## 다음 단계

다음 우선순위는 명확합니다.

1. 실제 사용자 데모를 여러 번 돌리면서 막히는 지점을 수집
2. 온보딩 UX를 더 줄이고 설명보다 행동 중심으로 다듬기
3. 단일 VPS 또는 관리형 플랫폼에 private beta 배포
4. 운영 메트릭과 장애 대응 자동화 보강

## 실행과 문서

상세 실행 절차는 아래 문서를 참고하면 됩니다.

- 로컬 실행: [docs/runbooks/local-setup.md](docs/runbooks/local-setup.md)
- 반복 데모: [docs/runbooks/local-demo.md](docs/runbooks/local-demo.md)
- GitHub App 설정: [docs/runbooks/github-app-checklist.md](docs/runbooks/github-app-checklist.md)
- private beta 배포: [docs/runbooks/private-beta-launch.md](docs/runbooks/private-beta-launch.md)
- VPS 준비: [docs/runbooks/vps-bootstrap.md](docs/runbooks/vps-bootstrap.md)
- 운영 점검: [docs/runbooks/operator-daily-check.md](docs/runbooks/operator-daily-check.md)

## 한 줄 요약

이 프로젝트는 **PR 리뷰 지연을 GitHub Check와 Discord로 추적하고, 운영자가 실패까지 관리할 수 있게 만든 설치형 GitHub App 서비스**입니다.
