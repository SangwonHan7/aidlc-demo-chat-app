"use client";

import { usePresenceStore } from "@/store/presenceStore";

interface Props {
  userId: string;
}

// Gap 2(Backend Presence API 보완) 결과물 소비. business-rules.md: push 채널이 없어 polling으로 갱신.
export function PresenceIndicator({ userId }: Props) {
  const isOnline = usePresenceStore((s) => s.onlineUserIds.has(userId));
  return (
    <span
      data-testid={`presence-indicator-${userId}`}
      title={isOnline ? "온라인" : "오프라인"}
      className={`inline-block h-2 w-2 rounded-full ${isOnline ? "bg-green-500" : "bg-gray-300"}`}
    />
  );
}
