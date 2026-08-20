"use client";

import { useEffect } from "react";
import { useChatStore } from "@/store/chatStore";

// story 1.3: 참여 중인 GROUP 채널은 클릭해서 열고, 아직 참여하지 않은 PUBLIC 채널은 "참여" 버튼으로
// 바로 참여할 수 있다. DIRECT 채널은 DirectMessageList가 별도로 보여준다.
export function ChannelList() {
  const joinedChannels = useChatStore((s) => s.channels.filter((c) => c.type === "GROUP"));
  const discoverableChannels = useChatStore((s) => s.discoverableChannels);
  const activeChannelId = useChatStore((s) => s.activeChannelId);
  const setActiveChannel = useChatStore((s) => s.setActiveChannel);
  const joinChannel = useChatStore((s) => s.joinChannel);
  const loadChannels = useChatStore((s) => s.loadChannels);
  const loadDiscoverableChannels = useChatStore((s) => s.loadDiscoverableChannels);

  useEffect(() => {
    void loadDiscoverableChannels();
  }, [loadDiscoverableChannels]);

  const joinedIds = new Set(joinedChannels.map((c) => c.id));
  const notYetJoined = discoverableChannels.filter((c) => !joinedIds.has(c.id));

  async function handleJoin(channelId: string): Promise<void> {
    await joinChannel(channelId);
    await Promise.all([loadChannels(), loadDiscoverableChannels()]);
    setActiveChannel(channelId);
  }

  return (
    <div data-testid="channel-list">
      <ul>
        {joinedChannels.map((channel) => (
          <li key={channel.id}>
            <button
              onClick={() => setActiveChannel(channel.id)}
              data-testid={`channel-list-item-${channel.id}`}
              className={`w-full truncate px-3 py-1 text-left text-sm ${
                activeChannelId === channel.id ? "bg-blue-100" : ""
              } ${channel.status === "ARCHIVED" ? "text-gray-400" : ""}`}
            >
              # {channel.name}
              {channel.status === "ARCHIVED" && " (보관됨)"}
            </button>
          </li>
        ))}
      </ul>

      {notYetJoined.length > 0 && (
        <div className="mt-2 border-t pt-2">
          <p className="px-3 text-xs text-gray-400">참여 가능한 공개 채널</p>
          <ul>
            {notYetJoined.map((channel) => (
              <li key={channel.id} className="flex items-center justify-between px-3 py-1 text-sm">
                <span className="truncate text-gray-500"># {channel.name}</span>
                <button
                  onClick={() => handleJoin(channel.id)}
                  data-testid={`channel-list-join-button-${channel.id}`}
                  className="text-xs text-blue-600"
                >
                  참여
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
