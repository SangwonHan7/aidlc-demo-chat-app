# Business Logic Model - Backend

## AuthComponent 워크플로우
1. register: 이메일 중복 확인 -> 비밀번호 해싱 -> User 저장
2. login: 이메일로 User 조회 -> lockedUntil 확인 -> 비밀번호 검증 -> 성공 시 failedLoginCount 초기화 및 JWT 발급 / 실패 시 failedLoginCount 증가, 5회 도달 시 lockedUntil 설정

## ChannelComponent 워크플로우
1. createChannel: type/visibility 검증 -> Channel 저장 -> 생성자를 OWNER로 ChannelMember 저장
2. joinChannel(PUBLIC만): 이미 멤버인지 확인(ALREADY_MEMBER면 에러) -> ChannelMember(MEMBER) 저장
3. inviteMember(INVITE_ONLY): 호출자가 OWNER인지 확인 -> 대상이 이미 멤버인지 확인(ALREADY_MEMBER면 에러) -> ChannelMember(MEMBER) 저장
4. removeMember / leaveChannel: OWNER가 나가는 경우 Channel.status=ARCHIVED로 전환, 그 외에는 ChannelMember 삭제

## MessagingComponent 워크플로우
1. saveMessage: Channel.status 확인(ARCHIVED면 에러) -> 멤버십 확인 -> Message 저장 -> ChatFacadeService에 반환(브로드캐스트용)
2. getMessageHistory: cursor 기준 이전 페이지 조회, 최신순 정렬

## PresenceComponent 워크플로우
1. markOnline/markOffline: Redis에 사용자별 세션 집합 갱신, 세션 수 0이면 offline

## EventComponent 워크플로우
1. publish: 토픽별 payload를 Kafka로 발행 (JSON 직렬화)
2. subscribe: 등록된 handler로 역직렬화된 payload 전달

## PBT-01: Testable Properties (Property-Based Testing 전체 규칙 적용, requirements.md 참조)

| Component | Property | Category | 설명 |
|---|---|---|---|
| AuthComponent | login(register(email,pw)) 성공 | Round-trip | 가입 직후 동일 비밀번호로 로그인 성공 |
| AuthComponent | 실패 횟수 임계값 | Invariant | failedLoginCount < 5이면 잠기지 않음, ==5 순간부터 잠김 |
| ChannelComponent | 생성자는 항상 멤버 | Invariant | createChannel 이후 getMember(ownerId)는 항상 true, role=OWNER |
| ChannelComponent | 중복 참여/초대 무변화 | Invariant | ALREADY_MEMBER 에러 발생 시 멤버 목록/개수는 호출 전과 동일 (Q4 답변 A로 멱등이 아닌 불변량으로 처리) |
| ChannelComponent | ARCHIVED 이후 쓰기 거부 | Invariant | status=ARCHIVED가 되면 이후 모든 saveMessage 호출은 항상 실패 |
| MessagingComponent | 저장-조회 라운드트립 | Round-trip | saveMessage로 저장한 내용은 getMessageHistory 조회 결과에 동일하게 나타남 |
| MessagingComponent | 페이지 무결성 | Invariant | cursor 페이지네이션 결과는 전체 메시지 집합을 중복/누락 없이 덮음, 정렬 순서(최신순) 유지 |
| PresenceComponent | 멱등적 상태 전이 | Idempotence | markOnline(markOnline(u)) 상태는 markOnline(u) 상태와 동일 (여러 세션/중복 호출에도 online) |
| EventComponent | 발행-구독 라운드트립 | Round-trip | publish한 payload를 subscribe 핸들러가 역직렬화했을 때 원본과 동일 |

N/A로 표시: Oracle(참조 구현과 비교할 대상 없음 - 단순 CRUD/상태 전이 위주), Commutativity(순서 의존적 연산 위주라 해당 없음), Induction(재귀적 구조 없음), Easy verification(복잡한 탐색/최적화 알고리즘 없음)

## 참고
프론트엔드 컴포넌트 설계는 Backend 유닛 범위에 포함되지 않음 (Frontend 유닛에서 별도 진행).
