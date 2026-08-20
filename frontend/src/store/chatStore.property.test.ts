import fc from "fast-check";
import { describe, it } from "vitest";
import { mergeIncomingMessage, prependHistoryPage } from "./chatStore";
import type { ChatMessage } from "@/types/domain";

function toMessage(id: string, index: number): ChatMessage {
  const sentAt = new Date(Date.UTC(2026, 0, 1, 0, 0, index)).toISOString();
  return { id, channelId: "channel-1", senderId: "sender", content: "content", sentAt };
}

const idsArb = fc.uniqueArray(fc.uuid(), { minLength: 2, maxLength: 30 });

describe("mergeIncomingMessage (property-based)", () => {
  it("is idempotent: merging the same message twice equals merging it once (#1)", () => {
    fc.assert(
      fc.property(idsArb, (ids) => {
        const [incomingId, ...restIds] = ids;
        const base = restIds.map((id, i) => toMessage(id, i));
        const incoming = toMessage(incomingId, restIds.length);
        const once = mergeIncomingMessage(base, incoming);
        const twice = mergeIncomingMessage(once, incoming);
        return JSON.stringify(twice) === JSON.stringify(once);
      })
    );
  });
});

describe("prependHistoryPage (property-based)", () => {
  it("keeps the result sorted by sentAt ascending (#2)", () => {
    fc.assert(
      fc.property(idsArb, (ids) => {
        const mid = Math.floor(ids.length / 2);
        const older = ids.slice(0, mid).map((id, i) => toMessage(id, i));
        const recent = ids.slice(mid).map((id, i) => toMessage(id, mid + i));
        const result = prependHistoryPage(recent, older);
        for (let i = 1; i < result.length; i++) {
          if (result[i - 1].sentAt > result[i].sentAt) return false;
        }
        return true;
      })
    );
  });

  it("contains exactly the union of both pages' ids with no duplicates or omissions (#3)", () => {
    fc.assert(
      fc.property(idsArb, (ids) => {
        const mid = Math.floor(ids.length / 2);
        const olderIds = ids.slice(0, mid);
        const recentIds = ids.slice(mid);
        const older = olderIds.map((id, i) => toMessage(id, i));
        const recent = recentIds.map((id, i) => toMessage(id, mid + i));
        const result = prependHistoryPage(recent, older);
        const resultIds = [...result.map((m) => m.id)].sort();
        const expectedIds = [...olderIds, ...recentIds].sort();
        return JSON.stringify(resultIds) === JSON.stringify(expectedIds);
      })
    );
  });
});
