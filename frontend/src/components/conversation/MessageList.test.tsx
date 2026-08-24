import { beforeEach, describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { MessageList } from "./MessageList";
import { useChatStore } from "@/store/chatStore";
import { useAuthStore } from "@/store/authStore";
import type { ChatMessage, User } from "@/types/domain";

const CHANNEL_ID = "channel-1";

function message(id: string, senderId: string, content: string): ChatMessage {
  return { id, channelId: CHANNEL_ID, senderId, content, sentAt: "2026-01-01T00:00:00Z" };
}

function user(id: string, displayName: string): User {
  return { id, email: `${id}@example.com`, displayName };
}

// 2026-08-22 사용자 요청("채팅에서 누가 무슨 말을 했는지 구분이 안 된다")에 대한 회귀 테스트.
// senderProfiles는 ConversationView가 fetchMemberProfilesById()로 미리 불러와 내려주는 값을 흉내낸다.
describe("MessageList (example-based) - 발신자 표시", () => {
  beforeEach(() => {
    useChatStore.setState({ messagesByChannel: {}, nextCursorByChannel: {} });
    useAuthStore.setState({ user: user("me", "나") });
  });

  it("다른 사용자의 메시지에는 표시이름을 보여준다", () => {
    useChatStore.setState({ messagesByChannel: { [CHANNEL_ID]: [message("m1", "other", "안녕하세요")] } });

    render(<MessageList channelId={CHANNEL_ID} senderProfiles={{ other: user("other", "상대방") }} />);

    expect(screen.getByTestId("message-list-item-sender-m1")).toHaveTextContent("상대방");
  });

  it("내가 보낸 메시지에는 표시이름을 보여주지 않는다(정렬로 이미 구분됨)", () => {
    useChatStore.setState({ messagesByChannel: { [CHANNEL_ID]: [message("m2", "me", "안녕하세요")] } });

    render(<MessageList channelId={CHANNEL_ID} senderProfiles={{}} />);

    expect(screen.queryByTestId("message-list-item-sender-m2")).toBeNull();
  });

  it("채널을 나간 사용자 등 프로필을 찾을 수 없으면 폴백 문자열을 보여준다", () => {
    useChatStore.setState({ messagesByChannel: { [CHANNEL_ID]: [message("m3", "left-user", "안녕하세요")] } });

    render(<MessageList channelId={CHANNEL_ID} senderProfiles={{}} />);

    expect(screen.getByTestId("message-list-item-sender-m3")).toHaveTextContent("알 수 없는 사용자");
  });
});
