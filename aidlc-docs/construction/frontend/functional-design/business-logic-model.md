# Business Logic Model - Frontend Unit

## 화면별 데이터 흐름 워크플로우

### 1. 로그인 흐름 (Story 1.1)
1. `LoginForm` 제출 → `POST /api/auth/login`
2. 성공: `TokenResponse` → localStorage 저장 + `useAuthStore` 갱신 → STOMP 연결 수립 → `GET /api/channels` 호출해 사이드바 채널 목록 로드 → 앱쉘(`/`)로 이동
3. 실패: 폼 하단에 인라인 에러 표시 (계정 잠금 시 `AccountLockedException`의 errorCode 문구)

### 2. 채널/DM 전환 흐름 (Story 1.2, 1.3)
1. `Sidebar`에서 채널/DM 클릭 → `useChatStore.activeChannelId` 갱신
2. 해당 채널의 메시지가 캐시에 없으면 `GET /api/channels/{id}/messages`(최근 50개, `before` 없음) 호출
3. STOMP 구독 전환: 이전 채널 구독 해지 → 신규 채널 `/topic/channel/{channelId}` 구독
4. 화면에 보이는 멤버의 presence 즉시 조회 + 30초 polling 시작 (business-rules.md 참고)

### 3. 메시지 전송/수신 흐름 (Story 1.2, 1.3)
1. `MessageInput`에서 전송 → WS SEND `/app/chat.send/{channelId}`
2. 서버가 저장 + Kafka 발행 → Redis Pub/Sub → 모든 구독자(발신자 포함)에게 `/topic/channel/{channelId}`로 브로드캐스트
3. 클라이언트는 브로드캐스트 수신 시점에만 `messagesByChannel`에 추가 (낙관적 업데이트 미적용, business-rules.md 참고)
4. 재연결 시에는 REST 이력 재조회로 갭을 보완하고 `id` 기준 중복 제거

### 4. 메시지 이력 무한 스크롤 흐름 (Story 1.5)
1. 채널 진입 시 최근 50개 로드
2. 사용자가 `MessageList` 최상단으로 스크롤 → `nextCursor`를 `before` 파라미터로 재호출 → 기존 목록 앞에 prepend
3. `nextCursor`가 `null`이면 이력 끝으로 간주하고 추가 요청을 중단

### 5. 채널 생성/초대/제외 흐름 (Story 2.1, 2.2)
1. `CreateChannelModal` 제출 → `POST /api/channels` → 성공 시 채널 목록에 추가 + 자동 선택
2. `MemberManagementPanel`에서 사용자 검색 후 초대 → `POST /api/channels/{id}/members` (OWNER가 아니면 UI에서 버튼 비노출 - 서버도 `ForbiddenActionException`으로 방어)
3. 멤버 제외 → `DELETE /api/channels/{id}/members/{userId}`

### 6. 재연결/오프라인 흐름 (Story 1.2, 1.3 연속성)
연결 끊김 감지 → "연결이 끊겼습니다. 재연결 중..." 배너 표시 → 지수 백오프 재시도 → 성공 시 배너 제거 + 활성 채널 이력 재조회로 갭 보완 → Access Token 만료가 원인이면 `/api/auth/refresh` 선행 후 재연결 (business-rules.md 참고)

### 7. 온라인 상태 흐름 (Story 1.4, Gap 2 해결)
화면 진입 시 보이는 userId 목록으로 `GET /api/presence?userIds=` 즉시 호출 → `usePresenceStore.onlineUserIds` 갱신 → `PresenceIndicator`가 해당 상태를 점(dot) 색상으로 표시 → 30초 간격 재호출 → 화면 이탈 시 polling 중단

## PBT-01 Testable Properties (Property-Based Testing, Full Enforcement)

프레임워크 선택(예상: fast-check, JS/TS 표준 PBT 라이브러리)은 NFR Requirements 단계에서 확정한다. 여기서는 기술 독립적으로 "무엇을 속성으로 검증할지"만 식별한다. 대상은 순수 함수/상태 전이 로직(스토어 리듀서, 캐시 병합, cursor 병합, 백오프 계산)이며, 실제 네트워크/WebSocket I/O는 목(mock) 처리 대상으로 제외한다.

| # | 대상 | 속성 유형 | 속성 설명 |
|---|---|---|---|
| 1 | useChatStore 메시지 병합 | Idempotence | 동일한 메시지(같은 id)를 여러 번 반영해도 `messagesByChannel`의 최종 목록에는 정확히 1개만 존재한다 (재연결 시 중복 수신 대비 de-dup) |
| 2 | useChatStore 이력 prepend | Invariant | 무한 스크롤로 이전 페이지를 prepend해도 메시지 배열의 `sentAt` 순서는 항상 오름차순을 유지한다 |
| 3 | useChatStore cursor 병합 | Round-trip | 최신 페이지 이후 이전 페이지를 연속 조회해 합친 목록에는 두 페이지의 메시지 id 합집합이 정확히 한 번씩만 존재한다 (누락/중복 없음) |
| 4 | usePresenceStore polling 병합 | Idempotence | 동일한 presence 응답을 여러 번 반영해도 `onlineUserIds` 집합의 최종 상태는 동일하게 유지된다 |
| 5 | useAuthStore 토큰 갱신 | Invariant | `refreshAccessToken` 성공 후에는 항상 유효한 accessToken이 저장되어 있고, 실패 시에는 명시적 로그아웃 전까지 기존 토큰을 지우지 않는다 |
| 6 | 클라이언트 입력 검증 함수 (이메일/비밀번호/displayName) | Oracle | 동일 입력에 대해 클라이언트 검증 결과는 Backend Bean Validation 규칙(business-rules.md에 명시된 동일 조건식)과 항상 같은 방향으로 판정한다 - 클라이언트가 통과시켰는데 서버가 거부하는 케이스는 없어야 한다 |
| 7 | 지수 백오프 계산 함수 | Invariant | n번째 재시도 대기시간은 이전 시도보다 크거나 같고, 상한(30s)을 넘지 않는다 |

Commutativity, Induction, Easy Verification 카테고리는 화면/상태 전이 로직의 특성상 마땅한 대상이 없어 해당 없음으로 표시한다 (Backend business-logic-model.md와 동일한 판단 기준).

### PBT-10 (상호 보완) 참고
위 7개 속성 테스트는 각각 대응하는 example-based 테스트와 짝을 이루도록 Code Generation 단계에서 작성한다 (Backend에서 적용한 패턴과 동일).
