# Business Rules - Backend

## Channel
- 이름은 필수, 1~100자, 중복 허용 (Q2 답변 A - ID로만 구분)
- type=GROUP 생성 시 visibility(PUBLIC/INVITE_ONLY)를 반드시 지정 (FR-4)
- type=DIRECT는 시스템이 자동 생성 (사용자가 직접 "DM 채널 생성"을 호출하지 않고, 상대방과의 최초 메시지 전송 시 자동으로 없으면 생성)
- PUBLIC 채널: 누구나 joinChannel 가능
- INVITE_ONLY 채널: OWNER의 inviteMember 호출로만 참여 가능
- 이미 멤버인 사용자에 대한 재초대 또는 재참여 시도 -> ALREADY_MEMBER 에러 반환 (Q4 답변 A, 멱등 처리 아님)
- OWNER가 채널을 나가면 채널 status=ARCHIVED로 전환 (Q3 답변 B)
- status=ARCHIVED인 채널은 신규 메시지 저장(saveMessage) 요청을 CHANNEL_ARCHIVED 에러로 거부

## Auth
- 로그인 5회 연속 실패 시 lockedUntil 설정 (짧은 시간, 예: 15분), 그 사이 로그인 시도는 ACCOUNT_LOCKED 에러
- 로그인 성공 시 failedLoginCount 초기화

## Messaging
- 메시지 이력 조회는 cursor(마지막으로 받은 메시지의 id 또는 sentAt) 기반 페이지네이션 (Q5 답변 A)
- 기본 페이지 크기: 50건, 최신 메시지가 먼저 오는 역순 정렬

## 공통 에러 포맷
tech-env.md 기준: { "errorCode": "...", "message": "..." } (예: ALREADY_MEMBER, CHANNEL_ARCHIVED, ACCOUNT_LOCKED, NOT_A_MEMBER)
