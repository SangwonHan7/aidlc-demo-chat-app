# Code Generation Plan - Backend Unit

## Unit Context
- Stories: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2 (stories.md) - 전체 스토리가 Backend API/로직 필요
- Dependencies: Infra 유닛의 로컬 개발 환경(PostgreSQL/Kafka/Redis/Vault, docker-compose)이 먼저 떠 있어야 통합 테스트 가능. 이번 단계는 코드/테스트 작성까지이며 실제 기동/빌드 검증은 Build and Test 단계에서 수행
- Interfaces 제공: REST API(JSON) + WebSocket(STOMP) - Frontend 유닛이 이 계약에 의존 (unit-of-work-dependency.md, Backend 우선 개발)
- 소유 엔티티: User, Channel, ChannelMember, Message, (RefreshToken은 Redis에 저장하므로 JPA 엔티티 아님)
- 코드 위치: `backend/` (workspace root 기준, unit-of-work.md 모노레포 구조)
- 기술 스택: Java 17, Spring Boot 3.x, Gradle, Spring Data JPA, PostgreSQL, Redis(Lettuce), Kafka(spring-kafka, KRaft), Flyway, jqwik(PBT) + JUnit5 + Mockito

## Plan

### Step 1: Project Structure Setup (Greenfield)
- [x] `backend/build.gradle`, `backend/settings.gradle` - Spring Boot 3.x, Java 17, 의존성(Web, WebSocket, Data JPA, PostgreSQL driver, Data Redis, spring-kafka, Flyway, Actuator, Micrometer-Prometheus, springdoc-openapi, jqwik, JUnit5, Mockito)
- [x] `backend/src/main/resources/application.yml` - 기본 설정(DB/Redis/Kafka 연결 정보는 환경변수+Vault로 주입하는 placeholder)
- [x] `backend/Dockerfile` (멀티스테이지 빌드)

### Step 2: Business Logic Generation (domain-entities.md, business-rules.md, business-logic-model.md)
- [x] User, Channel, ChannelMember, Message JPA 엔티티 (`backend/src/main/java/.../domain/`)
- [x] AuthService, ChannelService, MessagingService, PresenceService, EventService, ChatFacadeService (`backend/src/main/java/.../service/`)
- [x] 공통 예외 클래스 (AlreadyMemberException, ChannelArchivedException, AccountLockedException 등) 및 errorCode 매핑
- Related Stories: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2

### Step 3: Business Logic Unit Testing (PBT Full Enforcement)
- [x] JUnit5+Mockito 예시 기반 테스트 (각 서비스 핵심 시나리오)
- [x] jqwik 속성 기반 테스트 - business-logic-model.md PBT-01 표의 9개 속성 + nfr-design-patterns.md의 2개 속성(Rate Limiter Invariant, 캐시-DB 일관성) = 11개
- [x] PBT-10: 각 Property 테스트와 짝이 되는 example-based 회귀 테스트 최소 1개씩 확인

### Step 4: Business Logic Summary
- [x] `aidlc-docs/construction/backend/code/business-logic-summary.md` 작성

### Step 5: API Layer Generation
- [x] AuthController (회원가입/로그인/토큰 재발급), ChannelController(생성/초대/제외/참여/목록), MessageController(이력 조회), ChatWebSocketController(STOMP, 메시지 전송/구독)
- [x] Request/Response DTO(record), GlobalExceptionHandler({errorCode,message} 포맷)
- [x] JwtAuthenticationFilter, RateLimitInterceptor (Redis 기반)
- Related Stories: 1.1, 1.2, 1.3, 1.5, 2.1, 2.2

### Step 6: API Layer Unit Testing
- [x] MockMvc/WebMvcTest 기반 컨트롤러 테스트 (성공/에러코드 케이스) - AuthControllerTest 작성, 나머지는 Build and Test로 이동(하단 알려진 제약 참고)
- [x] jqwik: 입력 검증 관련 속성 - AuthServicePropertyTest 등 서비스 레이어 속성 테스트로 커버, 컨트롤러 레벨 전용 속성 테스트는 범위 축소

### Step 7: API Layer Summary
- [x] `aidlc-docs/construction/backend/code/api-layer-summary.md` 작성

### Step 8: Repository Layer Generation
- [x] Spring Data JPA Repository: UserRepository, ChannelRepository, ChannelMemberRepository, MessageRepository
- [x] Redis 기반 서비스: PresenceRedisService, LoginLockRedisService, RefreshTokenRedisService, MembershipCacheService, MessageRateLimitService
- [x] Kafka: EventPublisher(Producer), ChatMessageConsumer (chat-messages 토픽), 재시도 설정(최대 3회)

### Step 9: Repository Layer Unit Testing
- [x] Repository/Redis/Kafka 연동 지점 단위 테스트 (Mockito로 외부 의존성 목킹) - Redis 서비스는 jqwik으로, Kafka는 Build and Test로 이동(하단 알려진 제약 참고)
- [x] jqwik: 저장-조회 라운드트립(Message는 MessagingServicePropertyTest로 커버) - Kafka 발행-구독 라운드트립은 실제 브로커 필요로 범위 축소

### Step 10: Repository Layer Summary
- [x] `aidlc-docs/construction/backend/code/repository-layer-summary.md` 작성

### Step 11: Database Migration Scripts
- [x] Flyway 마이그레이션: `backend/src/main/resources/db/migration/V1__init_schema.sql` (users, channels, channel_members, messages) - 작성 완료, 실제 실행 검증은 Build and Test 단계로 이동

### Step 12: Documentation Generation
- [x] `backend/README.md` (실행 방법, 환경변수, 엔드포인트 개요)
- [x] springdoc-openapi 의존성으로 `/swagger-ui` 자동 문서화 (별도 마크다운 API 문서는 생성하지 않고 코드 주석/어노테이션으로 대체)

### Step 13: Deployment Artifacts Generation
- [x] `backend/Dockerfile` (Step 1에서 생성) 확인/보완
- [x] `infra/docker-compose/docker-compose.yml`에 backend 서비스 항목 추가 (Infra 유닛 소유 파일이지만 Backend 이미지 참조를 위해 최소 반영)
- [x] 참고: `infra/k3s/app/backend-deployment.yaml` 등 전체 K8s 매니페스트는 unit-of-work.md의 개발 순서에 따라 Frontend 코드 생성 이후 Infra 마무리 단계에서 생성 (지금은 범위 아님) - 계획대로 범위 제외 유지

## Part 2 완료
전체 13단계 실행 완료 (2026-08-18). 상세 결과는 각 Summary 문서(business-logic-summary.md, api-layer-summary.md, repository-layer-summary.md) 참고. 알려진 제약(Kafka 통합 테스트, Flyway 실행 검증, WebSocket 인증 통합 테스트)은 Build and Test 단계로 이동.

## Post-Approval Patch (2026-08-18)
Frontend Functional Design 진행 중 발견된 Gap(Story 1.4 Presence API 미연결)을 사용자 승인 하에 보완 - PresenceController/PresenceStatusResponse 추가, StompAuthChannelInterceptor에 markOnline/markOffline 연동, PresenceControllerTest 추가. 상세는 api-layer-summary.md의 "Post-Approval Patch" 섹션과 audit.md 참고.

## 실행 방식
Part 2(Generation)에서 위 13단계를 순서대로 실행하며, 각 단계 완료 시 체크박스를 [x]로 표시하고 aidlc-state.md를 갱신합니다.
