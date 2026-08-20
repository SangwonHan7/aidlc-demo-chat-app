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
