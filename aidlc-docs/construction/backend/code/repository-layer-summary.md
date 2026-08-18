# Repository Layer Summary - Backend

## 생성된 코드
- JPA Repository: UserRepository, ChannelRepository, ChannelMemberRepository, MessageRepository(cursor 페이지네이션 쿼리)
- Redis 서비스: LoginLockRedisService, RefreshTokenRedisService, PresenceRedisService, MembershipCacheService, MessageRateLimitService
- Kafka: EventPublisher(발행, 재시도는 producer 설정), ChatMessageConsumer(구독), KafkaConfig(JSON 직렬화/역직렬화), KafkaTopics
- DB 마이그레이션: `backend/src/main/resources/db/migration/V1__init_schema.sql` (users, channels, channel_members, messages)

## 테스트
- Redis 서비스 중 PresenceRedisService(멱등성), MessageRateLimitService(임계값 불변량)는 jqwik 속성 테스트로 검증
- LoginLockRedisService, RefreshTokenRedisService, MembershipCacheService에 대한 전용 단위 테스트는 이번 라운드에서 생성하지 않음 - ChannelServiceTest/ChannelServicePropertyTest에서 MembershipCacheService를 목으로 다루며 간접 검증됨
- Kafka(EventPublisher/ChatMessageConsumer)는 실제 브로커 또는 Testcontainers가 필요해 단위 테스트 대상에서 제외 - Build and Test 단계 통합 테스트로 이동

## 알려진 제약
- Flyway 마이그레이션은 실제 PostgreSQL에 대해 실행해 검증한 적이 없음 (sandbox 셸 사용 불가로 로컬 빌드/마이그레이션 실행이 이번 세션에서 불가능했음) - Build and Test 단계에서 최초 실행 및 검증 필요
