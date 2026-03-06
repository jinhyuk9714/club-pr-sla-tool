# ADR: Ops UI for Design Partner Beta

## Status

Accepted

## Date

2026-03-06

## Context

- private beta 단계에서 JSON ops API만으로는 운영자가 installation 상태, outbound queue, dead letter를 빠르게 파악하기 어려웠다.
- 디자인 파트너 1~2개 팀을 실제로 운영하려면 개발자 개입 없이 일일 점검과 replay를 할 수 있어야 했다.
- 별도 admin 계정 체계를 지금 도입하면 운영 복잡도만 늘고 beta 속도를 늦춘다.

## Decision

- 같은 Spring 앱 안에 server-rendered ops UI를 추가한다.
- ops UI 인증은 `OPS_ADMIN_API_KEY` 를 입력받아 만든 별도 HTTP session으로 처리한다.
- `/ops/**` 는 ops session이 없으면 `/ops/login` 으로 보낸다.
- UI는 overview, installations, deliveries, dead letters 네 화면만 제공한다.
- dead letter replay는 기존 서비스와 JSON ops API를 재사용하고, UI는 운영에 필요한 요약 정보만 노출한다.

## Consequences

- 운영자는 브라우저만으로 beta 상태를 빠르게 점검할 수 있다.
- 별도 operator account 관리가 없으므로 `OPS_ADMIN_API_KEY` 보안 관리가 중요해진다.
- ops UI는 내부 운영용이므로 self-serve admin 기능이나 allowlist 편집 기능은 포함하지 않는다.
- 멀티 인스턴스 세션 관리와 고급 감사 기능은 다음 단계로 미룬다.
