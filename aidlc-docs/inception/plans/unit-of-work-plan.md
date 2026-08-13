# Unit of Work Plan - QuickChat

## Plan

- [ ] Application Design의 5개 컴포넌트(Auth/Channel/Messaging/Presence/Event)와 stories.md의 7개 스토리를 유닛에 매핑
- [ ] 아래 질문 답변을 반영해 유닛 경계/개발 순서/코드 구조 확정
- [ ] unit-of-work.md 작성 (유닛 정의, 책임, 코드 구조 전략)
- [ ] unit-of-work-dependency.md 작성 (유닛 간 의존관계 매트릭스)
- [ ] unit-of-work-story-map.md 작성 (스토리-유닛 매핑)

## Questions

각 질문에 알파벳으로 답한 뒤 채팅으로 "답변 완료"라고 알려주세요.

## Question 1 (Story Grouping / 유닛 경계)

tech-env.md는 "MVP는 단일 Spring Boot 서비스"와 Next.js 프론트엔드를 명시합니다. 유닛을 몇 개로 나눌까요?

A) Backend, Frontend 2개 유닛 (Application Design의 5개 컴포넌트는 모두 Backend 유닛 내부 모듈로 배치)

B) Backend, Frontend, Infra(K8s 매니페스트/Docker Compose 등 배포 자원) 3개 유닛으로 분리

C) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 2 (Dependencies / 개발 순서)

Frontend는 Backend의 REST/WebSocket API에 의존합니다. 개발 순서를 어떻게 할까요?

A) Backend 유닛을 먼저 완료(설계+코드+테스트)한 뒤 Frontend 유닛 진행

B) API 계약(엔드포인트/메시지 포맷)을 먼저 확정하고, Backend와 Frontend를 병렬로 진행

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3 (Code Organization - Greenfield)

코드 저장 구조를 어떻게 할까요? (aidlc-demo-chat-app 프로젝트 루트 기준)

A) 모노레포 - 프로젝트 루트에 backend/, frontend/ 하위 폴더로 분리

B) 별도 리포지토리 - backend와 frontend를 각각 다른 Git 리포지토리로 분리

C) Other (please describe after [Answer]: tag below)

[Answer]: A
