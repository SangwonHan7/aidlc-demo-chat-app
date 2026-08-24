# Frontend Components Summary - Frontend

## 생성된 코드
- 페이지(App Router): `src/app/page.tsx`(AppShell), `src/app/login/page.tsx`, `src/app/register/page.tsx`, `src/app/api/health/route.ts`
- 레이아웃: `src/components/layout/AppShellLayout.tsx`(부트스트랩: 세션 복원 -> 내 프로필 조회 -> WS 연결 -> 채널 로드), `MainPanel.tsx`, `EmptyState.tsx`
- 인증: `src/components/auth/LoginForm.tsx`, `RegisterForm.tsx`
- 채널: `src/components/channel/Sidebar.tsx`, `ChannelList.tsx`, `DirectMessageList.tsx`, `CreateChannelModal.tsx`, `MemberManagementPanel.tsx`
- 대화: `src/components/conversation/ConversationView.tsx`, `ConversationHeader.tsx`, `MessageList.tsx`(react-window 가상화), `MessageInput.tsx`
- 상태 표시: `src/components/presence/PresenceIndicator.tsx`
- 공통: `src/components/common/ErrorBoundary.tsx` (`app/layout.tsx` 최상위에 연결)
- 신규 lib: `src/lib/directChannel.ts` - `resolveDirectChannelPeer()` (DIRECT 채널의 상대방 프로필 조회, `Channel.name`이 화면용 문자열이 아니라서 필요)
- 모든 상호작용 요소에 `data-testid="{component}-{element-role}"` 부여 완료

## 설계 결정 반영
- Slack형 앱쉘(Question 1=A): `/`는 `AppShellLayout`(Sidebar+MainPanel 상시 공존), `/login`·`/register`는 앱쉘 없는 독립 페이지
- 채널/DM 선택은 `activeChannelId` 클라이언트 상태로만 전환, `MainPanel`만 다시 그림 (페이지 전체 전환 없음)
- DIRECT 채널 표시명: `channel.name`(내부 식별자 `"dm:{id1}:{id2}"`)을 그대로 쓰지 않고, 멤버 목록에서 나를 제외한 상대를 찾아 프로필을 조회(`resolveDirectChannelPeer`)해 표시 - `ConversationView`/`ConversationHeader`/`DirectMessageList` 공통 적용
- 에러 표시: 컨텍스트별 인라인(Question 7=B) - 각 폼/모달 하단에 `{component}-error` testid로 표시
- 온라인 상태: `PresenceIndicator`가 화면 진입 시 + 30초 polling으로 `GET /api/presence` 갱신 (Gap 2 해결안 소비)
- 로그아웃 시 `chatStore.reset()`/`presenceStore.reset()`을 함께 호출해 동일 브라우저에서 다른 사용자로 재로그인할 때 이전 사용자의 채널/메시지/온라인상태가 잔류하지 않도록 처리 (이번 단계에서 자체 발견한 누락, 아래 참고)

## 이번 단계에서 발견하고 직접 보완한 항목 (투명성 목적)
Functional/NFR/Infrastructure Design에서 이미 승인된 설계를 그대로 구현하는 과정에서, 여러 화면이 공통으로 전제하는 기반 기능 중 실제로는 존재하지 않던 것들을 발견했다. 모두 대안이 갈리는 설계 트레이드오프가 아니라(Contradiction 1/Gap 2처럼 사용자 선택이 필요한 종류가 아니라) 이미 승인된 화면이 동작하기 위한 필수 배선이라고 판단해, 발견 즉시 직접 보완하고 여기 모아 투명하게 보고한다.

1. **Backend Post-Approval Patch 3 (채널 멤버 목록/사용자 조회/공개 채널 발견)** - `ConversationHeader`(DM 상대 표시), `MemberManagementPanel`(story 2.2), `ChannelList`(story 1.3 "공개 채널은 목록에서 바로 참여")를 구현하려 하니 Backend에 (a) 채널 멤버 목록 조회, (b) UUID -> 프로필 변환, (c) 참여 여부 무관 공개 채널 전체 목록 API가 없었음. `GET /api/channels/{id}/members`, `GET /api/users?ids=`, `GET /api/users/search?email=`, `GET /api/channels/discoverable`를 추가. 상세: `aidlc-docs/construction/backend/code/api-layer-summary.md`
2. **Backend Post-Approval Patch 4 (내 프로필 조회)** - `AppShellLayout` 부트스트랩을 구현하려 하니, 로그인/새로고침 이후 "나는 누구인가"를 알아낼 API가 전혀 없어 `authStore.user`가 항상 `null`로 남는 문제를 발견 (`isOwner` 판정, DM 상대 식별, 멤버 관리에서 자기 자신 제외 등 다수 화면이 이 값을 전제). `GET /api/users/me`를 추가하고 Frontend에 `authStore.loadCurrentUser()`를 추가해 부트스트랩 단계에서 호출
3. **ErrorBoundary 미연결** - NFR Requirements에서 결정하고 컴포넌트까지 만들어 두었던 `ErrorBoundary`가 실제로는 어디에도 렌더링 트리에 연결되어 있지 않아 방어 기능이 전혀 동작하지 않는 상태였음. `app/layout.tsx` 최상위에 연결
4. **로그아웃 시 스토어 잔류** - `chatStore`/`presenceStore`가 Zustand 싱글턴이라 로그아웃해도 이전 사용자의 채널/메시지/온라인상태가 메모리에 남아있던 것을 발견. 두 스토어에 `reset()`을 추가하고 `Sidebar`의 로그아웃 처리에서 `stompClient.disconnect()`와 함께 호출

