# QuickChat Backend

Spring Boot 3 기반 백엔드. 상세 설계는 `../aidlc-docs/construction/backend/`를 참고하세요.

## 실행 방법 (로컬)

1. 의존 서비스 기동: PostgreSQL, Redis, Kafka(KRaft), Vault (`../infra/docker-compose/docker-compose.yml` 예정)
2. 환경변수 설정 (아래 표 참고)
3. `./gradlew bootRun`

## 환경변수

| 변수 | 설명 | 기본값 |
|---|---|---|
| DB_URL | PostgreSQL JDBC URL | jdbc:postgresql://localhost:5432/quickchat |
| DB_USERNAME | DB 사용자 | quickchat |
| DB_PASSWORD | DB 비밀번호 | (없음, Vault에서 주입 권장) |
| REDIS_HOST / REDIS_PORT | Redis 접속 정보 | localhost / 6379 |
| KAFKA_BOOTSTRAP_SERVERS | Kafka 브로커 주소 | localhost:9092 |
| JWT_SECRET | JWT 서명 키 (HS256, 32바이트 이상) | (없음, Vault에서 주입 필수) |
| CORS_ALLOWED_ORIGIN | Frontend가 배포된 origin (다른 origin이므로 CORS 허용 필요) | http://localhost:3000 |

## API 개요

- `POST /api/auth/register`, `/api/auth/login`, `/api/auth/refresh` - 인증 (FR-1)
- `POST /api/channels`, `GET /api/channels`, `POST /api/channels/direct` - 채널 생성/조회/DM (FR-3, FR-4)
- `POST /api/channels/{id}/join`, `POST /api/channels/{id}/members`, `DELETE /api/channels/{id}/members/{userId}` - 참여/초대/제외
- `GET /api/channels/{id}/messages?before=&size=` - 메시지 이력 (cursor 기반, FR-7)
- `GET /api/presence?userIds=` - 온라인 상태 조회, UUID 목록 파라미터 (FR-6, story 1.4)
- WebSocket(STOMP) `/ws` 엔드포인트, `/app/chat.send/{channelId}`로 전송, `/topic/channel/{channelId}` 구독 (FR-2, FR-5)
  - CONNECT 인증 성공 시 서버가 자동으로 해당 세션을 온라인으로 표시하고, DISCONNECT 시 오프라인으로 표시 (별도 클라이언트 API 호출 불필요)
- API 문서: `/swagger-ui.html` (springdoc-openapi)
- 헬스체크/메트릭: `/actuator/health`, `/actuator/prometheus`

## 테스트

- `./gradlew test` - JUnit5(예시 기반) + jqwik(속성 기반, PBT) 함께 실행
- 속성 기반 테스트 대상과 근거: `../aidlc-docs/construction/backend/code/business-logic-summary.md`
