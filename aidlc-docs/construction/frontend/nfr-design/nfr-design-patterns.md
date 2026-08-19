# NFR Design Patterns - Frontend Unit

## Resilience Pattern: 자동 재시도 없음
- REST 요청은 실패 시 자동 재시도를 하지 않는다 - 성공/실패와 무관하게 즉시 결과를 반영하고, 실패 시 인라인 에러 + 재시도 버튼(Functional Design `business-rules.md`의 에러 표시 정책)으로 사용자가 직접 재시도한다 (Question 1 답변 B)
- WebSocket 재연결(지수 백오프)은 Functional Design 단계에서 이미 결정된 별도 정책이며 이 패턴과 무관하게 그대로 유지된다

## Scalability Pattern: 고정 레플리카
- HPA(오토스케일링) 미적용, Backend와 동일하게 고정 레플리카 수로 운영 (Question 2 답변 A)
- 정확한 레플리카 수는 Infrastructure Design 단계에서 확정 (Backend가 Infrastructure Design에서 NAS 실사양 확인 후 결정한 것과 동일한 순서)

## Performance Pattern: 메시지 목록 가상화
- `MessageList` 컴포넌트에 `react-window`를 적용해 화면에 보이는 부분만 렌더링 (Question 3 답변 A)
- 무한 스크롤(cursor 기반 이력 로드, Functional Design Story 1.5 흐름)과 함께 동작: 스크롤 위치 유지, 이전 페이지 prepend 시 가상화 리스트의 오프셋 재계산이 필요 - 구체적 구현은 Code Generation 단계에서 처리

## Security Pattern: CORS 명시적 허용
- Frontend와 Backend는 서로 다른 origin에 배포된다 (Question 4 답변 B)
- Backend `SecurityConfig`에 `CorsConfigurationSource`를 추가해 허용 origin(`quickchat.cors.allowed-origin` / 환경변수 `CORS_ALLOWED_ORIGIN`)과 메서드(GET/POST/PUT/DELETE/OPTIONS), 헤더(Authorization, Content-Type)를 명시적으로 열었다
- 인증은 Bearer 토큰(Authorization 헤더)만 사용하므로 credentials(쿠키) 허용은 설정하지 않음 - Frontend Functional Design Contradiction 1(토큰을 localStorage에 저장, 쿠키 미사용) 결정과 일관됨
- WebSocket(`WebSocketConfig`)은 이미 `setAllowedOriginPatterns("*")`로 모든 origin에 열려 있었음 - REST는 이보다 엄격하게 특정 origin만 허용 (REST가 더 민감한 CRUD 작업을 다루므로)

## 알려진 제약 (투명성 목적)
- 실제 브라우저 preflight(OPTIONS) 동작과 CORS 헤더 조합은 단위 테스트로 검증하지 않음 - `StompAuthChannelInterceptor`와 마찬가지로 통합/E2E 수준의 검증이 필요해 Build and Test 단계로 이동
- WebSocket의 `setAllowedOriginPatterns("*")`는 이번 라운드에서 특정 origin으로 좁히지 않음 (범위 밖) - Build and Test 또는 이후 보안 강화 라운드에서 REST와 동일하게 좁히는 것을 검토 권장

## PBT 관점 추가 없음
이번 NFR Design 단계에서 식별된 패턴(재시도 없음, 고정 레플리카, 가상화, CORS)은 배포/설정 성격이 강해 business-logic-model.md의 PBT-01 목록에 추가할 새로운 속성은 없다. 가상화 리스트의 스크롤 위치 보존 로직이 순수 함수로 분리 가능하다면 Code Generation 단계에서 속성 후보로 재검토한다.
