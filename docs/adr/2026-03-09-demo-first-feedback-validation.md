# ADR: Demo-First Feedback Validation

## Status

Accepted

## Context

- 현재 제품은 설치형 GitHub App, 온보딩 재진입 흐름, ops UI, 로컬 데모 키트까지 갖춘 상태다.
- 하지만 실제 외부 파일럿 배포 경로는 아직 고정되지 않았다.
- 지금 필요한 정보는 인프라보다 “처음 보는 사용자가 어디서 막히는지”에 더 가깝다.

## Decision

- 당분간 실제 외부 파일럿 배포 대신 `local demo + ngrok` 경로로 사용자 반응을 먼저 수집한다.
- 데모 운영 경로는 `demo-up`, `demo-status`, `demo-down`, `local-demo.md` 로 고정한다.
- 각 세션은 같은 피드백 템플릿으로 기록하고, 3~5회 세션 후 round summary 문서에서 상위 3개 UX 문제를 고정한다.

## Consequences

- 다음 UX 개선 phase는 실제 사용자 관찰을 근거로 잡을 수 있다.
- 외부 배포가 늦어져도 제품 학습은 계속 진행할 수 있다.
- 지금 단계의 우선순위는 기능 확장보다 온보딩 혼동 제거와 설명력 강화로 이동한다.
