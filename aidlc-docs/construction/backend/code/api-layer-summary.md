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
