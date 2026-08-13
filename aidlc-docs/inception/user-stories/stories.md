# User Stories - QuickChat

분류 방식: Persona-Based (story-generation-plan.md Question 1 답변 A)
인수 조건 형식: 체크리스트 (story-generation-plan.md Question 2 답변 B)

## Persona: 일반 사용자 (팀원)

### Story 1.1: 회원가입 및 로그인
As a 일반 사용자, I want to 이메일과 비밀번호로 가입하고 로그인, so that 내 계정으로 메신저를 이용할 수 있다.

Acceptance Criteria:
- [ ] 이메일+비밀번호로 회원가입할 수 있다
- [ ] 로그인 성공 시 JWT(Access+Refresh)가 발급된다
- [ ] 비밀번호 5회 연속 실패 시 짧은 시간 동안 계정이 잠긴다

Related FR: FR-1

### Story 1.2: 1:1 다이렉트 메시지 전송
As a 일반 사용자, I want to 다른 사용자와 1:1로 실시간 메시지를 주고받기, so that 이메일보다 빠르게 소통할 수 있다.

Acceptance Criteria:
- [ ] 사용자를 검색해 DM을 열 수 있다
- [ ] 메시지 전송 시 500ms 이내에 상대방 화면에 표시된다 (WebSocket/STOMP)
- [ ] 전송한 메시지는 저장되어 이후에도 조회 가능하다

Related FR: FR-2, FR-5

### Story 1.3: 그룹 채널 참여 및 메시지
As a 일반 사용자, I want to 그룹 채널에 참여해 여러 명과 메시지를 주고받기, so that 팀 단위로 협업할 수 있다.

Acceptance Criteria:
- [ ] 공개 채널은 목록에서 바로 참여할 수 있다
- [ ] 초대 전용 채널은 초대를 받아야 참여할 수 있다
- [ ] 채널 내 메시지는 참여자 전원에게 실시간으로 표시된다

Related FR: FR-3, FR-4, FR-5

### Story 1.4: 온라인 상태 확인
As a 일반 사용자, I want to 다른 사용자의 접속 여부를 확인하기, so that 지금 응답을 받을 수 있을지 가늠할 수 있다.

Acceptance Criteria:
- [ ] 사용자 목록/DM 화면에서 온라인/오프라인 상태가 표시된다
- [ ] 상태는 접속/해제 시 합리적인 지연 내에 갱신된다

Related FR: FR-6

### Story 1.5: 메시지 이력 조회
As a 일반 사용자, I want to 채널/DM의 과거 메시지를 페이지 단위로 조회하기, so that 이전 대화 맥락을 다시 확인할 수 있다.

Acceptance Criteria:
- [ ] 채널/DM 진입 시 최근 메시지부터 페이지 단위로 불러온다
- [ ] 과거로 스크롤하면 이전 메시지를 추가로 불러온다
- [ ] 삭제 정책이 없으므로 보관된 모든 메시지가 조회 가능하다

Related FR: FR-7, FR-8

## Persona: 채널 관리자

### Story 2.1: 채널 생성 및 공개 범위 설정
As a 채널 관리자, I want to 새 채널을 만들고 공개/초대 전용 여부를 선택하기, so that 목적에 맞는 사람만 채널에 모이게 할 수 있다.

Acceptance Criteria:
- [ ] 채널 생성 시 이름과 공개 범위(공개/초대 전용)를 지정한다
- [ ] 공개로 설정하면 누구나 채널 목록에서 참여할 수 있다
- [ ] 초대 전용으로 설정하면 초대받은 사용자만 참여할 수 있다

Related FR: FR-3, FR-4

### Story 2.2: 채널 구성원 초대 및 관리
As a 채널 관리자, I want to 채널에 구성원을 초대하거나 내보내기, so that 채널 구성을 유지 관리할 수 있다.

Acceptance Criteria:
- [ ] 관리자는 사용자를 검색해 채널에 초대할 수 있다
- [ ] 관리자는 채널에서 구성원을 제외할 수 있다
- [ ] 일반 참여자는 초대/제외 권한이 없다 (본인 채널/DM만 접근 가능)

Related FR: FR-3

## Persona-Story Mapping

| Persona | Stories |
|---|---|
| 일반 사용자 | 1.1, 1.2, 1.3, 1.4, 1.5 |
| 채널 관리자 | 1.1-1.5 (일반 사용자 권한 포함) + 2.1, 2.2 |

## INVEST 준수 노트
- Independent: 각 스토리는 다른 스토리 완료 여부와 무관하게 독립적으로 검증 가능
- Negotiable: Acceptance Criteria는 구현 방법을 강제하지 않고 결과만 정의
- Valuable: 각 스토리는 vision.md Feature Area 1(실시간 메시징)에 직접 대응
- Estimable: 단일 기능 단위로 분리되어 규모 추정 가능
- Small: 각 스토리는 화면 1개 또는 API 흐름 1개로 범위를 제한
- Testable: 체크리스트 형태의 Acceptance Criteria로 테스트 가능
