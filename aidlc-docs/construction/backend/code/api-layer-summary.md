# API Layer Summary - Backend

## 생성된 코드
- Controller: AuthController, ChannelController, MessageController, PresenceController (REST), ChatWebSocketController (STOMP)
- WebSocket 지원: WebSocketConfig, StompAuthChannelInterceptor(CONNECT 프레임 JWT 인증 + CONNECT/DISCONNECT 시 Presence markOnline/markOffline 연동), RedisBroadcastListener(Redis Pub/Sub -> STOMP 브로드캐스트 릴레이)
- Security: SecurityConfig, JwtAuthenticationFilter, JwtTokenProvider
- DTO 11개(record): RegisterRequest, LoginRequest, RefreshRequest, TokenResponse, UserResponse, CreateChannelRequest, StartDirectChannelRequest, InviteMemberRequest, ChannelResponse, MessageResponse, MessagePageResponse, PresenceStatusResponse, ErrorResponse
- GlobalExceptionHandler: 모든 응답을 { "errorCode", "message" } 포맷으로 통일

## 테스트
- AuthControllerTest (MockMvc, `@WebMvcTest` + `addFilters=false`): 회원가입 성공/중복이메일/유효성오류, 로그인 토큰 응답
- PresenceControllerTest (MockMvc, 동일 패턴): 다중/단일 userId 조회 시 온라인 상태 매핑 확인
- ChannelController, MessageController, ChatWebSocketController에 대한 동일 수준의 MockMvc 테스트는 이번 라운드에서 생성하지 않음 - Build and Test 단계의 통합 테스트(실제 인증 포함)로 보완 권장

## 알려진 제약 (투명성 목적)
- WebSocket 인증(StompAuthChannelInterceptor)은 단위 테스트로 다루지 않음 - 실제 STOMP 클라이언트 연결이 필요해 Build and Test의 통합/E2E 테스트 대상. CONNECT/DISCONNECT 시 Presence 연동 로직도 동일하게 미적용 - Build and Test에서 함께 검증 필요
- Rate limiting(메시지 전송)은 ChatFacadeService 내부에 있어 API 레이어 테스트에는 포함하지 않음 (서비스 레이어에서 MessageRateLimitServicePropertyTest로 검증)

## Post-Approval Patch (2026-08-18)
Frontend Functional Design 진행 중 Story 1.4(온라인 상태)에 대한 API가 실제로는 연결되어 있지 않음을 발견 (`frontend-functional-design-clarification-questions.md` Gap 2). 사용자 선택(A)에 따라 이미 승인된 Backend Code Generation 산출물에 다음을 보완:
- `PresenceController`(`GET /api/presence?userIds=`) + `PresenceStatusResponse` DTO 추가
- `StompAuthChannelInterceptor`에 CONNECT 시 `markOnline`, DISCONNECT 시 `markOffline` 호출 추가
- `PresenceControllerTest` 추가
- 기존 `PresenceService`/`PresenceRedisService`는 변경 없음 (원래부터 존재했으나 호출하는 곳이 없었음)
- 알려진 제약: `presence:{userId}` Redis 키의 TTL(2분)을 하트비트로 갱신하는 로직은 이번 보완 범위에 포함하지 않음 - WebSocketConfig에 STOMP 하트비트 설정이 없어 장시간 연결 시 온라인 상태가 만료될 수 있음. Build and Test 단계에서 검증/보완 필요.

## Post-Approval Patch 2 (2026-08-18) - CORS
Frontend NFR Design(frontend-nfr-design-plan.md Question 4)에서 Frontend를 Backend와 다른 origin에 배포하기로 결정(답변 B). 검토 결과 `WebSocketConfig`는 `setAllowedOriginPatterns("*")`로 모든 origin을 허용하는 반면 `SecurityConfig`에는 CORS 설정이 전혀 없어 REST 호출이 브라우저에서 차단되는 상태였음. 다음을 보완:
- `SecurityConfig`에 `CorsConfigurationSource` 빈 추가 - 허용 origin(`quickchat.cors.allowed-origin`, 환경변수 `CORS_ALLOWED_ORIGIN`), 메서드(GET/POST/PUT/DELETE/OPTIONS), 헤더(Authorization, Content-Type) 명시. Credentials(쿠키)는 허용하지 않음 - Bearer 토큰만 사용하므로 불필요
- `SecurityFilterChain`에 `.cors(...)` 적용
- `application.yml`에 `quickchat.cors.allowed-origin` 프로퍼티 추가 (기본값 `http://localhost:3000`)
- 알려진 제약: 실제 preflight(OPTIONS) 동작은 단위 테스트로 검증하지 않음 - Build and Test 단계의 통합 테스트로 이동. WebSocket 쪽 `setAllowedOriginPatterns("*")`는 이번 라운드에서 좁히지 않음(범위 밖)

