import { beforeEach, describe, expect, it } from "vitest";
import { mergeIncomingMessage, prependHistoryPage, useChatStore } from "./chatStore";
import type { Channel, ChatMessage } from "@/types/domain";

function message(id: string, sentAt: string): ChatMessage {
  return { id, channelId: "channel-1", senderId: "user-1", content: "hi", sentAt };
}

describe("mergeIncomingMessage (example-based, PBT-10 pair for property #1)", () => {
  it("appends a new message in sentAt order", () => {
    const base = [message("a", "2026-01-01T00:00:00Z")];
    const result = mergeIncomingMessage(base, message("b", "2026-01-01T00:01:00Z"));
    expect(result.map((m) => m.id)).toEqual(["a", "b"]);
  });

  it("does not duplicate a message that already exists", () => {
    const base = [message("a", "2026-01-01T00:00:00Z")];
    const result = mergeIncomingMessage(base, message("a", "2026-01-01T00:00:00Z"));
    expect(result).toHaveLength(1);
  });
});

describe("prependHistoryPage (example-based, PBT-10 pair for properties #2/#3)", () => {
  it("prepends older messages while keeping ascending order", () => {
    const recent = [message("b", "2026-01-01T00:01:00Z")];
    const older = [message("a", "2026-01-01T00:00:00Z")];
    const result = prependHistoryPage(recent, older);
    expect(result.map((m) => m.id)).toEqual(["a", "b"]);
  });

  it("does not duplicate ids that appear on both pages", () => {
    const recent = [message("a", "2026-01-01T00:00:00Z")];
    const result = prependHistoryPage(recent, [message("a", "2026-01-01T00:00:00Z")]);
    expect(result).toHaveLength(1);
  });
});

function channel(id: string): Channel {
  return {
    id,
    name: "general",
    type: "GROUP",
    visibility: "PUBLIC",
    ownerId: "user-1",
    status: "ACTIVE",
    createdAt: "2026-01-01T00:00:00Z",
  };
}

// 2026-08-21 사용자 발견: 새로고침하면 activeChannelId가 초기화되어 채팅 화면이 비어 보이는 문제에
// 대한 회귀 테스트. setActiveChannel/restoreActiveChannel이 실제 localStorage에 쓰고 읽는 것까지
// 검증한다(authStore의 토큰 저장/복원 테스트와 같은 성격).
describe("chatStore.setActiveChannel / restoreActiveChannel (새로고침 후 채널 복원)", () => {
  beforeEach(() => {
    window.localStorage.clear();
    useChatStore.setState({ channels: [], activeChannelId: null });
  });

  it("setActiveChannel으로 선택한 채널 id를 localStorage에 저장한다", () => {
    useChatStore.getState().setActiveChannel("channel-1");
    expect(window.localStorage.getItem("quickchat.activeChannelId")).toBe("channel-1");
    expect(useChatStore.getState().activeChannelId).toBe("channel-1");
  });

  it("저장된 채널이 로드된 채널 목록에 있으면 activeChannelId로 복원한다", () => {
    window.localStorage.setItem("quickchat.activeChannelId", "channel-1");
    useChatStore.setState({ channels: [channel("channel-1")] });

    useChatStore.getState().restoreActiveChannel();

    expect(useChatStore.getState().activeChannelId).toBe("channel-1");
  });

  it("저장된 채널이 더 이상 목록에 없으면(제외/삭제) 복원하지 않고 저장값을 지운다", () => {
    window.localStorage.setItem("quickchat.activeChannelId", "channel-removed");
    useChatStore.setState({ channels: [channel("channel-1")] });

    useChatStore.getState().restoreActiveChannel();

    expect(useChatStore.getState().activeChannelId).toBeNull();
    expect(window.localStorage.getItem("quickchat.activeChannelId")).toBeNull();
  });

  it("reset()은 저장된 activeChannelId도 함께 지운다(다른 사용자 재로그인 시 잔류 방지)", () => {
    useChatStore.getState().setActiveChannel("channel-1");
    useChatStore.getState().reset();
    expect(window.localStorage.getItem("quickchat.activeChannelId")).toBeNull();
  });
});
