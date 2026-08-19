# Frontend Functional Design - Clarification Questions

frontend-functional-design-plan.md 답변을 검토한 결과, Question 3 답변과 이미 승인된 Backend 구현 사이에 모순이 발견되어 진행 전에 확인이 필요합니다.

## Contradiction 1: Refresh Token 저장 방식 (Q3=A) vs 이미 구현된 Backend Auth API

Q3에서 "A) Access Token은 메모리 보관 + Refresh Token은 httpOnly 쿠키에 저장"을 선택하셨습니다.

하지만 이미 승인되어 생성된 Backend 코드는 다음과 같이 동작합니다 (`AuthController.java`, `TokenResponse.java`, `SecurityConfig.java`):

- `/api/auth/login`, `/api/auth/refresh`는 accessToken과 refreshToken을 **둘 다 JSON 응답 본문**으로 반환합니다 (`TokenResponse(accessToken, refreshToken)`). Set-Cookie로 내려주는 로직이 없습니다.
- `/api/auth/refresh`는 쿠키가 아니라 **요청 본문의 `refreshToken` 필드**(`RefreshRequest`)를 읽습니다.
- `SecurityConfig`는 stateless bearer-token 방식을 전제로 **CSRF 보호를 비활성화**한 상태입니다. 쿠키 기반 인증을 실제로 쓰려면 CSRF 보호를 다시 검토해야 합니다.

브라우저 JavaScript는 httpOnly 쿠키를 직접 생성할 수 없으므로(그것이 httpOnly의 목적), Frontend 코드만 작성해서는 Q3=A를 그대로 구현할 수 없습니다. Backend를 수정하거나, Frontend의 저장 방식을 현재 Backend 계약에 맞게 조정해야 합니다.

### Clarification Question 1

어떻게 처리할까요?

A) 현재 Backend 계약 유지 + localStorage 저장 - Access/Refresh Token 모두 JSON으로 받아 localStorage에 저장 (Q3 답변을 B로 변경하는 것과 동일). Backend 수정 없음. Security Baseline 확장이 이번 프로젝트에서 미적용(Requirements Analysis Q5=B)이라는 결정과 일관됩니다. XSS 발생 시 두 토큰 모두 탈취 위험이 있습니다.

B) 현재 Backend 계약 유지 + 메모리만 사용 - Access/Refresh Token 모두 JSON으로 받지만 Zustand 메모리에만 보관하고 localStorage에는 저장하지 않음. Backend 수정 없음. 탈취 위험은 최소화되지만 새로고침 시 세션이 소실되어 재로그인이 필요합니다.

C) Backend 수정 (httpOnly 쿠키로 전환) - 원래 Q3=A 의도대로 `/login`, `/refresh` 응답을 Set-Cookie(HttpOnly, Secure, SameSite)로 변경하고, `/refresh`가 쿠키에서 읽도록 수정, CSRF 보호 재검토. 이미 승인된 Backend Code Generation 산출물 일부를 재작업해야 합니다.

D) Other (please describe after [Answer]: tag below)

[Answer]: A

**해결됨**: 현재 Backend 계약(JSON 응답으로 두 토큰 모두 반환)을 유지하고, Frontend는 이를 localStorage에 저장합니다. Backend 코드 변경 없음. frontend-functional-design-plan.md Q3의 실제 구현 방향은 "B(로컬스토리지)"로 확정합니다.

---

## Gap 2: Presence(온라인 상태) API가 Backend에 실제로 연결되어 있지 않음

unit-of-work-story-map.md는 Story 1.4(온라인 상태 확인)에 대해 Backend가 "Presence API"를 제공한다고 되어 있습니다. 실제 생성된 Backend 코드를 다시 확인한 결과:

- `PresenceService`(markOnline/markOffline/isOnline)와 `PresenceRedisService`는 존재합니다.
- 하지만 이를 호출하는 REST 컨트롤러나 WebSocket 연결/해제 이벤트 리스너가 전혀 없습니다 (`PresenceService`를 참조하는 코드는 자기 자신과 `PresenceRedisService` 외에는 없음, `StompAuthChannelInterceptor`는 CONNECT 인증만 하고 markOnline을 호출하지 않음).
- 즉 지금 상태로는 어떤 사용자도 "온라인"으로 표시될 트리거가 없고, 다른 사용자의 상태를 조회할 API도 없습니다. Story 1.4의 인수 조건("사용자 목록/DM 화면에서 온라인/오프라인 상태가 표시된다")을 충족할 방법이 없습니다.

이건 Backend Code Generation 단계에서 놓친 부분으로 보입니다(business-logic-summary.md/api-layer-summary.md에도 이 누락이 명시적으로 기록되어 있지 않았습니다).

### Clarification Question 2

어떻게 처리할까요?

A) Backend에 최소 기능 추가 - `StompAuthChannelInterceptor`가 CONNECT 시 `markOnline`, DISCONNECT 시 `markOffline`을 호출하도록 확장하고, 다른 사용자 상태를 조회하는 짧은 REST 엔드포인트(예: `GET /api/presence?userIds=`)를 추가합니다. 이미 승인된 Backend Code Generation 산출물에 소규모 추가/보완이 필요합니다.

B) 이번 범위에서 온라인 상태 기능을 제외 - Story 1.4는 Frontend Functional Design/Code Generation 범위에서 제외하고 향후 별도 작업으로 미룹니다 (frontend-components.md에 명시적 gap으로 기록).

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

답변 후 채팅으로 "답변 완료"라고 알려주세요.
