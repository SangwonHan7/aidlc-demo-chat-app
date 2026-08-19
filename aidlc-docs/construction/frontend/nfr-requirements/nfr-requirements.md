# NFR Requirements - Frontend Unit

## Performance
- 정량적 성능 기준(Lighthouse 점수 등)은 설정하지 않음 - 실습 데모 수준 목표 유지 (Question 3 답변 A)
- 체감 목표: 메시지 전송 후 500ms 이내 화면 반영 (requirements.md FR-2) - Backend와 공유하는 목표이며, Frontend 자체 렌더링 오버헤드도 이 예산 안에 포함해야 함
- 렌더링 전략: Next.js App Router, 전체 클라이언트 컴포넌트("use client") - 서버 렌더링 비용 없이 SPA처럼 동작 (Question 1 답변 A)

## Scalability
- Frontend는 상태 없는(stateless) HTTP/Node 서버 - 대화/세션 상태는 전부 클라이언트(브라우저) 또는 Backend/Redis에 있음
- 레플리카 수 등 배포 스케일링 결정은 Backend NFR Design과 동일한 패턴으로 Frontend NFR Design 단계에서 확정 (이번 단계 범위 아님)

## Availability
- 목표 가동률: Backend NFR Requirements와 동일한 99.5%(실습 기간) 목표 적용
- k8s 배포 시 liveness/readiness probe용 최소 헬스체크 엔드포인트가 필요함 - Question 4(에러 모니터링 도구 미도입)와 배치되지 않는 최소한의 인프라 훅으로 취급 (tech-stack-decisions.md 참고), 실제 probe 설정은 Infrastructure Design 단계에서 확정

## Security
- Access/Refresh Token은 localStorage에 저장 (Functional Design business-rules.md, Contradiction 1 해결 결과를 그대로 계승) - XSS 발생 시 탈취 위험을 감수한다는 결정 유지
- React는 JSX 렌더링 시 값을 기본적으로 이스케이프하므로, `dangerouslySetInnerHTML`을 사용하지 않는 한 메시지 본문의 XSS 방지(tech-env.md 요구사항)는 기본적으로 충족됨 - Code Generation 단계에서 이 금지 규칙을 코드 주석/컨벤션으로 명시
- Security Baseline Extension 미적용 (requirements.md Q5=B) - CSP 헤더, Subresource Integrity 등 고급 보안 헤더 설정은 범위 밖

## Reliability and Observability
- 별도 APM/에러 트래킹 도구를 도입하지 않음 - 브라우저 콘솔 로그 + 최상위 React Error Boundary로 렌더링 오류 시 화면 전체가 깨지지 않도록만 방어 (Question 4 답변 A)
- Resiliency Baseline Extension 미적용 (requirements.md Q6=B) - 고급 장애복구/DR 설계는 범위 밖

## Testability
- Vitest(예시 기반 테스트) + fast-check(Property-Based Testing, Full Enforcement) 조합 (Question 2 답변 A, PBT-09 상당)
- business-logic-model.md에서 식별한 PBT-01 관점 7개 속성을 fast-check로 구현

## Maintainability
- 스타일링: Tailwind CSS (Question 5 답변 A)
- HTTP/WebSocket 클라이언트: axios + `@stomp/stompjs` (Question 6 답변 B) - axios interceptor로 401 자동 재발급(business-rules.md 토큰 정책) 구현

## Usability
- 데스크톱 웹 브라우저 우선, 최소 키보드 네비게이션(폼 제출, 포커스 이동)만 지원 - 모바일 반응형과 스크린리더 최적화는 범위 밖 (Question 7 답변 A)
