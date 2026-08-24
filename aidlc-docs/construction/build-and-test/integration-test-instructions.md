# Integration Test Instructions - QuickChat

## Purpose
Backend와 Frontend는 각각 별도 AI 세션 단계에서 만들어졌고, 실제 네트워크로 서로 연결되어 동작한 적이 아직 없다. 이 문서는 Code Generation 각 단계에서 "Build and Test로 이동"이라고 명시적으로 미뤄둔 항목들을 모아, 실제 기동 후 확인해야 할 통합 시나리오로 정리한다. (정적 계약 감사로 이미 확인된 API 경로/DTO 필드 일치 여부는 `contract-test-instructions.md` 참고 - 여기서는 "실제로 떠서 동작하는지"만 다룬다.)

## Setup Integration Test Environment

### 1. Start Required Services

```bash
cd infra/docker-compose
docker compose up -d   # postgres, redis, kafka, vault, backend, frontend 전부
```

### 2. Configure Service Endpoints

```bash
export API_URL=http://localhost:8080
export WS_URL=ws://localhost:8080/ws
export FRONTEND_URL=http://localhost:3000
```

## Test Scenarios

### Scenario 1: Flyway 마이그레이션 - 실제 PostgreSQL 대비 최초 실행
- **Description**: `backend/src/main/resources/db/migration/V1__init_schema.sql`이 실제 PostgreSQL 16에 대해 오류 없이 적용되는지. 작성 후 한 번도 실행되지 않았음(알려진 제약, `repository-layer-summary.md`)
- **Setup**: 깨끗한 `postgres` 컨테이너(볼륨 삭제 후)
- **Test Steps**: 백엔드 최초 기동 → Flyway가 자동 적용 → `\dt`로 `users`, `channels`, `channel_members`, `messages` 테이블 존재 확인
- **Expected Results**: 마이그레이션 성공, 스키마가 `domain/*.java` 엔티티와 매핑 오류 없이 일치(Hibernate `ddl-auto=validate` 또는 `none` 설정이라면 특히 중요 - `application.yml` 확인)
- **Cleanup**: 볼륨 삭제 후 재기동으로 반복 검증 가능

### Scenario 2: 회원가입 -> 로그인 -> JWT로 보호된 API 호출 (Story 1.1)
- **Description**: 전체 인증 흐름
- **Test Steps**: `POST /api/auth/register` → `POST /api/auth/login` → 응답의 `accessToken`으로 `GET /api/users/me` 호출
- **Expected Results**: 각 단계 200/201, `GET /api/users/me`가 가입한 이메일/표시이름을 반환. 비밀번호 5회 연속 실패 시 짧은 시간 잠금(`AccountLockedException`, 423) 확인

### Scenario 3: WebSocket 연결 -> 구독 -> 실시간 메시지 수신 (Story 1.2, 1.3)
- **Description**: STOMP 인증 + 실시간 브로드캐스트. **Post-Approval Patch 5로 막 수정된 필드명 버그(`messageId`→`id`)의 실제 검증이 이 시나리오의 핵심 목적**
- **Test Steps**: 사용자 A, B로 각각 로그인 → 채널 생성(A) → B 초대 또는 PUBLIC이면 B가 참여 → 둘 다 `/ws`에 CONNECT(Authorization 헤더) → `/topic/channel/{channelId}` 구독 → A가 `/app/chat.send/{channelId}`로 2개 이상 메시지 연속 전송
- **Expected Results**: B의 클라이언트가 **모든** 메시지를 수신(수정 전이었다면 두 번째 메시지부터 사라졌을 것 - `chatStore.mergeIncomingMessage`가 `id`를 정상적으로 구분하는지 확인). 각 메시지의 `id`가 실제 UUID이고 `undefined`가 아님을 확인
- **Cleanup**: 연결 해제(DISCONNECT) 후 presence가 오프라인으로 바뀌는지 함께 확인(Scenario 5와 연계)

### Scenario 4: 멤버가 아닌 채널을 구독 시도 (Post-Approval Patch 6, H2 회귀 확인)
- **Description**: 방금 수정한 SUBSCRIBE 인가 검사의 실제 동작 확인 - 단위 테스트(`StompAuthChannelInterceptorTest`)는 인터셉터를 직접 호출해 확인했지만, 실제 STOMP 클라이언트로 SUBSCRIBE했을 때 클라이언트 쪽에 어떤 프레임이 도착하는지(ERROR 프레임인지, 연결 자체가 끊기는지)는 아직 확인한 적 없음
- **Test Steps**: 사용자 C(어느 채널에도 속하지 않음)가 다른 채널의 UUID를 추정/획득해 `/topic/channel/{다른채널ID}`를 SUBSCRIBE
- **Expected Results**: 구독이 거부되고 C는 그 채널의 메시지를 수신하지 못함. 프런트엔드(`stompClient.ts`)가 이 실패를 어떻게 처리하는지도 함께 확인 - 현재 `onStompError`는 재연결만 시도하므로 사용자에게 에러가 표시되지 않을 수 있음(알려진 제약, 아래 참고)

