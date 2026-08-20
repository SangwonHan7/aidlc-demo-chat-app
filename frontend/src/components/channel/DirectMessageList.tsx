"use client";

import { useEffect, useState } from "react";
import type { AxiosError } from "axios";
import { apiClient } from "@/lib/apiClient";
import { useAuthStore } from "@/store/authStore";
import { useChatStore } from "@/store/chatStore";
import { resolveDirectChannelPeer } from "@/lib/directChannel";
import { PresenceIndicator } from "@/components/presence/PresenceIndicator";
import { messageForErrorCode } from "@/lib/errorMessages";
import type { ApiError, User } from "@/types/domain";

// story 1.2: 사용자를 이메일로 검색해 DM을 시작한다. 기존 DM 목록은 상대방 displayName으로 표시한다
// (channel.name은 화면용이 아님 - lib/directChannel.ts 참고).
export function DirectMessageList() {
  const directChannels = useChatStore((s) => s.channels.filter((c) => c.type === "DIRECT"));
  const activeChannelId = useChatStore((s) => s.activeChannelId);
  const setActiveChannel = useChatStore((s) => s.setActiveChannel);
  const startDirectMessage = useChatStore((s) => s.startDirectMessage);
  const currentUserId = useAuthStore((s) => s.user?.id);
  const [peers, setPeers] = useState<Record<string, User | null>>({});
  const [searchEmail, setSearchEmail] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!currentUserId) return;
    directChannels.forEach((channel) => {
      if (peers[channel.id] !== undefined) return;
      void resolveDirectChannelPeer(channel.id, currentUserId).then((peer) =>
        setPeers((prev) => ({ ...prev, [channel.id]: peer }))
      );
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [directChannels, currentUserId]);

  async function handleStartDirectMessage(): Promise<void> {
    setError(null);
    try {
      const { data: matches } = await apiClient.get<User[]>("/api/users/search", {
        params: { email: searchEmail.trim() },
      });
      if (matches.length === 0) {
        setError("해당 이메일의 사용자를 찾을 수 없습니다.");
        return;
      }
      const channel = await startDirectMessage(matches[0].id);
      setPeers((prev) => ({ ...prev, [channel.id]: matches[0] }));
      setActiveChannel(channel.id);
      setSearchEmail("");
    } catch (err) {
      const apiError = (err as AxiosError<ApiError>).response?.data;
      setError(apiError ? messageForErrorCode(apiError.errorCode, apiError.message) : "DM 시작에 실패했습니다.");
    }
  }

  return (
    <div data-testid="direct-message-list">
      <ul>
        {directChannels.map((channel) => {
          const peer = peers[channel.id];
          return (
            <li key={channel.id}>
              <button
                onClick={() => setActiveChannel(channel.id)}
                data-testid={`direct-message-list-item-${channel.id}`}
                className={`flex w-full items-center gap-2 truncate px-3 py-1 text-left text-sm ${
                  activeChannelId === channel.id ? "bg-blue-100" : ""
                }`}
              >
                {peer && <PresenceIndicator userId={peer.id} />}
                <span className="truncate">{peer?.displayName ?? "..."}</span>
              </button>
            </li>
          );
        })}
      </ul>

      <div className="mt-2 flex gap-1 border-t px-3 pt-2">
        <input
          value={searchEmail}
          onChange={(e) => setSearchEmail(e.target.value)}
          placeholder="이메일로 DM 시작"
          className="flex-1 rounded border px-2 py-1 text-xs"
          data-testid="direct-message-list-search-input"
        />
        <button
          onClick={handleStartDirectMessage}
          data-testid="direct-message-list-search-button"
          className="text-xs text-blue-600"
        >
          시작
        </button>
      </div>
      {error && (
        <p className="px-3 text-xs text-red-600" data-testid="direct-message-list-error">
          {error}
        </p>
      )}
    </div>
  );
}
