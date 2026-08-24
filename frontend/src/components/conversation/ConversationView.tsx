"use client";

import { useEffect, useState } from "react";
import { useChatStore } from "@/store/chatStore";
import { useAuthStore } from "@/store/authStore";
import { usePresenceStore } from "@/store/presenceStore";
import { subscribeToChannel } from "@/lib/stompClient";
import { resolveDirectChannelPeer } from "@/lib/directChannel";
import { fetchMemberProfilesById } from "@/lib/memberProfiles";
import { ConversationHeader } from "./ConversationHeader";
import { MessageList } from "./MessageList";
import { MessageInput } from "./MessageInput";
import { MemberManagementPanel } from "@/components/channel/MemberManagementPanel";
import type { ChatMessage, User } from "@/types/domain";

const PRESENCE_POLL_INTERVAL_MS = 30000;

export function ConversationView() {
  const activeChannelId = useChatStore((s) => s.activeChannelId);
  const channel = useChatStore((s) => s.channels.find((c) => c.id === activeChannelId));
  const receiveMessage = useChatStore((s) => s.receiveMessage);
  const loadHistory = useChatStore((s) => s.loadHistory);
  const refreshPresence = usePresenceStore((s) => s.refreshPresence);
  const currentUserId = useAuthStore((s) => s.user?.id);
  const [managingMembers, setManagingMembers] = useState(false);
  const [directPeer, setDirectPeer] = useState<User | null>(null);
  const [senderProfiles, setSenderProfiles] = useState<Record<string, User>>({});

  useEffect(() => {
    if (!activeChannelId) return;
    void loadHistory(activeChannelId);
    subscribeToChannel(activeChannelId, (payload) => receiveMessage(payload as ChatMessage));
  }, [activeChannelId, loadHistory, receiveMessage]);

  // 채팅에서 발신자를 구분할 수 있도록(2026-08-22 사용자 요청) 채널 멤버 프로필을 미리 불러와
  // MessageList에 넘긴다. 새 멤버가 초대되는 등 목록이 바뀔 수 있어 채널이 바뀔 때마다 다시 불러온다.
  useEffect(() => {
    setSenderProfiles({});
    if (!activeChannelId) return;
    void fetchMemberProfilesById(activeChannelId).then(setSenderProfiles);
  }, [activeChannelId]);

  // DIRECT 채널은 channel.name이 화면용 문자열이 아니므로(lib/directChannel.ts 참고), 상대방
  // displayName을 별도로 조회한다.
  useEffect(() => {
    setDirectPeer(null);
    if (!channel || channel.type !== "DIRECT" || !currentUserId) return;
    void resolveDirectChannelPeer(channel.id, currentUserId).then(setDirectPeer);
  }, [channel, currentUserId]);

  useEffect(() => {
    if (!channel || !currentUserId) return;
    const userIds =
      channel.type === "DIRECT" && directPeer ? [directPeer.id, currentUserId] : [currentUserId];

    void refreshPresence(userIds);
    const interval = setInterval(() => void refreshPresence(userIds), PRESENCE_POLL_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [channel, currentUserId, directPeer, refreshPresence]);

  if (!channel || !activeChannelId) return null;

  const isOwner = channel.ownerId === currentUserId;
  const displayName = channel.type === "DIRECT" ? directPeer?.displayName ?? "..." : channel.name;

  return (
    <div className="flex h-full flex-col" data-testid="conversation-view">
      <ConversationHeader
        channelName={displayName}
        isOwner={isOwner}
        otherUserId={channel.type === "DIRECT" ? directPeer?.id : undefined}
        onManageMembers={() => setManagingMembers(true)}
      />
      <MessageList channelId={activeChannelId} senderProfiles={senderProfiles} />
      <MessageInput channelId={activeChannelId} />
      {managingMembers && (
        <MemberManagementPanel channelId={activeChannelId} onClose={() => setManagingMembers(false)} />
      )}
    </div>
  );
}
