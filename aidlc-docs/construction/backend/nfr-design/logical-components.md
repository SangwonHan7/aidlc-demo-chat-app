# Logical Components - Backend Unit

## Redis (단일 인스턴스, 키 프리픽스로 용도 구분 - Q5 답변 A)

| 용도 | 키 패턴 | TTL/비고 |
|---|---|---|
| 온라인 상태 | presence:{userId} | 세션 연결 동안 유지 |
| 로그인 실패 잠금 | login-lock:{email} | 실패 후 일정 시간(예: 15분) |
| Refresh Token | refresh-token:{token} | 토큰 만료 기간과 동일 |
| 채널 멤버십 캐시 | membership:{channelId}:{userId} | 짧은 TTL(예: 5분) + 변경 시 즉시 무효화 |
| 메시지 Rate Limit | rate-limit:{userId} | 1초 고정 윈도우 |
| WebSocket 브로드캐스트 Pub/Sub | ws-broadcast:{channelId} | 해당 없음 (Pub/Sub 채널) |

## Kafka (KRaft 모드)

| 토픽 | 용도 | Producer | Consumer |
|---|---|---|---|
| chat-messages | 메시지 저장 후 비동기 브로드캐스트 트리거 | EventComponent | EventComponent -> MessagingComponent.broadcastMessage |
| notifications | 향후 확장용 (MVP 범위 밖) | - | - |

- 발행 실패 시 최대 3회 재시도 (nfr-design-patterns.md Q1 답변 A)

## Vault
- 경로 예시: secret/quickchat/db (DB 접속정보), secret/quickchat/jwt (JWT 서명 키)
- 애플리케이션은 기동 시 Vault에서 조회, 환경변수에 평문 저장하지 않음 (requirements.md 기본 보안 요구사항)
