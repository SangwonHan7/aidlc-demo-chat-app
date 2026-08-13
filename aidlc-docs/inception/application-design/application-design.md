# Application Design - QuickChat (통합 문서)

이 문서는 components.md, component-methods.md, services.md, component-dependency.md를 통합한 요약입니다. 상세 내용은 각 파일을 참고하세요.

## 범위
백엔드(Spring Boot 단일 서비스) 내부 컴포넌트/서비스 설계. 프론트엔드는 Construction 단계 Frontend 유닛의 Functional Design에서 다룹니다 (application-design-plan.md Question 4 답변 B).

## 컴포넌트 (5개)
- AuthComponent, ChannelComponent, MessagingComponent, PresenceComponent, EventComponent
- 근거: application-design-plan.md Question 1 답변 A (Presence를 별도 컴포넌트로 분리), Question 3 답변 A (Kafka 로직을 EventComponent로 분리)

## 서비스 레이어
- ChatFacadeService가 컴포넌트 2개 이상을 가로지르는 흐름(대표적으로 메시지 전송)을 조정
- 단일 컴포넌트로 끝나는 흐름은 해당 컴포넌트 서비스가 직접 처리
- 근거: application-design-plan.md Question 2 답변 A

## 의존관계 요약
ChatFacadeService가 ChannelComponent/MessagingComponent/EventComponent/PresenceComponent에 의존. MessagingComponent는 EventComponent를 통해서만 Kafka와 통신. 자세한 매트릭스와 메시지 전송 시퀀스는 component-dependency.md 참고.

## 다음 단계와의 연결
- Units Generation에서 이 컴포넌트들이 Backend 유닛 내부의 모듈로 배치됨
- Functional Design(Construction, per-unit)에서 각 메서드의 상세 비즈니스 로직과 PBT 대상 속성(PBT-01) 식별
