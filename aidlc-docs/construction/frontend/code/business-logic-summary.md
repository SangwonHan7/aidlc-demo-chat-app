# Business Logic Summary - Frontend

## 생성된 코드
- Types: `src/types/domain.ts` (domain-entities.md의 타입 전부)
- Store(Zustand): `src/store/authStore.ts`, `src/store/chatStore.ts`, `src/store/presenceStore.ts`
- Lib: `src/lib/apiClient.ts`(axios + 401 자동 재발급 인터셉터), `src/lib/stompClient.ts`(STOMP 싱글턴 + 지수 백오프 재연결), `src/lib/backoff.ts`, `src/lib/validation.ts`, `src/lib/errorMessages.ts`

## 설계 결정 반영
- 토큰은 JSON 응답 그대로 localStorage에 저장 (Functional Design Contradiction 1 해결)
- 401 발생 시 `apiClient`의 응답 인터셉터가 `authStore`에 등록된 refresh 핸들러를 통해 1회 자동 재발급, 동시 요청은 in-flight promise로 공유 (business-rules.md)
- STOMP 연결은 앱 전역 싱글턴(`stompClient.ts`)으로 관리, 채널 전환 시 구독만 교체, 끊기면 `backoff.ts`의 지수 백오프로 재연결 (Question 4/5 답변 A)
- `apiClient`/`stompClient`는 각각 `NEXT_PUBLIC_API_BASE_URL`, `NEXT_PUBLIC_WS_URL` 환경변수로 Backend origin을 주입받음 (Infrastructure Design 결정)

## PBT Compliance (Property-Based Testing, Full Enforcement)

| Rule | 상태 | 비고 |
|---|---|---|
| PBT-01 (속성 식별) | Compliant | business-logic-model.md에 7개 속성 문서화 |
| PBT-02 (Round-trip) | Compliant | `prependHistoryPage`의 페이지 합집합 무결성 - `chatStore.property.test.ts` |
| PBT-03 (Invariant) | Compliant | 이력 정렬 순서, 토큰 갱신 불변식, 백오프 상한 - `chatStore`/`authStore`/`backoff` property test |
| PBT-04 (Idempotence) | Compliant | 메시지 병합, presence 병합 - `chatStore`/`presenceStore` property test |
| PBT-05 (Oracle) | Partial | 이메일 검증은 fast-check의 독립 생성기(`fc.emailAddress()`)와 비교(진짜 오라클). 비밀번호/표시이름은 Backend Hibernate Validator를 JS에서 재현할 수 없어 문서화된 길이 불변식 검증으로 대체 - `validation.property.test.ts`의 상단 주석에 명시 |
| PBT-06 (Stateful) | Partial | Zustand 스토어의 상태 전이는 순수 함수로 분리해 예시+속성 테스트로 다뤘으나, 정식 커맨드 시퀀스 기반 stateful PBT는 미적용 - Build and Test 단계에서 통합 테스트로 보완 권장 |
| PBT-07 (생성기 품질) | Compliant | `fc.uuid()`, `fc.emailAddress()` 등 도메인에 맞는 생성기 사용 |
| PBT-08 (Shrinking/재현성) | Compliant | fast-check 기본 shrinking에 위임, CI 시드 고정 정책은 Build and Test 단계에서 확정 |
| PBT-09 (프레임워크) | Compliant | fast-check (frontend-nfr-requirements/tech-stack-decisions.md 참고), Vitest와 통합 |
| PBT-10 (상호 보완) | Compliant | 7개 속성 테스트 모두 대응하는 example-based 테스트(`*.test.ts`)와 짝을 이룸 |

### 블로킹 파인딩 없음이지만 명시적으로 범위를 좁힌 항목 (투명성 목적)
- STOMP 재연결/구독 전환 로직(`stompClient.ts`) 자체는 실제 WebSocket 서버가 필요해 단위 테스트로 다루지 않음 - Build and Test 단계의 통합 테스트로 이동 (Backend의 `StompAuthChannelInterceptor`와 동일한 성격의 제약)
- `apiClient.ts`의 401 인터셉터/in-flight 캐싱은 실제 axios 요청 흐름을 모킹한 통합 테스트가 필요해 이번 라운드에서는 로직을 pure function(`nextAuthState`)으로 분리해 그 부분만 검증했고, 인터셉터 자체의 동작은 Build and Test로 이동