## Post-Approval Patch 3 (2026-08-18) - 조회성 API 3종 보완
Frontend Code Generation 중 화면(`ConversationHeader`/`MemberManagementPanel`/`ChannelList`)을 구현하면서 이미 승인된 화면 설계가 요구하는 조회 API 3개가 Backend에 없다는 것을 발견했다. 셋 다 대안이 여러 갈래로 나뉘는 설계 결정이 아니라(트레이드오프가 있던 Presence/CORS 사례와 다름) 이미 승인된 화면이 필요로 하는 데이터를 노출하는 것뿐이라고 판단해, 이번에는 사전 질문 없이 직접 보완했다:
- `ChannelController`에 `GET /api/channels/{channelId}/members` 추가 (`ChannelMemberResponse` DTO 신규) - 요청자도 해당 채널의 멤버여야 조회 가능(조회는 OWNER 전용이 아님, 초대/제외만 OWNER 전용으로 기존 규칙 유지). `ChannelService.listMembers(channelId, requesterId)` 추가. DM 상대 식별(story 1.2/1.4)과 멤버 관리 UI(story 2.2)에 필요
- 신규 `UserController`(`GET /api/users?ids=`) - `UserRepository.findAllById` 그대로 사용, 인증만 요구(추가 멤버십 검증 없음 - 이미 채널/DM으로 UUID를 알게 된 상대의 공개 프로필만 노출하므로 낮은 위험)
- `UserController`에 `GET /api/users/search?email=` 추가 - story 1.2(DM 상대 검색)/2.2(초대 대상 검색)에 필요. 정확한 이메일 일치만 지원(부분/유사 검색 없음 - 임의 사용자 열람 범위를 넓히지 않기 위한 의도적 단순화. 회원가입의 EMAIL_ALREADY_EXISTS로 이미 이메일 존재 여부가 노출되므로 새로운 정보 노출은 아님)
- `ChannelController`에 `GET /api/channels/discoverable` 추가 - 기존 `GET /api/channels`는 "내 채널"만 반환해 아직 참여하지 않은 PUBLIC 채널을 발견할 방법이 없었음(story 1.3: "공개 채널은 목록에서 바로 참여할 수 있다"). `ChannelRepository.findByTypeAndVisibility`, `ChannelService.listDiscoverablePublicChannels()` 추가
- `ChannelControllerTest`(listMembers, listDiscoverable), `UserControllerTest`(배치 조회 + 검색) 추가
- 알려진 제약: 이 엔드포인트들의 인가 경계(예: 전혀 모르는 사용자의 UUID/이메일로 프로필을 조회할 수 있는지)는 이번 데모 범위에서 더 엄격히 제한하지 않음 - 실서비스라면 검토 필요

## Post-Approval Patch 4 (2026-08-19) - 내 프로필 조회 API
Frontend `AppShellLayout`(부트스트랩) 구현 중 `useAuthStore.user`가 로그인/새로고침 후에도 항상 `null`로 남는 것을 발견했다. 원인은 "로그인/토큰 복원 후 내가 누구인지" 조회하는 API가 Backend에 전혀 없었기 때문 - `POST /api/auth/login`은 토큰만 반환하고(`TokenResponse`에 사용자 정보 없음), 새로고침 시 복원되는 것도 localStorage의 토큰뿐이다. `currentUserId`는 `ConversationView`(isOwner 판정, DM 상대 식별), `MemberManagementPanel`(자기 자신 제외), `DirectMessageList`, `Sidebar` 등 이미 승인된 화면 다수가 전제하는 값이라, 이 API 없이는 앱이 사실상 동작하지 않는다. Presence/CORS처럼 트레이드오프가 있는 설계 분기가 아니라(로그인된 사용자가 자기 자신의 프로필을 조회하는 것은 대안이 없는 기본 기능) Post-Approval Patch 3과 같은 성격으로 판단해 사전 질문 없이 직접 보완했다:
- `UserController`에 `GET /api/users/me` 추가 - `JwtAuthenticationFilter`가 `SecurityContext`에 심어둔 `Principal.getName()`(userId 문자열)을 `ChannelController.currentUserId(Principal)`과 동일한 패턴으로 사용, `UserRepository.findById` 후 없으면 기존 `UserNotFoundException`(404) 재사용
- `UserControllerTest`에 성공/404 케이스 추가
- Frontend: `authStore.loadCurrentUser()` 추가, `AppShellLayout` 부트스트랩 단계에서 세션 복원 직후 호출
- 알려진 제약: 별도 신규 예외/권한 규칙을 만들지 않고 기존 `UserNotFoundException`을 재사용함 - 토큰은 유효하지만 사용자 레코드가 삭제된 극단적 케이스에만 해당(일반 흐름에서는 발생하지 않음)

