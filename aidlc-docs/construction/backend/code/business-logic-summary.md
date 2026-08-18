# Business Logic Summary - Backend

## 생성된 코드
- Domain: User, Channel, ChannelMember(+ChannelMemberId), Message, ChannelType/Visibility/Status/Role (enum)
- Service: AuthService, ChannelService, MessagingService, PresenceService, EventPublisher(EventComponent), ChatFacadeService(Orchestrator)
- Exception: ApiException(base) + 8개 구체 예외(AlreadyMember, ChannelArchived, AccountLocked, NotAMember, InvalidCredentials, RateLimited, EmailAlreadyExists, ChannelNotFound, ForbiddenAction)

## 설계 변경 사항 (Functional Design 대비)
- User의 failedLoginCount, RefreshToken 엔티티는 NFR Requirements 결정(Redis 저장)에 따라 JPA 엔티티에서 제외하고 Redis 서비스로 구현 - domain-entities.md 갱신함
- Channel/User/Message는 클라이언트 측 UUID 생성(생성자에서 즉시 id 할당)으로 변경 - DB 라운드트립 없이 즉시 id 사용 가능, 테스트 용이성도 개선

## PBT Compliance (Property-Based Testing, Full Enforcement)

| Rule | 상태 | 비고 |
|---|---|---|
| PBT-01 (속성 식별) | Compliant | business-logic-model.md에 11개 속성 문서화 |
| PBT-02 (Round-trip) | Compliant | Auth(register->login), Messaging(save->retrieve) - AuthServicePropertyTest, MessagingServicePropertyTest |
| PBT-03 (Invariant) | Compliant | Channel 생성자-멤버, 중복 참여 무변화, ARCHIVED 쓰기거부, 캐시-DB 일관성, Rate limiter 임계값 - ChannelServicePropertyTest, MessagingServicePropertyTest, MessageRateLimitServicePropertyTest |
| PBT-04 (Idempotence) | Compliant | Presence markOnline - PresenceRedisServicePropertyTest |
| PBT-05 (Oracle) | N/A | 참조 구현과 비교할 복잡한 알고리즘 없음 (단순 CRUD/상태전이 위주) |
| PBT-06 (Stateful) | Partial | Channel 멤버십/상태 전이는 예시+속성 테스트로 다뤘으나, 정식 커맨드 시퀀스 기반 stateful PBT(모델 비교)는 미적용 - Build and Test 단계에서 통합 테스트로 보완 권장 |
| PBT-07 (생성기 품질) | Compliant | @Provide로 도메인에 맞는 이메일/비밀번호/문자열 커스텀 생성기 사용 (원시 정수/문자열만 사용한 곳 없음) |
| PBT-08 (Shrinking/재현성) | Compliant | jqwik 기본 shrinking 유지, 실패 시 seed 로그는 jqwik 기본 동작에 위임 (CI 설정은 Build and Test 단계에서 확정) |
| PBT-09 (프레임워크) | Compliant | jqwik 1.8.5 (build.gradle), JUnit5 플랫폼과 통합 |
| PBT-10 (상호 보완) | Compliant | 모든 속성 테스트는 대응하는 example-based 테스트(AuthServiceTest, ChannelServiceTest, MessagingServiceTest)와 짝을 이룸 |

### 블로킹 파인딩 없음이지만 명시적으로 범위를 좁힌 항목 (투명성 목적)
- Kafka 발행-구독 라운드트립(EventComponent, PBT-02): 단위 테스트에서는 EventPublisher/ChatMessageConsumer를 개별적으로 다루지 않았음. 실제 Kafka(또는 Testcontainers)가 필요한 통합 시나리오이므로 Build and Test 단계의 통합 테스트로 이동
- 메시지 이력 페이지 무결성(중복/누락 없음, PBT-03): MessagingServicePropertyTest의 round-trip 테스트가 정렬 로직을 일부 다루지만, 여러 페이지에 걸친 전체 무결성은 통합 테스트 단계에서 보강 권장