Backend에 대한 변경(1, 2)은 이미 승인된 Backend Code Generation 결과물에 대한 재작업이므로, 전체 Frontend Code Generation 완료 보고 시 Patch 1(Presence)~4(내 프로필 조회)를 모아 다시 한 번 사용자에게 명시적으로 보고할 예정.

5. **Post-Approval Patch 7 (2026-08-21, `npm test` 실제 실행 결과에서 발견) - 네이티브 이메일 검증이 커스텀 에러 메시지를 가로챔** - 사용자가 로컬에서 `npm test`를 실제로 실행한 결과 `LoginForm.test.tsx`/`RegisterForm.test.tsx`/`LoginForm.property.test.tsx` 3개가 실패했다. 원인을 코드로 직접 추적: `LoginForm`/`RegisterForm`의 `<input type="email">`이 비어있지 않고 형식이 이상하면(예: "not-an-email"), 제출 버튼 클릭 시 브라우저(및 jsdom)의 네이티브 HTML5 제약 검증이 우리 `onSubmit` 핸들러보다 먼저 개입해 `submit` 이벤트 자체를 막아버린다. 그 결과 `handleSubmit`이 전혀 실행되지 않아 커스텀 한글 에러 메시지("올바른 이메일 형식을 입력해주세요.")가 절대 뜨지 않는다 - 테스트만의 문제가 아니라 실제 브라우저에서도 우리 UX 대신 브라우저 자체 툴팁이 뜨는 실제 결함이었다(business-rules.md의 "클라이언트 검증 메시지는 한글로 표시" 요구와 불일치). 경쟁하는 설계 대안이 없는 프레임워크 동작 결함이라 판단해 두 `<form>`에 `noValidate`를 추가해 직접 수정. 부수적으로, 이 결함을 진단하는 과정에서 `LoginForm.property.test.tsx`/`RegisterForm.property.test.tsx`가 `cleanup()`을 성공 경로 끝에서만 호출해, 한 iteration이 실패하면 DOM이 정리되지 않고 다음 iteration이 "여러 엘리먼트 발견" 2차 오류로 원인을 가리는 테스트 하니스 결함도 함께 발견해 `try/finally` + iteration 시작 전 `cleanup()`으로 수정.
7. **Post-Approval Patch 8 (2026-08-21, 사용자가 실제 배포된 화면에서 발견) - 새로고침 시 채팅 내역 휘발** - 새로고침하면 채팅 화면이 비어버리는 문제가 보고됨. 원인: `activeChannelId`/`messagesByChannel`이 Zustand 인메모리 상태뿐이라 새로고침으로 완전히 초기화되고, `AppShellLayout` 부트스트랩이 로그인 세션(`accessToken`)과 채널 "목록"만 복원할 뿐 어떤 채널을 보고 있었는지는 전혀 복원하지 않았음(메시지 자체는 서버 DB에 그대로 남아있어 데이터 손실은 아니었음). `chatStore.ts`에 `restoreActiveChannel()`을 추가 - `setActiveChannel` 호출 시 채널 id를 `localStorage`에 저장(authStore.ts의 토큰 저장과 동일한 패턴)하고, `AppShellLayout`의 부트스트랩이 `loadChannels()` 완료 후 이를 호출해 저장된 채널이 여전히 로드된 채널 목록에 있을 때만(멤버십 유지) 복원한다 - 없으면(제외/삭제됨) 저장값을 지운다. `activeChannelId`가 복원되면 `ConversationView`의 기존 effect가 `loadHistory()`를 호출해 실제 메시지 이력을 서버에서 다시 불러온다. `chatStore.reset()`(로그아웃)에서도 이 저장값을 함께 지워, 다른 사용자로 재로그인할 때 이전 사용자가 보던 채널이 잔류하지 않도록 함. 회귀 테스트 4건을 `chatStore.test.ts`에 추가.
8. **Post-Approval Patch 10 (2026-08-22, 사용자 기능 요청) - 채팅에서 발신자 구분 표시** - "채팅에서 누가 무슨 말을 했는지 구분이 안 된다"는 요청(Backend Post-Approval Patch 9로 새로고침 후 메시지 이력이 정상적으로 다시 보이게 된 뒤 발견). 기존 `MessageList.tsx`는 `message.senderId`만 갖고 있고 표시이름을 전혀 조회하지 않아, 내가 보낸 메시지인지 아닌지만(좌우 정렬) 구분됐고 다른 사람들끼리는 구분이 안 됐다(GROUP 채널에서 특히 문제). `MemberManagementPanel.tsx`에 이미 있던 `GET /api/channels/{id}/members` + `GET /api/users?ids=` 패턴을 `lib/memberProfiles.ts`(`fetchMemberProfilesById`)로 재사용 가능하게 뽑아내고, `ConversationView.tsx`가 채널이 바뀔 때마다 이를 호출해 `senderId -> User` 맵을 `MessageList`에 내려준다. `MessageList`는 내가 보낸 메시지가 아닌 경우에만 말풍선 위에 발신자 표시이름을 작게 보여준다(이미 정렬로 구분되는 내 메시지에는 표시하지 않음 - Slack 등 일반적인 채팅 UI 관례). 채널에서 이미 나간 사용자가 보낸 과거 메시지는 멤버 목록에 없어 프로필을 못 찾을 수 있어 "알 수 없는 사용자"로 폴백 처리. react-window의 고정 행 높이(`ROW_HEIGHT`)를 56px→72px로 늘려 이름 한 줄이 들어갈 공간을 확보(내 메시지도 동일한 행 높이를 가져야 가상화 리스트가 성립하므로, 이름을 안 보여줄 때도 높이는 동일하게 유지). 신규 테스트: `MessageList.test.tsx`(발신자 표시/미표시/폴백 3가지 케이스) - 이 컴포넌트는 그동안 조합 컴포넌트라는 이유로 단위 테스트가 없었는데, 이번에 처음 추가함.

