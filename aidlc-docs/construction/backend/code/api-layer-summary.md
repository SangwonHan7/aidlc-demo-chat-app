# API Layer Summary - Backend

## 생성된 코드
- Controller: AuthController, ChannelController, MessageController (REST), ChatWebSocketController (STOMP)
- WebSocket 지원: WebSocketConfig, StompAuthChannelInterceptor(CONNECT 프레임 JWT 인증), RedisBroadcastListener(Redis Pub/Sub -> STOMP 브로드캐스트 릴레이)
- Security: SecurityConfig, JwtAuthenticationFilter, JwtTokenProvider
- DTO 10개(record): RegisterRequest, LoginRequest, RefreshRequest, TokenResponse, UserResponse, CreateChannelRequest, StartDirectChannelRequest, InviteMemberRequest, ChannelResponse, MessageResponse, MessagePageResponse, ErrorResponse
- GlobalExceptionHandler: 모든 응답을 { "errorCode", "message" } 포맷으로 통일

## 테스트
- AuthControllerTest (MockMvc, `@WebMvcTest` + `addFilters=false`): 회원가입 성공/중복이메일/유효성오류, 로그인 토큰 응답
- ChannelController, MessageController, ChatWebSocketController에 대한 동일 수준의 MockMvc 테스트는 이번 라운드에서 생성하지 않음 - Build and Test 단계의 통합 테스트(실제 인증 포함)로 보완 권장

## 알려진 제약 (투명성 목적)
- WebSocket 인증(StompAuthChannelInterceptor)은 단위 테스트로 다루지 않음 - 실제 STOMP 클라이언트 연결이 필요해 Build and Test의 통합/E2E 테스트 대상
- Rate limiting(메시지 전송)은 ChatFacadeService 내부에 있어 API 레이어 테스트에는 포함하지 않음 (서비스 레이어에서 MessageRateLimitServicePropertyTest로 검증)
