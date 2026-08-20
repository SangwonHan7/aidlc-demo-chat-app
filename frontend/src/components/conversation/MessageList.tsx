"use client";

import { useEffect, useRef } from "react";
import { FixedSizeList, type ListOnScrollProps } from "react-window";
import { useChatStore } from "@/store/chatStore";
import { useAuthStore } from "@/store/authStore";

interface Props {
  channelId: string;
}

const ROW_HEIGHT = 56;
const LIST_HEIGHT = 480;

// NFR Design(Frontend) Question 3 답변 A: react-window로 가상화 - 화면에 보이는 부분만 렌더링.
export function MessageList({ channelId }: Props) {
  const messages = useChatStore((s) => s.messagesByChannel[channelId] ?? []);
  const loadMoreHistory = useChatStore((s) => s.loadMoreHistory);
  const currentUserId = useAuthStore((s) => s.user?.id);
  const listRef = useRef<FixedSizeList>(null);

  useEffect(() => {
    if (messages.length > 0) {
      listRef.current?.scrollToItem(messages.length - 1, "end");
    }
  }, [messages.length]);

  function handleScroll({ scrollOffset }: ListOnScrollProps): void {
    if (scrollOffset === 0) {
      void loadMoreHistory(channelId);
    }
  }

  return (
    <div className="flex-1" data-testid="message-list">
      <FixedSizeList
        ref={listRef}
        height={LIST_HEIGHT}
        width="100%"
        itemCount={messages.length}
        itemSize={ROW_HEIGHT}
        onScroll={handleScroll}
      >
        {({ index, style }) => {
          const message = messages[index];
          const isMine = message.senderId === currentUserId;
          return (
            <div
              style={style}
              data-testid={`message-list-item-${message.id}`}
              className={`flex flex-col px-3 py-1 ${isMine ? "items-end" : "items-start"}`}
            >
              <span className="max-w-xs rounded bg-gray-100 px-3 py-2 text-sm">{message.content}</span>
            </div>
          );
        }}
      </FixedSizeList>
    </div>
  );
}
