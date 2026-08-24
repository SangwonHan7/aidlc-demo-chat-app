"use client";

import { useEffect, useRef } from "react";
import { FixedSizeList, type ListOnScrollProps } from "react-window";
import { useChatStore } from "@/store/chatStore";
import { useAuthStore } from "@/store/authStore";
import type { User } from "@/types/domain";

interface Props {
  channelId: string;
  // 2026-08-22 사용자 요청("누가 무슨 말을 했는지 구분이 안 된다")에 대응 - ConversationView가
  // fetchMemberProfilesById()로 미리 불러온 채널 멤버 프로필을 senderId 기준으로 넘겨준다.
  senderProfiles: Record<string, User>;
}

// 발신자 표시이름 한 줄을 넣기 위해 기존 56px에서 늘림 - 내 메시지에도 항상 같은 높이를 확보해야
// react-window(FixedSizeList)가 요구하는 고정 행 높이를 유지할 수 있다.
const ROW_HEIGHT = 72;
const LIST_HEIGHT = 480;

// NFR Design(Frontend) Question 3 답변 A: react-window로 가상화 - 화면에 보이는 부분만 렌더링.
export function MessageList({ channelId, senderProfiles }: Props) {
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
          // 채널에서 이미 나간 사용자가 보낸 과거 메시지는 프로필을 못 찾을 수 있음(memberProfiles.ts
          // 알려진 제약 참고) - 그 경우 표시이름 대신 폴백 문자열을 보여준다.
          const senderName = senderProfiles[message.senderId]?.displayName ?? "알 수 없는 사용자";
          return (
            <div
              style={style}
              data-testid={`message-list-item-${message.id}`}
              className={`flex flex-col px-3 py-1 ${isMine ? "items-end" : "items-start"}`}
            >
              {!isMine && (
                <span
                  className="mb-0.5 px-1 text-xs text-gray-500"
                  data-testid={`message-list-item-sender-${message.id}`}
                >
                  {senderName}
                </span>
              )}
              <span className="max-w-xs rounded bg-gray-100 px-3 py-2 text-sm">{message.content}</span>
            </div>
          );
        }}
      </FixedSizeList>
    </div>
  );
}
