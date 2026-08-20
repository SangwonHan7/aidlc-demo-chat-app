"use client";

import { useChatStore } from "@/store/chatStore";
import { ConversationView } from "@/components/conversation/ConversationView";
import { EmptyState } from "./EmptyState";

// business-rules.md 앱 셸: 활성 채널이 있으면 대화 화면을, 없으면 안내 화면을 보여준다.
export function MainPanel() {
  const activeChannelId = useChatStore((s) => s.activeChannelId);

  return (
    <main className="flex-1 overflow-hidden" data-testid="main-panel">
      {activeChannelId ? <ConversationView /> : <EmptyState />}
    </main>
  );
}
