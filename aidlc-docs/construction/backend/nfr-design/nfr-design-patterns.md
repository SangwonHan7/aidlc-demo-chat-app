# NFR Design Patterns - Backend Unit

## Resilience Pattern: Kafka 발행 재시도
- EventComponent.publish() 실패 시 짧은 간격으로 최대 3회 재시도, 모두 실패하면 실패 로그를 남기고 예외 전파 (Q1 답변 A)
- Spring Retry(@Retryable) 또는 Kafka Producer 자체 retries 설정으로 구현

## Scalability Pattern: 고정 레플리카
- HPA(오토스케일링) 미적용, 고정 레플리카 수로 운영 (NAS 리소스 제약, Q2 답변 B)
- 정확한 레플리카 수는 Infrastructure Design 단계에서 실제 NAS 사양 확인 후 결정 (requirements.md Open Item과 연결)

## Performance Pattern: 멤버십 캐싱
- ChannelComponent.isMember() 결과를 Redis에 캐싱 (키: membership:{channelId}:{userId}, 짧은 TTL)
- 멤버 추가/제거(inviteMember, removeMember, joinChannel, leaveChannel) 시 해당 캐시 즉시 무효화 (Q3 답변 A)

## Security Pattern: 메시지 전송 Rate Limiting
- 사용자당 메시지 전송 rate limit 적용 - Redis 기반 고정 윈도우 카운터 (Q4 답변 A)
- 초과 시 RATE_LIMITED 에러 반환. 구체적 임계값(예: 초당 5건)은 Code Generation 단계에서 확정

## PBT 관점 추가 속성 (business-logic-model.md 보완)
- Rate Limiter Invariant: 윈도우 내 N번째 호출까지는 허용되고 N+1번째부터는 항상 차단됨 (경계값 테스트 대상)
- 캐시-DB 일관성 Invariant: 멤버십 캐시 무효화 직후 조회 결과는 항상 DB의 실제 멤버십 상태와 일치