## Post-Approval Patch 5 (2026-08-20) - WebSocket 브로드캐스트 필드명 버그 (Build and Test 계약 감사에서 발견)
Build and Test 단계 진입 시 실제 실행 전에 Backend/Frontend 계약을 정적으로 전체 대조하는 감사를 수행했고, 유일한 실제 결함을 발견했다: `RedisBroadcastListener`가 내부 Kafka/Redis 전송용 `ChatMessageEvent`(필드명 `messageId`)를 변환 없이 그대로 `/topic/channel/{channelId}`로 내보내고 있었는데, Frontend는 `id` 필드를 기대한다(`ChatMessage` 타입, `chatStore.mergeIncomingMessage`). 그 결과 실시간으로 수신되는 첫 메시지는 `id: undefined`로 저장되고, 같은 채널의 이후 모든 실시간 메시지도 `undefined === undefined`로 오인되어 중복 처리되어 화면에 표시되지 않는 채로 조용히 사라지는 버그였다(새로고침 후 REST 이력 재조회로만 복구됨 - REST 쪽 `MessageResponse`는 원래부터 `id`를 올바르게 사용해 영향 없음).
- 이건 설계 트레이드오프가 아니라 두 독립적으로 생성된 코드베이스의 필드명이 실제로 어긋난 결함이라 판단해, 이전 패치들과 같은 기준(사용자 확인 없이 직접 수정)으로 즉시 수정했다.
- `MessageResponse`에 `from(ChatMessageEvent)` 팩토리를 추가해 REST 이력 조회와 WebSocket 실시간 브로드캐스트가 동일한 클라이언트 노출 형태(`{id, channelId, senderId, content, sentAt}`)를 공유하도록 통일. `RedisBroadcastListener`가 이 팩토리를 거치도록 수정
- `RedisBroadcastListenerTest` 추가 (회귀 테스트) - 이 경로는 기존에 단위 테스트가 전혀 없었음(알려진 제약에 이미 명시되어 있던 항목)
- Frontend는 수정 불필요 - `ConversationView.tsx`의 기존 캐스팅 코드가 이제는 실제로 맞는 형태를 받게 됨
- 알려진 제약: 이 수정은 정적 코드 대조로 발견한 것이라, 실제 WebSocket 연결을 통한 종단간 확인(다중 메시지 연속 수신)은 아직 하지 못함 - Build and Test의 통합/E2E 테스트로 실제 확인 필요

## Post-Approval Patch 6 (2026-08-20) - 보안 점검(source-code-security-check) HIGH/MEDIUM 2건 수정
Build and Test 단계에서 `source-code-security-check` 스킬로 전체 코드베이스 보안 점검을 실행했다(`.gstack/security-reports/cso-2026-08-20.md`). Gate 판정은 BLOCKED(standard 게이트, HIGH 3건 발견 - 2 이하만 허용)였고, 그중 두 건은 명백한 접근 제어 구현 결함이라 판단해 즉시 수정했다(세 번째 HIGH는 Spring Boot 3.3.2/Spring Framework 6.1.x의 OSS 패치 라이프사이클 관련 항목으로, 코드 수정이 아니라 향후 버전 업그레이드 계획이 필요한 사안이라 이번 라운드에서는 수정하지 않고 보고서에만 기록):
- **H1 (접근 제어 누락)**: `ChannelService.joinChannel()`이 `channel.getVisibility()`를 전혀 확인하지 않아, INVITE_ONLY 채널(그룹 채널이든 1:1 DM이든)의 UUID만 알면 초대 없이 누구나 스스로 참여할 수 있었다. 이미 승인된 규칙("초대 전용 채널은 초대를 받아야 참여할 수 있다", stories.md Story 1.3/2.1)을 어기는 구현 버그로 판단해 PUBLIC이 아니면 `ForbiddenActionException`을 던지도록 수정. `ChannelServiceTest`에 회귀 테스트 추가
- **H2 (접근 제어 누락)**: STOMP `SUBSCRIBE` 프레임에는 어떤 인가 검사도 없어(`StompAuthChannelInterceptor`는 CONNECT/DISCONNECT만 처리), 인증된 사용자라면 임의의 channelId로 `/topic/channel/{channelId}`를 구독해 자신이 속하지 않은 채널의 실시간 메시지를 그대로 엿볼 수 있었다. `StompAuthChannelInterceptor`에 SUBSCRIBE 처리 분기를 추가해 `ChannelService.requireMember`로 멤버십을 확인하도록 수정(REST 쪽은 이미 이 검사를 하고 있었음). `StompAuthChannelInterceptorTest` 신규 추가(이 클래스는 기존에 단위 테스트가 전혀 없었음)
- **M1 (신뢰 경계 불일치)**: `WebSocketConfig`의 `setAllowedOriginPatterns("*")`가 REST 쪽 `SecurityConfig`의 단일 origin CORS 설정과 불일치했다. NFR Design(Question 4 답변 B, Frontend는 정확히 하나의 origin에서 배포)에 따라 동일한 `quickchat.cors.allowed-origin` 값을 재사용하도록 수정
- 나머지 발견 항목(의존성 CVE, Docker 이미지가 root로 실행되는 문제, docker-compose.yml에 평문으로 커밋된 dev-only 자격증명, 사용자 검색/조회 API의 열람 범위 등)은 코드 수정 없이 `.gstack/security-reports/cso-2026-08-20.md`에 전체 기록했으며, 담당자 검토와 위험수용/조치 계획이 필요함(같은 폴더의 `risk-acceptance-2026-08-20.md` 참고)
- 알려진 제약: H2 수정 후에도 SUBSCRIBE 거부 시 클라이언트에 전달되는 에러가 사용자 친화적이지 않음(전용 STOMP 에러 큐가 없어 일반적인 예외 전파에 의존) - Build and Test 단계에서 실제 연결로 동작을 확인하고 필요하면 보완

