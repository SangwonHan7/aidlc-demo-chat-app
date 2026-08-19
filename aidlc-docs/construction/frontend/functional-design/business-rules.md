# Business Rules - Frontend Unit

## 인증/토큰 저장 (Question 3, Contradiction 1 해결)
- Access Token, Refresh Token 모두 로그인/재발급 응답(JSON)을 그대로 localStorage에 저장한다 (키: `quickchat.accessToken`, `quickchat.refreshToken`). Backend는 Set-Cookie를 내려주지 않으므로 이 방식만 실제로 구현 가능하다.
- 페이지 로드 시 localStorage에 토큰이 있으면 자동으로 로그인 상태를 복원하고, 없으면 `/login`으로 리다이렉트한다.
- Access Token 만료(Backend 기본 TTL 15분, `JwtTokenProvider`)로 401 응답을 받으면: 저장된 refreshToken으로 `/api/auth/refresh`를 1회 자동 호출 → 성공하면 원래 요청을 재시도하고, 실패(refreshToken도 만료/무효)하면 토큰을 삭제하고 `/login`으로 이동한다.
- 동시에 여러 요청이 401을 받아도 refresh 호출은 1회만 수행하고 나머지 요청은 그 결과를 기다린다 (중복 재발급 방지, in-flight promise 캐싱).
- 로그아웃 시 localStorage 토큰 삭제 + STOMP 연결 종료.
- XSS 발생 시 두 토큰 모두 탈취될 수 있는 위험을 감수한다는 결정이며, Security Baseline 확장이 이번 프로젝트에서 미적용(requirements.md Q5=B)이라는 결정과 일관된다.

## 클라이언트 입력 검증 (Question 6 답변 B: 백엔드 규칙 + 추가 UX)
- 이메일: 형식 검증 (Backend `@Email`과 동일 수준의 정규식)
- 비밀번호: 8~100자 (Backend `@Size(min=8,max=100)`과 동일) + 추가 UX로 강도 표시(약함/보통/강함) - 표시용일 뿐 서버로 전송하는 값이나 검증 결과에는 영향 없음
- displayName: 1~50자 (Backend `@Size(max=50)`과 동일)
- 채널명: 필수 입력(`@NotBlank`와 동일) + 글자수 카운터 UX 표시(권장 50자, Backend는 길이 제한이 없으므로 강제 차단하지 않음)
- 메시지 입력: 공백만 있는 경우를 포함해 빈 내용이면 전송 버튼을 비활성화 (Backend도 별도 글자수 제한 없음, 클라이언트도 추가 제한 없음)
- 위 항목으로 걸러지지 않는 모든 검증/업무 규칙 실패는 클라이언트가 자체 문구를 만들지 않고 Backend 응답의 `errorCode`/`message`를 그대로 인라인 표시한다 (Question 7 답변 B)

## WebSocket 연결 정책 (Question 4 답변 A, Question 5 답변 A)
- 로그인 성공, 또는 앱 최초 로드 시 토큰 복원에 성공하면 STOMP 연결을 1회 수립하고 로그아웃 시에만 종료한다.
- CONNECT 프레임 헤더에 `Authorization: Bearer {accessToken}`을 포함한다 (`StompAuthChannelInterceptor` 계약).
- 채널/DM 전환 시: 이전 구독을 해지(unsubscribe)하고 새 채널의 `/topic/channel/{channelId}`를 구독한다. 연결 자체는 재사용한다.
- 연결 끊김을 감지하면 지수 백오프(1s → 2s → 4s → 8s → 최대 30s)로 재연결을 시도한다.
- 재연결 성공 시, 활성 채널에 대해 마지막으로 수신한 메시지의 `sentAt`을 기준으로 `GET /api/channels/{channelId}/messages`를 재조회해 끊긴 동안 누락된 메시지를 보완한다. 수신 메시지는 `id` 기준으로 중복 제거한다.
- Access Token 만료로 재연결이 인증 오류로 계속 실패하면 재연결 루프를 멈추고 `/api/auth/refresh`를 먼저 실행한 뒤, 새 토큰으로 재연결한다.

## 온라인 상태 갱신 정책 (Gap 2 해결 - 신규 결정)
Backend는 presence 변경을 실시간으로 push하는 별도 채널을 제공하지 않는다 (REST pull 방식의 `GET /api/presence?userIds=`만 존재, api-layer-summary.md Post-Approval Patch 참고). 따라서 Frontend는 다음과 같이 갱신한다.
1. 화면에 표시 중인 사용자 id 목록이 바뀔 때(채널 전환, DM 목록/멤버 목록 로드) 즉시 `GET /api/presence?userIds=`를 호출한다.
2. 화면이 떠 있는 동안 30초 간격으로 동일 API를 재호출해 갱신한다(polling). 화면을 벗어나면(unmount) polling을 중단한다.
3. 이 30초 간격은 Backend의 `presence:{userId}` Redis 키 TTL(2분)보다 충분히 짧다. 다만 Backend에는 아직 하트비트로 TTL을 갱신하는 로직이 없어(같은 문서의 알려진 제약 참고), 연결은 유지되어 있어도 2분 이상 STOMP 프레임 왕래가 없으면 실제로는 온라인인데 "오프라인"으로 보일 수 있다. Build and Test 단계에서 실사용 패턴을 확인한 뒤 하트비트 도입 여부를 재검토한다.

## 에러 표시 정책 (Question 7 답변 B: 컨텍스트별 인라인)
- 폼 제출 에러(로그인/가입/채널 생성 등): 해당 폼 하단에 `errorCode`에 대응하는 문구를 인라인으로 표시한다.
- 메시지 전송 실패(WS 오류 등): 해당 메시지 옆에 재전송 아이콘과 함께 인라인으로 표시한다.
- 목록/조회성 API 실패(채널 목록, 이력 조회 등): 해당 영역에 "불러오지 못했습니다" 상태와 재시도 버튼을 인라인으로 표시한다.
- `errorCode` → 사용자 문구 매핑은 Code Generation 단계에서 상수 파일(예: `lib/errorMessages.ts`)로 정리한다 (예: `RATE_LIMITED` → "메시지를 너무 빠르게 보내고 있어요. 잠시 후 다시 시도해주세요.").

## 메시지 송신 시 낙관적 업데이트 미적용 (설계 결정, 질문 항목 아님)
`ChatWebSocketController`는 `@SendTo`를 쓰지 않고 저장 → Kafka 발행 → Redis Pub/Sub → `/topic/channel/{channelId}` 브로드캐스트 경로로만 응답하며, 발신자 본인도 이 브로드캐스트를 구독자로서 수신한다. 따라서 Frontend는 전송 시 로컬에 메시지를 낙관적으로 먼저 추가하지 않고, 브로드캐스트 수신 시점에만 목록에 추가한다. 약간의 지연은 있지만 서버가 유일한 소스가 되어 중복/불일치 처리 로직이 필요 없다.
