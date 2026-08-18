# Functional Design Plan - Backend Unit

## Plan

- [ ] domain-entities.md 작성 (User, Channel, ChannelMember, Message, PresenceStatus 등)
- [ ] business-rules.md 작성 (검증 규칙, 제약조건)
- [ ] business-logic-model.md 작성 (컴포넌트별 워크플로우 + PBT-01 Testable Properties)
- [ ] 프론트엔드 컴포넌트 문서는 해당 없음 (Backend 유닛)

## Questions

각 질문에 알파벳으로 답한 뒤 채팅으로 "답변 완료"라고 알려주세요.

## Question 1 (Domain Model)

1:1 DM과 그룹 채널을 어떻게 모델링할까요?

A) 통합 모델 - Channel에 type(DIRECT/GROUP) 필드를 두어 DM도 멤버 2명인 Channel로 취급

B) 분리 모델 - DM과 Channel을 완전히 다른 엔티티/테이블로 분리

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 2 (Business Rules)

채널 이름 중복을 허용할까요?

A) 허용 - 채널은 ID로만 구분, 이름 중복 가능

B) 같은 소유자가 만든 채널 내에서는 이름 중복 불가

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3 (Business Scenarios)

채널 관리자(생성자)가 채널을 나가거나 계정을 탈퇴하면 채널은 어떻게 될까요?

A) 남은 멤버 중 한 명에게 관리자 권한 자동 위임

B) 관리자가 나가면 채널이 보관(archived) 상태로 전환되어 새 메시지 전송 불가

C) MVP 범위에서는 다루지 않음 - 관리자는 자신이 만든 채널을 나갈 수 없도록 단순화

D) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 4 (Business Rules / Idempotency)

이미 멤버인 사용자를 다시 초대하거나, 이미 참여 중인 공개 채널에 다시 참여를 시도하면 어떻게 처리할까요?

A) 에러 반환 (예: ALREADY_MEMBER 에러코드)

B) 에러 없이 성공 응답 (멱등 처리 - 이미 멤버면 그대로 성공)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 5 (Data Flow)

메시지 이력 조회 페이지네이션 방식은 어떻게 할까요?

A) Cursor 기반 - 마지막으로 받은 메시지의 ID/타임스탬프를 기준으로 다음 페이지 조회

B) Offset/페이지 번호 기반

C) Other (please describe after [Answer]: tag below)

[Answer]: A
