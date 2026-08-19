# Tech Stack Decisions - Frontend Unit

| Decision Area | Choice | Rationale |
|---|---|---|
| 렌더링 전략 | Next.js App Router, 전체 클라이언트 컴포넌트("use client") | SEO가 불필요한 내부 도구, Zustand/WebSocket 상태 관리 단순화 (Q1 답변 A) |
| Property-Based Testing 프레임워크 | fast-check | Vitest 통합이 쉬운 TS/JS 생태계 표준, PBT Full Enforcement 요구 충족 (Q2 답변 A, PBT-09 상당) |
| 성능 목표 | 정량적 기준 없음, 실습 데모 수준 | Q3 답변 A |
| 에러 모니터링 | 브라우저 콘솔 + React Error Boundary | 추가 도구 도입 없이 최소 방어, Resiliency Baseline 미적용과 일관 (Q4 답변 A) |
| 스타일링 | Tailwind CSS | 유틸리티 클래스 기반 빠른 개발, tech-env.md에 미지정이던 항목 확정 (Q5 답변 A) |
| HTTP 클라이언트 | axios | interceptor로 401 감지 + 자동 refresh 흐름을 간결하게 구현 (Q6 답변 B) |
| WebSocket 클라이언트 | `@stomp/stompjs` | STOMP 표준 클라이언트, Backend `WebSocketConfig`(`/ws`, `/app`, `/topic`)와 직접 호환 (Q6 답변 B) |
| 접근성/반응형 범위 | 데스크톱 우선, 최소 키보드 네비게이션 | Q7 답변 A |
| 빌드 도구 | Vite (Next.js 내부) | tech-env.md 표준 |
| 전역 상태 관리 | Zustand, 도메인별 3개 스토어(auth/chat/presence) | tech-env.md 표준 + Functional Design Question 2 답변 B |

## PBT-09 상당 준수 (JS/TS Property-Based Testing 프레임워크)
`fast-check`을 devDependencies에 포함하고 Vitest 테스트 러너와 통합한다. `fc.assert(fc.property(...))` 패턴으로 커스텀 arbitrary(유효한 이메일 문자열, UUID, 메시지 배열 등)를 사용하며, 실패 시 자동 shrinking과 seed 기반 재현을 지원한다 (jqwik이 Backend에서 제공하는 것과 동등한 보장).

## 헬스체크 엔드포인트 (Availability - Question 4 답변과 상충하지 않는 최소 인프라 훅)
`/api/health` (Next.js Route Handler)를 추가해 200 OK를 고정 응답한다. 이는 APM/모니터링 도구가 아니라 k8s liveness/readiness probe가 서버 프로세스 생존 여부만 확인하기 위한 최소 엔드포인트다. 실제 probe 설정(주기, 실패 임계치)은 Infrastructure Design 단계에서 Backend의 `/actuator/health` 패턴과 함께 확정한다.
