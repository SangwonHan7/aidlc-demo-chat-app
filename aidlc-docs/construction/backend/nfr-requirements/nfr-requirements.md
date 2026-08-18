# NFR Requirements - Backend Unit

## Performance
- p50 메시지 전달 지연 500ms 미만 (requirements.md)
- 500 동시 접속 WebSocket 세션 안정 처리

## Scalability
- 백엔드는 멀티 파드로 수평 확장 가능해야 함
- WebSocket 세션은 파드에 로컬로 유지되며, Redis Pub/Sub로 파드 간 메시지 브로드캐스트 (Application Design 확정)
- 로그인 실패 카운터/잠금 상태는 Redis에 저장해 파드 간 공유 (Q2 답변 A)

## Availability
- 목표 가동률 99.5% (실습 기간, requirements.md)
- 메시지 유실률 0% - Kafka를 버퍼로 사용해 컨슈머 재처리 가능하도록 설계

## Security
- JWT Access+Refresh 토큰, TLS(Ingress), Vault 시크릿 관리, XSS 이스케이프 (requirements.md 기본 보안 요구사항, Extension 여부와 무관하게 적용)
- Refresh Token은 Redis에 저장, TTL 만료 활용 (Q3 답변 B)
- Security Baseline Extension 미적용 (requirements.md Q5=B)

## Reliability and Observability
- Spring Boot Actuator 헬스체크(/actuator/health) + Prometheus 메트릭 노출(/actuator/prometheus) (Q5 답변 B)
- Resiliency Baseline Extension 미적용 (requirements.md Q6=B) - 고급 장애복구/DR 설계는 범위 밖

## Testability
- Unit Test 80%+ (JUnit5 + Mockito)
- Property-Based Testing 전체 규칙(PBT-01~10) 적용, Full Enforcement - 프레임워크는 jqwik (Q4 답변 A)

## Maintainability
- Spring Data JPA 사용 (Q1 답변 A) - 표준 레포지토리 패턴, 쿼리 메서드 활용
