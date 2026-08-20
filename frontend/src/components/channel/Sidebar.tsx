"use client";

import { useState } from "react";
import { useAuthStore } from "@/store/authStore";
import { useChatStore } from "@/store/chatStore";
import { usePresenceStore } from "@/store/presenceStore";
import { disconnect } from "@/lib/stompClient";
import { ChannelList } from "@/components/channel/ChannelList";
import { DirectMessageList } from "@/components/channel/DirectMessageList";
import { CreateChannelModal } from "@/components/channel/CreateChannelModal";

// business-rules.md 앱 셸(Question 1 답변 A, Slack 스타일): 좌측 고정 사이드바 = 채널 목록 + DM 목록 +
// 채널 생성 + 계정 영역. 로그아웃 시 WS 연결 해제와 다른 스토어 초기화까지 함께 책임진다.
export function Sidebar() {
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const [creatingChannel, setCreatingChannel] = useState(false);

  function handleLogout(): void {
    disconnect();
    useChatStore.getState().reset();
    usePresenceStore.getState().reset();
    logout();
  }

  return (
    <aside className="flex h-full w-64 flex-col border-r bg-gray-50" data-testid="sidebar">
      <div className="flex items-center justify-between border-b px-3 py-3">
        <span className="font-semibold">QuickChat</span>
        <button
          onClick={() => setCreatingChannel(true)}
          data-testid="sidebar-create-channel-button"
          className="text-sm text-blue-600"
        >
          + 새 채널
        </button>
      </div>

      <div className="flex-1 overflow-y-auto py-2">
        <p className="px-3 pb-1 text-xs font-medium text-gray-400">채널</p>
        <ChannelList />

        <p className="mt-3 px-3 pb-1 text-xs font-medium text-gray-400">다이렉트 메시지</p>
        <DirectMessageList />
      </div>

      <div className="flex items-center justify-between border-t px-3 py-2">
        <span className="truncate text-sm text-gray-700" data-testid="sidebar-current-user">
          {user?.displayName}
        </span>
        <button
          onClick={handleLogout}
          data-testid="sidebar-logout-button"
          className="text-xs text-gray-500"
        >
          로그아웃
        </button>
      </div>

      {creatingChannel && <CreateChannelModal onClose={() => setCreatingChannel(false)} />}
    </aside>
  );
}
