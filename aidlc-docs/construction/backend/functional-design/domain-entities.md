# Domain Entities - Backend

backend-functional-design-plan.md 답변 반영: Q1=A(DM/Channel 통합 모델)

## User
- id, email(unique), passwordHash, displayName, createdAt
- [갱신: nfr-requirements.md Q2 답변 A] failedLoginCount/lockedUntil은 JPA 컬럼이 아니라 Redis(LoginLockRedisService)에 저장 (멀티 파드 상태 공유 목적)

## Channel
- id, name, type(DIRECT | GROUP), visibility(PUBLIC | INVITE_ONLY) - DIRECT는 항상 INVITE_ONLY 성격(값 저장은 하되 UI/정책상 의미 없음)
- ownerId, status(ACTIVE | ARCHIVED), createdAt
- 이름 중복 허용 (Q2 답변 A)

## ChannelMember
- channelId, userId, role(OWNER | MEMBER), joinedAt
- 복합키: (channelId, userId)

## Message
- id, channelId, senderId, content, sentAt

## RefreshToken
- [갱신: nfr-requirements.md Q3 답변 B] JPA 엔티티가 아니라 Redis에 저장 (RefreshTokenRedisService). 키: refresh-token:{token}, value: userId, TTL: 만료 기간

## Relationships
- User 1..N ChannelMember N..1 Channel
- Channel 1..N Message
- User 1..N Message (sender)
- DIRECT 채널: ChannelMember 정확히 2건 (Q1 답변 A - DM도 멤버 2명인 Channel)
