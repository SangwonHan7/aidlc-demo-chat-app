# Frontend Components - Frontend Unit

## 화면/라우트 구조 (Question 1 답변 A: Slack형 앱쉘)

- `/login`, `/register`: 인증 화면 - 앱쉘 없는 독립 페이지
- `/` (인증 필요, 미인증 시 `/login`으로 리다이렉트): `AppShellLayout` - `Sidebar` + `MainPanel`이 하나의 레이아웃에 상시 공존
  - 채널/DM 선택은 클라이언트 상태(`useChatStore.activeChannelId`)로 관리하며, 선택이 바뀌어도 페이지 전체가 아니라 `MainPanel`만 다시 그린다 (전체 페이지 전환 없음)

## 컴포넌트 계층

### 1. 인증 (Story 1.1)
- `LoginPage` (`/login`)
  - `LoginForm`: email/password 입력, 제출 시 `useAuthStore.login()` 호출, 에러는 폼 하단 인라인 표시 (Question 7 답변 B)
- `RegisterPage` (`/register`)
  - `RegisterForm`: email/password/displayName 입력, 클라이언트 검증(이메일 형식·비밀번호 8자 이상 - Backend와 동일) + 추가 UX(비밀번호 강도 인디케이터, Question 6 답변 B)

### 2. 앱쉘 (Story 1.2, 1.3, 1.4, 1.5 공통)
- `AppShellLayout`
  - `Sidebar`
    - `ChannelList` - 참여 중인 그룹 채널 + 공개 채널 중 미참여 채널(참여 버튼 노출)
    - `DirectMessageList` - DM 상대 목록, 각 항목에 `PresenceIndicator` 포함
    - `CreateChannelButton` → `CreateChannelModal` 오픈
  - `MainPanel`
    - `EmptyState` - 선택된 채널/DM 없을 때
    - `ConversationView` - 선택된 채널/DM
      - `ConversationHeader` - 채널명, 멤버 관리 버튼(OWNER만 노출), `PresenceIndicator`(DM인 경우 상대방 상태)
      - `MessageList` - 무한 스크롤(cursor 기반, Story 1.5)
      - `MessageInput` - 전송 버튼, 빈 내용이면 비활성화, rate limit 안내 문구 표시 영역

### 3. 채널 관리 (Story 2.1, 2.2)
- `CreateChannelModal`: 이름 + 공개범위(PUBLIC/INVITE_ONLY) 선택, 글자수 카운터(Question 6 답변 B의 추가 UX)
- `MemberManagementPanel` (OWNER만 접근): 멤버 목록, 사용자 검색 후 초대, 멤버 제외

### 4. 온라인 상태 표시 (Story 1.4, Gap 2 해결)
- `PresenceIndicator`: 특정 userId의 온라인 여부를 점(dot) 색상으로 표시. `usePresenceStore`에서 상태를 읽고, 화면에 보이는 사용자 id 집합을 기준으로 `GET /api/presence?userIds=`를 화면 진입 시 + 30초 간격으로 호출해 갱신 (business-rules.md 참고 - Backend에 presence 변경 push 채널이 없어 polling 방식으로 결정)

## Zustand 스토어 (Question 2 답변 B: 도메인별 분리)

- `useAuthStore`: `user`, `accessToken`, `refreshToken`, `isAuthenticated`, `login()`, `logout()`, `refreshAccessToken()`
- `useChatStore`: `channels`, `activeChannelId`, `messagesByChannel`, `nextCursorByChannel`, `sendMessage()`, `loadHistory()`, `appendIncomingMessage()`, `createChannel()`, `inviteMember()`, `removeMember()`
- `usePresenceStore`: `onlineUserIds`, `refreshPresence(userIds)`
- WebSocket 연결 자체는 스토어가 아니라 `lib/stompClient.ts` 싱글턴 모듈에서 관리한다 (Question 4 답변 A). 위 스토어들은 이 모듈의 함수를 호출만 한다.

## 컴포넌트별 API/WebSocket 연동 지점

| 컴포넌트/동작 | 연동 API 또는 WebSocket 목적지 |
|---|---|
| `LoginForm` 제출 | `POST /api/auth/login` |
| `RegisterForm` 제출 | `POST /api/auth/register` |
| 401 발생 시 자동 재발급 | `POST /api/auth/refresh` |
| `ChannelList` / `DirectMessageList` 로드 | `GET /api/channels` |
| `CreateChannelModal` 제출 | `POST /api/channels` |
| DM 시작(사용자 검색 후 선택) | `POST /api/channels/direct` |
| 공개 채널 참여 | `POST /api/channels/{channelId}/join` |
| `MemberManagementPanel` 초대 | `POST /api/channels/{channelId}/members` |
| `MemberManagementPanel` 제외 | `DELETE /api/channels/{channelId}/members/{userId}` |
| `MessageList` 최초 로드/무한 스크롤 | `GET /api/channels/{channelId}/messages?before=&size=` |
| `PresenceIndicator` 갱신 | `GET /api/presence?userIds=` |
| WebSocket 연결 수립 | STOMP CONNECT `/ws` (헤더 `Authorization: Bearer {accessToken}`) |
| `MessageInput` 전송 | STOMP SEND `/app/chat.send/{channelId}` |
| `ConversationView` 실시간 수신 | STOMP SUBSCRIBE `/topic/channel/{channelId}` |

## 결정 사항 요약 (Functional Design 질문 답변 기준)

| 항목 | 결정 | 근거 |
|---|---|---|
| 레이아웃 | Slack형 앱쉘 (Sidebar + MainPanel) | Question 1 = A |
| 상태 관리 | 도메인별 스토어 3개 분리 | Question 2 = B |
| 토큰 저장 | Access+Refresh 모두 localStorage | Question 3 = A → Contradiction 1 해결(A: 계약 유지+localStorage) |
| WebSocket 생명주기 | 앱 전역 연결 1개 유지, 구독만 전환 | Question 4 = A |
| 재연결 정책 | 지수 백오프 + REST 재조회로 갭 보완 | Question 5 = A |
| 클라이언트 검증 범위 | Backend 규칙 + 추가 UX | Question 6 = B |
| 에러 표시 방식 | 컨텍스트별 인라인 | Question 7 = B |
| 온라인 상태 API | `GET /api/presence?userIds=` + polling | Gap 2 해결(A: Backend 보완) |
