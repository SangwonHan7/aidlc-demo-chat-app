# NFR Design Plan - Backend Unit

## Plan

- [ ] nfr-requirements.md 기반으로 회복성/확장성/성능/보안 패턴 설계
- [ ] 아래 질문 답변 반영
- [ ] nfr-design-patterns.md 작성
- [ ] logical-components.md 작성 (Redis/Kafka 구성 등)

## Questions

각 질문에 알파벳으로 답한 뒤 채팅으로 "답변 완료"라고 알려주세요.

## Question 1 (Resilience Patterns)

Resiliency Baseline 확장은 꺼져 있지만(requirements.md Q6=B) 최소한의 내결함성은 필요합니다. Kafka 발행(publish) 실패 시 어떻게 처리할까요?

A) 재시도 로직 추가 (예: 짧은 간격으로 3회 재시도 후 실패 로그)

B) 재시도 없이 즉시 실패 처리 (실습 범위로 단순화)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 2 (Scalability Patterns)

백엔드 파드 오토스케일링을 적용할까요? (NAS 리소스가 제한적임을 감안)

A) 적용 - CPU 사용률 기준 HPA(수평 파드 오토스케일러) 설정

B) 미적용 - 고정 레플리카 수로 운영 (NAS 리소스 제약 고려)

C) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 3 (Performance Patterns)

채널 멤버십 확인(isMember)은 메시지 전송마다 호출되는 핫 패스입니다. 캐싱할까요?

A) Redis에 멤버십 캐싱 (멤버 변경 시 무효화)

B) 캐싱 없이 매번 DB 조회 (MVP 단순화)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 4 (Security Patterns)

메시지 전송에 대한 사용자별 rate limiting(스팸/오남용 방지)을 적용할까요?

A) 적용 - 사용자당 초당 메시지 수 제한

B) 미적용 - MVP 범위 밖으로 명시

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 5 (Logical Components)

온라인 상태, 로그인 잠금 카운터, Refresh Token, WebSocket Pub/Sub까지 모두 Redis를 사용하기로 했습니다(NFR Requirements). Redis 구성은 어떻게 할까요?

A) 단일 Redis 인스턴스, 키 프리픽스로 용도 구분 (NAS 리소스 절약)

B) 용도별로 별도 Redis 인스턴스 또는 DB 번호 분리

C) Other (please describe after [Answer]: tag below)

[Answer]: A