---

## Post-Approval Patch 9 (2026-08-22, 실제 배포 환경에서 발견 - GET /api/channels/{id}/messages 500 오류)

사용자가 docker-compose로 실제 PostgreSQL에 붙여 처음으로 메시지 이력 조회 API를 실행하자
`GET /api/channels/{channelId}/messages?size=50`(cursor 없는 첫 페이지 요청)가 항상 500을 반환했다.
backend 로그: `SQL Error: 0, SQLState: 42P18 ... could not determine data type of parameter $2`.

원인: `MessageRepository.findPage`가 단일 JPQL `where m.channelId = :channelId and (:beforeSentAt is
null or m.sentAt < :beforeSentAt) order by m.sentAt desc`로 null/non-null cursor를 한 쿼리로 처리하고
있었는데, `beforeSentAt`이 실제로 null일 때 PostgreSQL이 `$2` 파라미터의 타입을 추론하지 못해
쿼리 자체가 실패했다. 이 쿼리는 이번 세션 내내 `mcp__workspace__bash` 샌드박스가 막혀 있어 실제 DB에
붙여 실행해본 적이 없었고(단위 테스트는 Mockito로 리포지토리를 목 처리해 이 문제를 잡아낼 수
없었음), 사용자가 실제 docker-compose 환경에서 메시지 이력을 조회한 것이 이 결함의 첫 실제 실행이었다.

수정: `MessageRepository`에서 `@Query` 하나 대신, null 여부에 따라 애초에 서로 다른(둘 다 파라미터가
항상 non-null인) Spring Data 파생 쿼리 메서드 두 개로 분리했다 - `findByChannelIdOrderBySentAtDesc`
(첫 페이지)와 `findByChannelIdAndSentAtLessThanOrderBySentAtDesc`(다음 페이지). `MessagingService
.getMessageHistory()`가 `beforeSentAt == null` 여부로 둘 중 하나를 호출하도록 변경. 이렇게 하면
PostgreSQL이 타입을 추론해야 하는 "null일 수도 있는 파라미터"가 애초에 존재하지 않아 근본적으로
문제가 재발하지 않는다(캐스팅 등으로 우회하지 않고 원인 자체를 제거).

테스트: `MessagingServiceTest`의 기존 `getMessageHistoryDelegatesToRepositoryWithDefaultPageSize`를
cursor 없음/있음 두 경로로 나눠 재작성, `MessagingServicePropertyTest`의 리포지토리 목도 새 메서드에
맞춰 갱신.

파일: `backend/src/main/java/com/quickchat/backend/repository/MessageRepository.java`,
`backend/src/main/java/com/quickchat/backend/service/MessagingService.java`,
`backend/src/test/java/com/quickchat/backend/service/MessagingServiceTest.java`,
`backend/src/test/java/com/quickchat/backend/service/MessagingServicePropertyTest.java`
