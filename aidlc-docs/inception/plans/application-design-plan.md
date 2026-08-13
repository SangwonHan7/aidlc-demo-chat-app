# Application Design Plan - QuickChat

## Plan

- [ ] requirements.md(FR-1~FR-8), stories.md 재검토하여 핵심 비즈니스 역량 식별
- [ ] 아래 질문 답변을 반영해 컴포넌트 경계/서비스 오케스트레이션 방식 확정
- [ ] components.md 작성 (컴포넌트별 책임/인터페이스)
- [ ] component-methods.md 작성 (메서드 시그니처, 상세 비즈니스 로직은 Functional Design에서)
- [ ] services.md 작성 (서비스 정의 및 오케스트레이션)
- [ ] component-dependency.md 작성 (의존관계, 통신 패턴, 데이터 흐름)
- [ ] application-design.md 작성 (위 4개 문서 통합본)

## Questions

각 질문에 알파벳으로 답한 뒤 채팅으로 "답변 완료"라고 알려주세요.

## Question 1 (Component Identification)

tech-env.md는 "MVP는 단일 Spring Boot 서비스, 필요 시 채팅/알림 서비스로 분리 가능한 구조로 패키지 분리"를 명시합니다. 백엔드 내부 컴포넌트 경계를 어떻게 나눌까요?

A) Auth, Channel, Messaging, Presence 4개 컴포넌트로 분리 (온라인 상태를 별도 컴포넌트로)

B) Auth, Channel, Messaging 3개 컴포넌트로 분리 (온라인 상태는 Messaging에 포함)

C) Auth, Chat(Channel+Messaging+Presence 통합) 2개 컴포넌트로 단순화

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 2 (Service Layer Design)

메시지 전송처럼 여러 컴포넌트(채널 멤버십 확인 -> 메시지 저장 -> Kafka 발행 -> Redis Pub/Sub -> WebSocket 브로드캐스트)를 가로지르는 흐름은 어떻게 조정할까요?

A) 별도 Orchestrator/Facade 서비스(예: ChatFacadeService)를 두어 흐름을 조정

B) Messaging 컴포넌트의 서비스가 다른 컴포넌트의 서비스를 직접 호출 (별도 오케스트레이터 없음)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3 (Component Dependencies)

Kafka 발행/구독 로직은 어디에 둘까요?

A) 별도 컴포넌트(예: EventPublisher/EventConsumer)로 분리해 여러 컴포넌트가 공용으로 사용

B) Messaging 컴포넌트 내부에 포함 (다른 컴포넌트는 Kafka를 직접 다루지 않음)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 4 (설계 범위)

이번 Application Design 범위에 프론트엔드(Next.js) 컴포넌트 구조도 포함할까요?

A) 포함 - 프론트엔드 주요 컴포넌트/상태 구조도 이번에 함께 정의

B) 제외 - 백엔드 중심으로만 진행하고, 프론트엔드는 Construction의 Frontend 유닛 Functional Design에서 다룸

C) Other (please describe after [Answer]: tag below)

[Answer]: B
