# Domain Entities - Backend

backend-functional-design-plan.md 답변 반영: Q1=A(DM/Channel 통합 모델)

## User
- id, email(unique), passwordHash, displayName, createdAt
- failedLoginCount, lockedUntil (5회 실패 잠금 정책)

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
- token, userId, expiresAt, revoked

## Relationships
- User 1..N ChannelMember N..1 Channel
- Channel 1..N Message
- User 1..N Message (sender)
- DIRECT 채널: ChannelMember 정확히 2건 (Q1 답변 A - DM도 멤버 2명인 Channel)
