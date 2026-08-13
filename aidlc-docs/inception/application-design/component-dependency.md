# Component Dependency - QuickChat (Backend)

## Dependency Matrix

| From | To | Reason |
|---|---|---|
| ChatFacadeService | ChannelComponent | 메시지 전송 전 멤버십 확인 |
| ChatFacadeService | MessagingComponent | 메시지 저장 및 브로드캐스트 트리거 |
| ChatFacadeService | EventComponent | Kafka 발행/구독 트리거 |
| ChatFacadeService | PresenceComponent | 온라인 상태 참고(수신자 상태 확인 등) |
| MessagingComponent | EventComponent | 메시지 저장 후 이벤트 발행 (직접 Kafka 클라이언트 미사용) |
| ChannelComponent | AuthComponent | 초대 대상 사용자 존재 확인 |
| PresenceComponent | (Redis 직접) | 온라인 상태 저장/구독 (컴포넌트 의존 아님, 인프라 의존) |

## 통신 패턴
- Facade -> Component: 동기 메서드 호출 (프로세스 내부)
- Component -> EventComponent -> Kafka: 비동기 발행/구독
- MessagingComponent 브로드캐스트: Redis Pub/Sub로 파드 간 전파 후 각 파드가 자신에게 연결된 WebSocket 세션에만 전송

## 데이터 흐름: 메시지 전송 시퀀스
1. 클라이언트 -> WebSocket(STOMP) -> Controller -> ChatFacadeService.sendMessage()
2. ChatFacadeService -> ChannelComponent.isMember(channelId, userId) 확인
3. ChatFacadeService -> MessagingComponent.saveMessage() (DB 저장)
4. ChatFacadeService -> EventComponent.publish("chat-messages", message)
5. EventComponent(Consumer 측) -> MessagingComponent.broadcastMessage() 트리거
6. MessagingComponent -> Redis Pub/Sub -> 각 파드의 WebSocket 세션 -> 클라이언트 수신

### 텍스트 기반 흐름 요약 (다이어그램 대체)
```
Client -> Controller -> ChatFacadeService
ChatFacadeService -> ChannelComponent (isMember?)
ChatFacadeService -> MessagingComponent (save)
ChatFacadeService -> EventComponent (publish chat-messages)
EventComponent -> MessagingComponent (broadcast, via Kafka consumer)
MessagingComponent -> Redis Pub/Sub -> WebSocket sessions -> Client(s)
```