### Scenario 5: 온라인 상태 표시 + 2분 TTL 갭 (Story 1.4)
- **Description**: `presence:{userId}` Redis 키의 TTL(2분)을 하트비트로 갱신하는 로직이 없다는 것이 알려진 제약(`api-layer-summary.md` Patch 1)
- **Test Steps**: 사용자 로그인 후 WebSocket 연결을 2분 이상 유지(메시지 전송 없이) → 다른 사용자가 `PresenceIndicator`/`GET /api/presence`로 상태 확인
- **Expected Results**: (현재 구현 기준 예상) 2분 경과 후 연결은 살아있지만 Redis TTL이 만료되어 "오프라인"으로 잘못 표시될 가능성이 높음 - 실제로 재현되는지 확인하고, 재현되면 STOMP 하트비트(`WebSocketConfig`에 미설정) 도입 여부를 결정할 것

### Scenario 6: CORS preflight 실제 브라우저 동작 (REST + WebSocket)
- **Description**: `SecurityConfig`의 `CorsConfigurationSource`(REST)와 방금 좁힌 `WebSocketConfig`의 `allowedOriginPatterns`(WS, Patch 6) 둘 다 단위 테스트로 검증되지 않음
- **Test Steps**: Frontend(`http://localhost:3000`)를 실제 브라우저로 열어 로그인 시도(REST) + WebSocket 연결(WS) - 둘 다 다른 origin(`http://localhost:8080`)의 Backend를 호출
- **Expected Results**: REST는 정상적으로 preflight(OPTIONS) 통과, WebSocket은 핸드셰이크 성공. `NEXT_PUBLIC_API_BASE_URL`/`CORS_ALLOWED_ORIGIN`이 서로 일치하지 않으면 여기서 바로 실패로 드러남 - build-instructions.md의 build-arg 관련 알려진 제약과 연결되는 지점이므로 실패 시 그쪽부터 확인

### Scenario 7: Kafka 발행-구독 라운드트립 (메시지 저장/브로드캐스트 파이프라인 전체)
- **Description**: `ChatFacadeService` → Kafka(`chat-messages` 토픽, 추정 - 실제 토픽명은 `EventPublisher`/`application.yml` 확인) → `ChatMessageConsumer` → `MessagingService.broadcastMessage` → Redis Pub/Sub → `RedisBroadcastListener` → STOMP. 지금까지 어느 구간도 실제 브로커로 검증된 적 없음(`business-logic-summary.md`, `repository-layer-summary.md` 알려진 제약)
- **Test Steps**: 메시지 전송 후 Kafka Consumer Group의 컨슈머 랙(lag)이 0으로 떨어지는지, 여러 백엔드 파드(현재는 고정 1 레플리카라 단일 파드지만 향후 스케일 대비)가 있다면 Redis Pub/Sub로 전 파드에 브로드캐스트되는지 확인
- **Expected Results**: 메시지 저장(PostgreSQL) → 실시간 브로드캐스트(STOMP)까지 p50 500ms 미만 (requirements.md Performance 목표 - `performance-test-instructions.md`와 연계)

### Scenario 8: 메시지 이력 다중 페이지 무결성 (Story 1.5)
- **Description**: `MessagingServicePropertyTest`의 round-trip 테스트는 정렬 로직 일부만 다뤘고, 여러 페이지에 걸친 전체 무결성(중복/누락 없음)은 통합 테스트로 보강 권장(`business-logic-summary.md`)
- **Test Steps**: 채널에 메시지 120개 이상 전송 → `GET /api/channels/{id}/messages?size=50`으로 3페이지 이상 cursor 페이지네이션 → 각 페이지의 `id` 집합을 합쳐 전체 메시지 집합과 대조
- **Expected Results**: 중복/누락 없이 정확히 전송한 메시지 수와 일치, `sentAt` 오름차순 유지(Frontend `prependHistoryPage`가 이미 이 속성을 pure function 단위로는 검증했음 - 실제 API 응답까지 연결했을 때도 유지되는지 확인)

## Run Integration Tests

### 1. Execute Integration Test Suite
현재 이 프로젝트에는 별도의 통합 테스트 코드(Testcontainers 등)가 작성되어 있지 않다 - 위 8개 시나리오는 수동으로(또는 이번 문서를 바탕으로 별도 통합 테스트 코드를 새로 작성해) 확인해야 한다. Testcontainers 기반 자동화가 필요하면 `backend/build.gradle`에 `org.testcontainers:junit-jupiter`, `org.testcontainers:postgresql`, `org.testcontainers:kafka` 추가를 권장.

### 2. Verify Service Interactions
- **Test Scenarios**: 위 1-8
- **Logs Location**: `docker compose logs backend`, `docker compose logs frontend`, 브라우저 개발자 도구 Network/WS 탭

### 3. Cleanup
```bash
cd infra/docker-compose && docker compose down -v
```

## 알려진 제약 (투명성 목적)
위 8개 시나리오 중 어느 것도 이번 세션에서 실제로 실행되지 않았다(샌드박스 제약). Scenario 3(WebSocket 필드명 버그)과 Scenario 4(SUBSCRIBE 인가)는 이번 Build and Test 단계에서 새로 발견/수정한 결함에 대한 것이라 특히 우선 확인이 필요하다.
