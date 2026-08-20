// domain-entities.md 미러링. 필드명은 Backend DTO(web/dto/*)와 camelCase로 이미 일치한다.

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

export type ChannelRole = "OWNER" | "MEMBER";

// Backend Code Generation Post-Approval Patch 3(ChannelMemberResponse)에 대응.
export interface ChannelMember {
  userId: string;
  role: ChannelRole;
}

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
}

export interface ApiError {
  errorCode: string;
  message: string;
}
