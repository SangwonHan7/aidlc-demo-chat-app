# Tech Stack Decisions - Backend Unit

| Decision Area | Choice | Rationale |
|---|---|---|
| 데이터 접근 계층 | Spring Data JPA | 팀 학습 곡선 낮음, 표준 레포지토리 패턴 (Q1 답변 A) |
| 로그인 실패 카운터/잠금 저장소 | Redis | 멀티 파드 환경에서 상태 공유 필요, TTL로 자동 해제 (Q2 답변 A) |
| Refresh Token 저장소 | Redis | TTL 자동 만료, 조회 성능 (Q3 답변 B) |
| Property-Based Testing 프레임워크 | jqwik | JUnit5 통합, requirements.md 권고안 확정 (Q4 답변 A, PBT-09) |
| 모니터링 | Spring Boot Actuator + Prometheus | Resiliency Extension은 미적용이지만 최소 관측성 확보 (Q5 답변 B) |
| WebSocket 프로토콜 | STOMP over WebSocket | tech-env.md 표준, 폴링 미사용 |
| 메시지 브로커 | Kafka (KRaft 모드) | tech-env.md 표준, 비동기 이벤트 처리 |
| 캐시 / Pub-Sub | Redis | 온라인 상태 추적 + WebSocket 파드 간 브로드캐스트 |
| 시크릿 관리 | HashiCorp Vault | tech-env.md 표준 |

## PBT-09 준수
jqwik을 프로젝트 의존성(build.gradle)에 포함하고 JUnit5 테스트 러너와 통합합니다. Custom generator, 자동 shrinking, seed 기반 재현성을 모두 지원합니다.
