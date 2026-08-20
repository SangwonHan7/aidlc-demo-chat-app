"use client";

import { PresenceIndicator } from "@/components/presence/PresenceIndicator";

interface Props {
  channelName: string;
  isOwner: boolean;
  otherUserId?: string;
  onManageMembers: () => void;
}

export function ConversationHeader({ channelName, isOwner, otherUserId, onManageMembers }: Props) {
  return (
    <div className="flex items-center justify-between border-b p-3" data-testid="conversation-header">
      <div className="flex items-center gap-2">
        <span className="font-medium" data-testid="conversation-header-title">
          {channelName}
        </span>
        {otherUserId && <PresenceIndicator userId={otherUserId} />}
      </div>
      {isOwner && (
        <button
          onClick={onManageMembers}
          className="text-sm text-blue-600"
          data-testid="conversation-header-manage-members-button"
        >
          멤버 관리
        </button>
      )}
    </div>
  );
}
