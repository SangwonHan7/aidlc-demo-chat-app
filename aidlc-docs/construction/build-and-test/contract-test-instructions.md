# Contract Test Instructions & Results - QuickChat

## Purpose
Backend(REST + STOMP)와 Frontend(axios/STOMP 클라이언트)는 서로 다른 Code Generation 단계에서 독립적으로 생성되었고, 실제로 함께 기동해 통신한 적이 없다(Frontend Code Generation Plan의 명시적 전제: "실제 기동/통합 검증은 Build and Test 단계에서 수행"). 다른 시나리오(integration-test-instructions.md)는 실제 실행이 필요하지만, API 경로/HTTP 메서드/요청·응답 필드명 같은 **계약 형태 불일치**는 코드를 정적으로 대조하는 것만으로도 상당 부분 찾아낼 수 있다. 이 문서는 그 정적 계약 감사를 실제로 수행한 결과다(instructions 문서이자 이미 실행된 결과 보고서).

## 감사 방법
1. Backend의 모든 `@RestController`(`AuthController`, `ChannelController`, `MessageController`, `PresenceController`, `UserController`)와 `web/dto/*.java` 전체, WebSocket(`WebSocketConfig`, `StompAuthChannelInterceptor`, `RedisBroadcastListener`, `SendMessageStompRequest`, `ChatMessageEvent`)를 읽어 "메서드+경로 -> 요청 필드 -> 응답 필드" 인벤토리 작성
2. Frontend의 `lib/apiClient.ts`, `lib/stompClient.ts`, `store/*.ts`, 그리고 컴포넌트에서 `apiClient.*`를 직접 호출하는 모든 지점(레포 전체 grep으로 12개 파일 확인, 스토어를 거치지 않는 우회 호출 없음 확인)을 대조
3. 에러 응답 형태(`GlobalExceptionHandler` -> `{errorCode, message}`)와 Frontend의 `ApiError`/`errorMessages.ts` 매핑을 12개 에러코드 전부 대조

## 실행 결과 (2026-08-20, 서브에이전트로 실행)

### REST 엔드포인트: 전부 일치 (OK)
AuthController(register/login/refresh), ChannelController(create/direct/list/discoverable/join/members POST·GET·DELETE), MessageController(history, cursor 페이지네이션), PresenceController(`GET /api/presence?userIds=`), UserController(`me`/`ids`/`search`) - 요청 바디 필드명, 응답 DTO 필드명, 쿼리 파라미터 이름과 형식(콤마 구분 리스트가 Spring의 `StringToCollectionConverter`로 정상 바인딩되는 것 포함) 전부 일치.

### 에러 계약: 전부 일치 (OK)
`ErrorResponse{errorCode,message}` ↔ Frontend `ApiError` 정확히 일치. 12개 에러코드(ACCOUNT_LOCKED, ALREADY_MEMBER, CHANNEL_ARCHIVED, CHANNEL_NOT_FOUND, EMAIL_ALREADY_EXISTS, FORBIDDEN_ACTION, INVALID_CREDENTIALS, NOT_A_MEMBER, RATE_LIMITED, USER_NOT_FOUND, VALIDATION_ERROR, INTERNAL_ERROR) 전부 Frontend `errorMessages.ts`에 빠짐없이 매핑됨.

### WebSocket 계약: 발견된 결함 1건 (수정 완료)
- CONNECT 인증 헤더(`Authorization: Bearer ...`), `SEND /app/chat.send/{channelId}` 바디(`{content}`), `SUBSCRIBE /topic/channel/{channelId}` 경로 자체는 일치
- **불일치(수정 전)**: 브로드캐스트 바디가 내부 Kafka 이벤트(`ChatMessageEvent`, 필드명 `messageId`)를 그대로 내보내고 있어 Frontend가 기대하는 `id` 필드가 없었음. `chatStore.mergeIncomingMessage`가 `undefined === undefined`로 오인해, 같은 채널의 두 번째 실시간 메시지부터 화면에서 사라지는 결함으로 이어짐(REST 이력 조회는 영향 없음 - 그쪽은 원래부터 `id`를 올바르게 사용)
- **처리**: Post-Approval Patch 5로 즉시 수정 - `MessageResponse.from(ChatMessageEvent)` 팩토리를 추가해 `RedisBroadcastListener`가 REST 이력 조회와 동일한 `{id,channelId,senderId,content,sentAt}` 형태로 브로드캐스트하도록 변경. 회귀 테스트 `RedisBroadcastListenerTest` 추가. 상세: `api-layer-summary.md` Patch 5
- **비고(계약 불일치는 아니지만 함께 발견)**: `ChatFacadeService.sendMessage()`(WebSocket SEND 경로)에서 던질 수 있는 4개 예외(RATE_LIMITED, CHANNEL_ARCHIVED, NOT_A_MEMBER, CHANNEL_NOT_FOUND)를 위한 전용 STOMP 에러 큐/`@MessageExceptionHandler`가 없음 - REST 경로였다면 `{errorCode,message}`로 보였을 에러가 WebSocket 경로에서는 사용자에게 전혀 보이지 않는다. 이건 "계약이 어긋난 것"이 아니라 "계약 자체가 없는 것"이라 이 문서의 판정 대상은 아니지만, integration-test-instructions.md Scenario 4와 함께 실사용 시나리오로 확인 권장

## 감사 범위
Backend 26개 파일(controller/dto/websocket/kafka/exception/security/service 핵심 경로), Frontend 21개 파일(lib/store/types + API를 직접 호출하는 모든 컴포넌트) - 상세 파일 목록은 이 문서 생성에 사용된 서브에이전트 결과에 기록되어 있으며, 요약하면 두 유닛의 통신 관련 코드 전체가 대상이었다(표본이 아니라 전체 커버리지).

## 알려진 제약 (투명성 목적)
이 감사는 **정적 코드 대조**이며, 실제로 두 서비스를 함께 띄워 확인한 것이 아니다. 다음은 정적 대조로는 확인할 수 없어 integration-test-instructions.md의 실제 실행이 필요한 항목:
- Jackson의 실제 직렬화/역직렬화 런타임 동작(예: `Instant`의 ISO-8601 문자열 형식이 Frontend의 `sentAt: string` 파싱/정렬 로직과 실제로 호환되는지)
- STOMP 프레임의 실제 타이밍/순서 보장
- 이번에 수정한 Patch 5(WebSocket 필드명)와 Patch 6의 H2(SUBSCRIBE 인가)가 실제 연결에서 의도대로 동작하는지