## 테스트 (Step 6, React Testing Library + fast-check)
- 예시 기반(RTL): `LoginForm.test.tsx`(이메일 형식 오류), `RegisterForm.test.tsx`(이메일/비밀번호/표시이름 오류, 표시이름 카운터, 비밀번호 강도 표시), `MessageInput.test.tsx`(빈 내용/공백만 있을 때 전송 버튼 비활성화), `PresenceIndicator.test.tsx`(온라인/오프라인 렌더링), `CreateChannelModal.test.tsx`(글자수 카운터, 빈 이름 검증, 공개범위 라디오, 취소)
- 속성 기반(fast-check): `LoginForm.property.test.tsx`(임의 문자열에 대해 이메일 오류 배너 표시 여부가 `isValidEmail`과 항상 일치), `RegisterForm.property.test.tsx`(비밀번호/표시이름 각각에 대해 동일한 방식으로 `isValidPassword`/`isValidDisplayName`과 일치) - `validation.property.test.ts`(순수 함수 자체)와는 별개로, "컴포넌트가 그 함수를 화면에 정확히 연결했는지"를 검증하는 배선 테스트. `login()`/`register()`는 목으로 대체해 유효한 입력이 실제 네트워크 호출로 이어지지 않게 격리
- PBT-10: 두 속성 테스트 모두 위 예시 기반 테스트와 짝을 이룸

### 알려진 제약 (투명성 목적)
- `mcp__workspace__bash` 샌드박스가 이번 세션 내내 `VM_DISK_SPACE_INSUFFICIENT`로 사용 불가해, 위 테스트 코드는 Read/Write/Edit로 작성했지만 `npm test`(Vitest) 실제 실행/통과 확인은 하지 못했다. Backend 쪽과 동일한 성격의 제약이며, Build and Test 단계에서 실제 실행 확인이 필요하다.
- `Sidebar`/`MainPanel`/`EmptyState`/`AppShellLayout`/`ChannelList`/`DirectMessageList`/`MemberManagementPanel`/`ConversationView` 등 조합 컴포넌트 자체에 대한 RTL 테스트는 이번 라운드에 포함하지 않음 - Step 6 계획(frontend-code-generation-plan.md)에 명시된 항목(LoginForm/RegisterForm/MessageInput/PresenceIndicator/CreateChannelModal)만 우선 다루었고, 나머지는 여러 스토어/네트워크 호출이 얽혀 있어 Build and Test 단계의 통합 테스트로 다루는 것이 더 적합하다고 판단
