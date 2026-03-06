# ADR: Private Beta Hardening

## Status

Accepted

## Date

2026-03-06

## Context

- 설치형 GitHub App V1은 동작했지만 private beta 출시 기준으로는 보안 경계와 운영 복구 경로가 약했다.
- installation 설정 페이지는 GitHub 접근 권한만 확인했고, beta 사용자 제한 계약이 없었다.
- Discord 알림과 GitHub Check 동기화는 호출 시점에 직접 외부 API를 때려서 실패 재시도와 운영 replay가 일관되지 않았다.
- 문서와 로컬 실행 자산 일부가 Redis 전제를 아직 남기고 있어 실제 운영 계약과 어긋났다.

## Decision

- 공개 라우트는 최소만 허용하고, installation 설정 라우트는 `세션 로그인 + installation 접근 권한 + allowlist` 로 보호한다.
- allowlist는 환경 변수 `BETA_ALLOWED_GITHUB_LOGINS`, `BETA_ALLOWED_GITHUB_ACCOUNTS` 로만 관리한다.
- 외부 전송은 `outbound_delivery_jobs` 테이블에 enqueue 하고 단일 워커가 처리한다.
- retry 정책은 `1m -> 5m -> 15m`, 최대 3회로 고정한다.
- 최종 실패는 기존 `dead_letters` 테이블에도 남기고 replay는 새 outbound job enqueue 방식으로 처리한다.
- 메트릭 수집용 `/actuator/prometheus` 는 공개하지 않고 `X-Admin-Api-Key` 로 보호한다.
- 배포 모델은 `single app container + Postgres` 로 고정하고 Redis 런타임 계약은 제거한다.

## Consequences

- 설치형 제품의 베타 사용자 범위가 운영 환경 변수만으로 통제된다.
- Discord와 GitHub Check 전송은 동일한 delivery lifecycle을 따라가므로 장애 복구 방식이 단순해진다.
- 전송은 비동기이므로 webhook 수신 직후 외부 반영까지 짧은 지연이 생길 수 있다.
- Prometheus 스크레이프는 admin API key를 전달할 수 있는 내부 경로에서만 구성해야 한다.
- 멀티 인스턴스 운영과 외부 메시지 브로커는 다음 단계로 미룬다.
