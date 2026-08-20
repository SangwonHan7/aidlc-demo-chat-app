import { create } from "zustand";
import { apiClient } from "@/lib/apiClient";
import type { Channel, ChatMessage, ChannelVisibility, MessagePage } from "@/types/domain";

/**
 * 실시간 브로드캐스트로 수신한 메시지를 목록에 병합한다.
 * PBT-01 속성 #1(Idempotence): 같은 id의 메시지를 여러 번 반영해도 최종 목록에는 1개만 남는다.
 */
export function mergeIncomingMessage(messages: ChatMessage[], incoming: ChatMessage): ChatMessage[] {
  if (messages.some((m) => m.id === incoming.id)) {
    return messages;
  }
  return [...messages, incoming].sort((a, b) => a.sentAt.localeCompare(b.sentAt));
}

/**
 * 이전 페이지(더 과거 메시지)를 기존 목록 앞에 합친다.
 * PBT-01 속성 #2(Invariant): 결과 배열의 sentAt은 항상 오름차순을 유지한다.
 * PBT-01 속성 #3(Round-trip): 두 페이지의 id 합집합이 정확히 한 번씩만 존재한다(중복/누락 없음).
 */
export function prependHistoryPage(existing: ChatMessage[], page: ChatMessage[]): ChatMessage[] {
  const existingIds = new Set(existing.map((m) => m.id));
  const newOnes = page.filter((m) => !existingIds.has(m.id));
  return [...newOnes, ...existing].sort((a, b) => a.sentAt.localeCompare(b.sentAt));
}

interface ChatState {
  channels: Channel[];
  discoverableChannels: Channel[];
  activeChannelId: string | null;
  messagesByChannel: Record<string, ChatMessage[]>;
  nextCursorByChannel: Record<string, string | null>;
  setActiveChannel: (channelId: string) => void;
  loadChannels: () => Promise<void>;
  loadDiscoverableChannels: () => Promise<void>;
  loadHistory: (channelId: string) => Promise<void>;
  loadMoreHistory: (channelId: string) => Promise<void>;
  receiveMessage: (message: ChatMessage) => void;
  createChannel: (name: string, visibility: ChannelVisibility) => Promise<Channel>;
  startDirectMessage: (otherUserId: string) => Promise<Channel>;
  joinChannel: (channelId: string) => Promise<void>;
  inviteMember: (channelId: string, inviteeId: string) => Promise<void>;
  removeMember: (channelId: string, userId: string) => Promise<void>;
  reset: () => void;
}

export const useChatStore = create<ChatState>((set, get) => ({
  channels: [],
  discoverableChannels: [],
  activeChannelId: null,
  messagesByChannel: {},
  nextCursorByChannel: {},

  setActiveChannel: (channelId) => set({ activeChannelId: channelId }),

  loadChannels: async () => {
    const { data } = await apiClient.get<Channel[]>("/api/channels");
    set({ channels: data });
  },

  // story 1.3: 참여 여부와 무관한 PUBLIC 채널 전체 목록 (Backend Post-Approval Patch 3에서 보완된
  // GET /api/channels/discoverable 소비).
  loadDiscoverableChannels: async () => {
    const { data } = await apiClient.get<Channel[]>("/api/channels/discoverable");
    set({ discoverableChannels: data });
  },

  loadHistory: async (channelId) => {
    const { data } = await apiClient.get<MessagePage>(`/api/channels/${channelId}/messages`, {
      params: { size: 50 },
    });
    set((state) => ({
      messagesByChannel: {
        ...state.messagesByChannel,
        [channelId]: [...data.messages].sort((a, b) => a.sentAt.localeCompare(b.sentAt)),
      },
      nextCursorByChannel: { ...state.nextCursorByChannel, [channelId]: data.nextCursor },
    }));
  },

  loadMoreHistory: async (channelId) => {
    const cursor = get().nextCursorByChannel[channelId];
    if (!cursor) return;
    const { data } = await apiClient.get<MessagePage>(`/api/channels/${channelId}/messages`, {
      params: { before: cursor, size: 50 },
    });
    set((state) => ({
      messagesByChannel: {
        ...state.messagesByChannel,
        [channelId]: prependHistoryPage(state.messagesByChannel[channelId] ?? [], data.messages),
      },
      nextCursorByChannel: { ...state.nextCursorByChannel, [channelId]: data.nextCursor },
    }));
  },

  receiveMessage: (message) => {
    set((state) => ({
      messagesByChannel: {
        ...state.messagesByChannel,
        [message.channelId]: mergeIncomingMessage(state.messagesByChannel[message.channelId] ?? [], message),
      },
    }));
  },

  createChannel: async (name, visibility) => {
    const { data } = await apiClient.post<Channel>("/api/channels", { name, visibility });
    set((state) => ({ channels: [...state.channels, data] }));
    return data;
  },

  startDirectMessage: async (otherUserId) => {
    const { data } = await apiClient.post<Channel>("/api/channels/direct", { otherUserId });
    set((state) =>
      state.channels.some((c) => c.id === data.id) ? state : { channels: [...state.channels, data] }
    );
    return data;
  },

  joinChannel: async (channelId) => {
    await apiClient.post(`/api/channels/${channelId}/join`);
  },

  inviteMember: async (channelId, inviteeId) => {
    await apiClient.post(`/api/channels/${channelId}/members`, { inviteeId });
  },

  removeMember: async (channelId, userId) => {
    await apiClient.delete(`/api/channels/${channelId}/members/${userId}`);
  },

  // AppShellLayout 로그아웃 경로에서 호출 - 같은 브라우저 탭에서 다른 사용자가 다시 로그인할 때
  // 이전 사용자의 채널/메시지가 잔류해 보이는 것을 방지한다.
  reset: () =>
    set({
      channels: [],
      discoverableChannels: [],
      activeChannelId: null,
      messagesByChannel: {},
      nextCursorByChannel: {},
    }),
}));
