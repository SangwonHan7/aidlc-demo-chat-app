import { describe, expect, it } from "vitest";
import { mergeIncomingMessage, prependHistoryPage } from "./chatStore";
import type { ChatMessage } from "@/types/domain";

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
