# Component Methods - QuickChat (Backend)

상세 비즈니스 로직(검증 규칙 등)은 Functional Design(Construction, per-unit)에서 정의합니다. 여기서는 메서드 시그니처와 입출력, 목적만 정의합니다.

## AuthComponent
| Method | Input | Output | Purpose |
|---|---|---|---|
| register | email, password | UserResponse | 신규 계정 생성 |
| login | email, password | TokenResponse(access, refresh) | 인증 및 토큰 발급 |
| validateToken | accessToken | UserPrincipal | WebSocket/REST 요청 인증 |
| refreshToken | refreshToken | TokenResponse | Access Token 재발급 |

## ChannelComponent
| Method | Input | Output | Purpose |
|---|---|---|---|
| createChannel | name, visibility(공개/초대전용), ownerId | ChannelResponse | 채널 생성 |
| inviteMember | channelId, inviterId, inviteeId | void | 초대 전용 채널에 멤버 추가 |
| removeMember | channelId, adminId, memberId | void | 채널에서 멤버 제외 |
| joinChannel | channelId, userId | void | 공개 채널 자율 참여 |
| isMember | channelId, userId | boolean | 멤버십 확인 (Facade에서 호출) |
| listChannelsForUser | userId | List of ChannelSummary | 사용자의 채널 목록 |

## MessagingComponent
| Method | Input | Output | Purpose |
|---|---|---|---|
| saveMessage | senderId, targetId(channel/DM), content | MessageRecord | 메시지 저장 |
| getMessageHistory | targetId, pageRequest | Page of MessageRecord | 이력 페이지 조회 |
| broadcastMessage | MessageRecord | void | Redis Pub/Sub로 각 파드에 전파 후 WebSocket 전송 |

## PresenceComponent
| Method | Input | Output | Purpose |
|---|---|---|---|
| markOnline | userId, sessionId | void | 접속 시 상태 갱신 |
| markOffline | userId, sessionId | void | 접속 해제 시 상태 갱신 |
| getStatus | userId | PresenceStatus | 현재 상태 조회 |
| subscribeStatusChanges | callback | Subscription | 상태 변경 실시간 반영 |

## EventComponent
| Method | Input | Output | Purpose |
|---|---|---|---|
| publish | topic, payload | void | Kafka 토픽 발행 |
| subscribe | topic, handler | void | Kafka 토픽 구독 및 핸들러 등록 |
