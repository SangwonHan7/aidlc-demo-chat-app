# Domain Entities (Frontend View Models) - Frontend Unit

Frontend는 자체 JPA/DB 엔티티가 없으므로, 여기서의 "domain entity"는 Backend REST API 응답을 그대로 반영하는 TypeScript 타입과 Zustand 스토어 상태 형태를 의미한다. 필드명은 Backend DTO와 camelCase로 이미 일치하므로 별도 변환 계층 없이 API 응답을 그대로 상태에 저장한다 (api-layer-summary.md 기준).

## API 타입 (Backend DTO 미러링)

```typescript
// types/domain.ts

export type ChannelType = "DIRECT" | "GROUP";
export type ChannelVisibility = "PUBLIC" | "INVITE_ONLY";
export type ChannelStatus = "ACTIVE" | "ARCHIVED";

export interface User {
  id: string; // UUID
  email: string;
  displayName: string;
}

export interface Channel {
  id: string;
  name: string;
  type: ChannelType;
  visibility: ChannelVisibility;
  ownerId: string;
  status: ChannelStatus;
  createdAt: string; // ISO-8601
}

export interface ChatMessage {
  id: string;
  channelId: string;
  senderId: string;
  content: string;
  sentAt: string; // ISO-8601
}

export interface MessagePage {
  messages: ChatMessage[];
  nextCursor: string | null; // 다음 요청의 `before` 파라미터로 그대로 사용
}

export interface PresenceStatus {
  userId: string;
  online: boolean;
}

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
}

export interface ApiError {
  errorCode: string;
  message: string;
}
```

## Backend DTO 대응 관계

| Frontend 타입 | Backend DTO | 비고 |
|---|---|---|
| User | UserResponse | 그대로 대응 |
| Channel | ChannelResponse | 그대로 대응 |
| ChatMessage | MessageResponse | 그대로 대응 (WebSocket 브로드캐스트 페이로드인 `ChatMessageEvent`와도 필드 동일) |
| MessagePage | MessagePageResponse | 그대로 대응 |
| PresenceStatus | PresenceStatusResponse | Gap 2 보완으로 신규 추가된 DTO |
| TokenPair | TokenResponse | 그대로 대응 |
| ApiError | ErrorResponse | GlobalExceptionHandler가 모든 에러를 이 포맷으로 반환 |

## Zustand 스토어 상태 형태 (Question 2 답변 B: 도메인별 분리)

```typescript
// store/authStore.ts
interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
}

// store/chatStore.ts
interface ChatState {
  channels: Channel[];
  activeChannelId: string | null;
  messagesByChannel: Record<string, ChatMessage[]>;
  nextCursorByChannel: Record<string, string | null>;
}

// store/presenceStore.ts
interface PresenceState {
  onlineUserIds: Set<string>;
}
```

STOMP 클라이언트 자체는 스토어가 아니라 `lib/stompClient.ts`의 싱글턴 모듈로 관리한다 (Question 4 답변 A - 앱 전역 연결 1개 유지). `useChatStore`는 이 모듈의 함수를 호출하되, 연결 객체 자체를 상태로 들고 있지 않는다.
