# Unit of Work - QuickChat

unit-of-work-plan.md 답변: Q1=B(Backend/Frontend/Infra 3유닛), Q2=A(Backend 우선), Q3=A(모노레포)

## Unit 1: Backend
- Type: Service (독립 배포 가능한 Spring Boot 애플리케이션)
- 책임: Application Design의 5개 컴포넌트(Auth, Channel, Messaging, Presence, Event) 전체 구현 - REST API + WebSocket(STOMP)
- 포함 모듈: AuthComponent, ChannelComponent, MessagingComponent, PresenceComponent, EventComponent, ChatFacadeService
- 코드 위치: backend/

## Unit 2: Frontend
- Type: Service (독립 배포 가능한 Next.js 애플리케이션)
- 책임: 로그인/가입 화면, DM/채널 대화 UI, 채널 생성/관리 UI, 온라인 상태 표시, 메시지 이력 스크롤, Zustand 전역 상태 관리
- 코드 위치: frontend/

## Unit 3: Infra
- Type: Infrastructure-as-code / 배포 자원 (서비스가 아닌 지원 유닛)
- 책임: 로컬 개발용 Docker Compose(Kafka KRaft, Redis, Vault, DB), K3s/K0s 배포 매니페스트, Vault 시크릿 등록, Kafka 토픽 생성 스크립트
- 코드 위치: infra/
- 참고: Question 3(코드 구조)의 답변은 backend/frontend 2개 폴더 기준으로 질문했으나, Question 1에서 Infra를 3번째 유닛으로 선택하셨으므로 동일한 모노레포 원칙을 확장해 infra/ 폴더를 추가했습니다. 다르게 두고 싶으시면 말씀해주세요.

## 코드 구조 전략 (모노레포, Question 3 답변 A)
```
aidlc-demo-chat-app/
├── backend/     (Spring Boot)
├── frontend/    (Next.js)
├── infra/       (Docker Compose, K8s manifests, 스크립트)
├── requirements/
└── aidlc-docs/
```

## 개발 순서
1. Infra 유닛의 로컬 개발용 Docker Compose(Kafka/Redis/Vault) 우선 준비 - Backend가 실제 의존성으로 개발/테스트하기 위해 필요
2. Backend 유닛 설계 + 코드 + 테스트 완료 (Question 2 답변 A)
3. Frontend 유닛 설계 + 코드 + 테스트 (Backend API 완성 후 시작)
4. Infra 유닛의 K8s 배포 매니페스트는 Backend/Frontend 컨테이너 이미지 준비 후 마무리
