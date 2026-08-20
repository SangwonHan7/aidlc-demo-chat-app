import { create } from "zustand";
import { apiClient } from "@/lib/apiClient";
import type { PresenceStatus } from "@/types/domain";

/**
 * presence 조회 응답을 현재 온라인 집합에 병합한다.
 * PBT-01 속성 #4(Idempotence): 동일한 응답을 여러 번 반영해도 최종 집합은 동일하게 유지된다.
 */
export function mergePresence(current: Set<string>, statuses: PresenceStatus[]): Set<string> {
  const next = new Set(current);
  for (const status of statuses) {
    if (status.online) {
      next.add(status.userId);
    } else {
      next.delete(status.userId);
    }
  }
  return next;
}

interface PresenceState {
  onlineUserIds: Set<string>;
  refreshPresence: (userIds: string[]) => Promise<void>;
  reset: () => void;
}

// business-rules.md 온라인 상태 갱신 정책: push 채널이 없어 화면 진입 시 + 30초 간격 polling.
export const usePresenceStore = create<PresenceState>((set, get) => ({
  onlineUserIds: new Set(),

  refreshPresence: async (userIds) => {
    if (userIds.length === 0) return;
    const { data } = await apiClient.get<PresenceStatus[]>("/api/presence", {
      params: { userIds: userIds.join(",") },
    });
    set({ onlineUserIds: mergePresence(get().onlineUserIds, data) });
  },

  // AppShellLayout 로그아웃 경로에서 chatStore.reset()과 함께 호출된다.
  reset: () => set({ onlineUserIds: new Set() }),
}));
