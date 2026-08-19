# NFR Requirements Plan - Frontend Unit

## Plan

- [x] requirements.md/tech-env.md의 성능·가용성·보안 목표를 Frontend NFR로 구체화
- [x] 아래 질문 답변을 반영해 렌더링 전략/스타일링/HTTP·WS 클라이언트/PBT 프레임워크 확정
- [x] nfr-requirements.md 작성
- [x] tech-stack-decisions.md 작성 (PBT-09 상당 - JS/TS PBT 프레임워크 선택 포함)

## Questions

각 질문에 알파벳으로 답한 뒤 채팅으로 "답변 완료"라고 알려주세요.

## Question 1 (Tech Stack Selection / 렌더링 전략)

tech-env.md는 Next.js 14.x(React 18)만 지정했고 렌더링 전략은 정해지지 않았습니다. 인증이 필요한 내부 팀 도구 특성상 SEO는 중요하지 않습니다.

A) App Router + 전부 클라이언트 컴포넌트("use client") - SPA처럼 동작, 서버 렌더링 없음. 상태 관리(Zustand)와 WebSocket 연결을 다루기 단순함

B) App Router + 일부 서버 컴포넌트 혼용 - 로그인 화면 등 초기 페이지는 서버에서 렌더링, 나머지는 클라이언트 컴포넌트

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 2 (Tech Stack - PBT Framework)

Property-Based Testing이 Full Enforcement로 결정되어 있습니다(requirements.md Q7=A). Backend는 jqwik을 선택했는데, Frontend(TypeScript)는 무엇을 사용할까요?

A) fast-check - Vitest와 통합이 쉬운 TS/JS 진영 표준 PBT 라이브러리, 자동 shrinking 지원

B) 별도 라이브러리 없이 Vitest 내에서 직접 랜덤 입력 생성 헬퍼를 작성 - shrinking/재현성(PBT-08)은 자체 구현해야 하고 생태계 표준은 아님

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3 (Performance)

프론트엔드의 성능 목표를 어느 수준으로 정할까요?

A) 실습 데모 수준 - 별도의 정량적 성능 기준(Lighthouse 점수 등) 없이, requirements.md의 메시지 전달 500ms 체감 목표만 유지

B) 정량적 기준 설정 - 초기 로드 3초 이내, Lighthouse Performance 90점 이상 등 명시적 기준 적용 및 측정

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 4 (Reliability / Observability)

프론트엔드 런타임 에러(렌더링 오류, 처리되지 않은 예외 등)를 어떻게 모니터링할까요?

A) 별도 모니터링 도구 없음 - 브라우저 콘솔 로그 + React Error Boundary로 화면 전체가 깨지지 않도록만 방어 (실습 데모 범위에 맞게 최소화)

B) 간단한 에러 로깅 API 추가 - 클라이언트 에러를 Backend에 전송해 서버 로그에서 확인 가능하게 함 (Backend에 신규 엔드포인트 추가 필요)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 5 (Maintainability / Tech Stack - 스타일링)

tech-env.md에 UI 스타일링 라이브러리가 명시되어 있지 않습니다. 무엇을 사용할까요?

A) Tailwind CSS - 유틸리티 클래스 기반, 별도 CSS 파일 최소화, 빠른 개발

B) CSS Modules - 컴포넌트별 scoped CSS, 추가 빌드 설정 불필요

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 6 (Tech Stack - HTTP/WebSocket 클라이언트)

REST API 호출과 STOMP 연결에 어떤 라이브러리를 사용할까요?

A) 브라우저 기본 `fetch` + `@stomp/stompjs`(STOMP 표준 클라이언트) - 추가 런타임 의존성 최소화

B) `axios` + `@stomp/stompjs` - axios의 interceptor로 401 감지/자동 재발급(business-rules.md의 토큰 재발급 정책) 로직을 더 간결하게 구현

C) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 7 (Usability)

접근성/반응형 지원 범위를 어디까지로 할까요?

A) 데스크톱 웹 브라우저 우선 - 최소 키보드 네비게이션(폼 제출, 포커스 이동)만 지원, 모바일 반응형과 스크린리더 최적화는 범위 밖

B) 반응형(모바일 브레이크포인트) + 기본 ARIA 라벨까지 포함

C) Other (please describe after [Answer]: tag below)

[Answer]: A
