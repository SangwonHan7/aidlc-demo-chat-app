# Functional Design Plan - Frontend Unit

## Unit Context

- Stories: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2 (unit-of-work-story-map.md) - 전체 스토리가 Frontend 화면 필요
- 책임: 로그인/가입 화면, DM/채널 대화 UI, 채널 생성/관리 UI, 온라인 상태 표시, 메시지 이력 스크롤, Zustand 전역 상태 관리 (unit-of-work.md)
- Backend와의 관계: Backend가 제공하는 REST API + WebSocket(STOMP) 계약에 의존 (backend/README.md API 개요, aidlc-docs/construction/backend/code/api-layer-summary.md 참고)
- 기술 스택: Next.js 14.x(React 18), TypeScript 5.x, Zustand, Vite, Vitest (tech-env.md)

## Plan

- [x] frontend-components.md 작성 (화면/라우트 구조, 컴포넌트 계층, Zustand 스토어 설계, 컴포넌트별 API/WebSocket 연동 지점)
- [x] domain-entities.md 작성 (프론트엔드 타입/뷰모델 - 백엔드 DTO 대응 관계 명시)
- [x] business-rules.md 작성 (클라이언트 검증 규칙, 토큰 저장/재발급 정책, 재연결 정책)
- [x] business-logic-model.md 작성 (화면별 데이터 흐름 워크플로우, PBT-01 관점 Testable Properties 식별 - 프레임워크 선택은 NFR Requirements에서 결정)

## 처리된 이슈
- Contradiction 1(토큰 저장 방식): 해결 - frontend-functional-design-clarification-questions.md 참고
- Gap 2(Presence API 미연결): 해결 - Backend에 PresenceController 등 소규모 보완 적용, api-layer-summary.md Post-Approval Patch 참고

## Questions

각 질문에 알파벳으로 답한 뒤 채팅으로 "답변 완료"라고 알려주세요.

## Question 1 (Frontend Components / 화면 레이아웃)

전체 UI 레이아웃 구조를 어떻게 할까요?

A) Slack형 앱쉘 - 사이드바(채널/DM 목록) + 메인 패널(대화창)이 하나의 레이아웃에 상시 공존, 채널 전환 시 메인 패널만 교체(클라이언트 라우팅)

B) 완전 분리 페이지 - 채널 목록 페이지와 대화 페이지가 서로 다른 화면으로, 이동 시 전체 페이지 전환

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 2 (State Management)

Zustand 스토어를 어떤 단위로 분리할까요?

A) 단일 글로벌 스토어 - auth/channels/messages/presence를 하나의 스토어에서 관리

B) 도메인별 스토어 분리 - useAuthStore(인증), useChatStore(채널+메시지), usePresenceStore(온라인 상태)로 분리

C) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 3 (Business Rules / 토큰 저장·재발급)

Access/Refresh Token을 어떻게 저장하고 재발급할까요?

A) Access Token은 메모리(Zustand, 새로고침 시 소실)에만 보관 + Refresh Token은 httpOnly 쿠키에 저장 - XSS로 인한 토큰 탈취 방지, 새로고침 시 `/api/auth/refresh` 호출로 Access Token 복구

B) Access+Refresh 모두 localStorage에 저장 - 구현이 단순하고 새로고침 후 즉시 복구 가능하나 XSS 발생 시 탈취 위험

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 4 (Data Flow / WebSocket 연결 생명주기)

STOMP WebSocket 연결을 언제 맺고 끊을까요?

A) 로그인 성공 시 앱 전역에서 연결 1개를 유지, 채널/DM 전환 시 구독(subscribe)만 교체 - 전환이 빠르고 연결 오버헤드가 적음

B) 채널/DM 화면에 진입할 때마다 새로 연결하고, 화면을 벗어나면 연결 종료

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 5 (Business Scenarios / 재연결·오프라인 처리)

네트워크 문제로 WebSocket 연결이 끊기면 어떻게 처리할까요?

A) 지수 백오프로 자동 재연결 시도 + 재연결 성공 시 마지막으로 수신한 메시지 이후 이력을 REST API(`GET /api/channels/{id}/messages`)로 재조회해 누락분 보완

B) "재연결 중" 배너만 표시 - 누락된 메시지는 사용자가 화면을 새로고침해야 확인 가능

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 6 (Business Rules / 클라이언트 검증 범위)

클라이언트 측 입력 검증은 어느 수준까지 할까요?

A) 백엔드와 동일한 규칙만 선반영 (이메일 형식, 필수 필드, 최소 글자수) - 그 외 에러는 API 응답의 errorCode를 그대로 표시

B) 백엔드 규칙 + 추가 UX 검증까지 포함 (예: 비밀번호 강도 실시간 표시, 채널명 글자수 카운터, 메시지 전송 버튼 비활성화 조건)

C) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 7 (Error Handling / 에러 표시 방식)

API/WebSocket 에러를 화면에 어떻게 표시할까요?

A) 공통 토스트(Toast) 알림 - 모든 에러를 동일한 방식으로 화면 한쪽에 짧게 표시

B) 컨텍스트별 인라인 표시 - 로그인 폼 에러는 폼 아래에, 메시지 전송 실패는 해당 메시지 옆에 표시하는 등 위치를 상황별로 다르게 함

C) Other (please describe after [Answer]: tag below)

[Answer]: A
