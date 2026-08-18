# NFR Requirements Plan - Backend Unit

## Plan

- [ ] requirements.md의 성능/가용성/보안 목표를 Backend NFR로 구체화
- [ ] 아래 질문 답변을 반영해 데이터 접근 계층/캐시/토큰 저장/PBT 프레임워크 확정
- [ ] nfr-requirements.md 작성
- [ ] tech-stack-decisions.md 작성 (PBT-09 프레임워크 선택 포함)

## Questions

각 질문에 알파벳으로 답한 뒤 채팅으로 "답변 완료"라고 알려주세요.

## Question 1 (Tech Stack Selection)

tech-env.md는 데이터 접근 계층을 Spring Data JPA 또는 MyBatis 중 택1로 남겨두었습니다. 어느 것을 사용할까요?

A) Spring Data JPA - 엔티티 매핑/쿼리 메서드 자동 생성, 학습 곡선이 낮음

B) MyBatis - SQL을 직접 작성해 세밀하게 제어

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 2 (Scalability)

로그인 실패 횟수(failedLoginCount) 기반 잠금 정책은 백엔드가 여러 파드로 확장될 수 있어 파드 간 공유 저장소가 필요합니다. 어디에 저장할까요?

A) Redis (카운터/잠금 상태를 파드 간 공유, TTL로 자동 해제)

B) DB (User 테이블 컬럼, 이미 도메인모델에 정의됨)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3 (Security / Tech Stack)

Refresh Token은 어디에 저장할까요?

A) DB (도메인모델의 RefreshToken 엔티티 사용)

B) Redis (TTL로 자동 만료, 조회 성능 이점)

C) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 4 (Tech Stack - PBT Framework)

Property-Based Testing 프레임워크로 jqwik(JUnit5 연동, requirements.md 권고)을 사용할까요?

A) 예 - jqwik 사용

B) 다른 프레임워크를 선호함

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 5 (Observability)

Resiliency Baseline 확장은 적용하지 않기로 했지만(requirements.md), 최소한의 헬스체크/모니터링 수준을 정해야 합니다.

A) Spring Boot Actuator 기본 헬스체크(/actuator/health)만 적용

B) Actuator + Prometheus 메트릭 노출(/actuator/prometheus)까지 적용

C) Other (please describe after [Answer]: tag below)

[Answer]: B
