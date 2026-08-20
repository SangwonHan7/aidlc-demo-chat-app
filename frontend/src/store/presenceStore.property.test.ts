import fc from "fast-check";
import { describe, it } from "vitest";
import { mergePresence } from "./presenceStore";
import type { PresenceStatus } from "@/types/domain";

const statusArb: fc.Arbitrary<PresenceStatus> = fc.record({
  userId: fc.uuid(),
  online: fc.boolean(),
});

// PBT-01 속성 #4 (Idempotence): 동일한 presence 응답을 여러 번 반영해도 최종 집합은 동일하게 유지된다.
describe("mergePresence (property-based)", () => {
  it("applying the same status list twice yields the same set as applying it once", () => {
    fc.assert(
      fc.property(fc.array(statusArb, { maxLength: 20 }), (statuses) => {
        const once = mergePresence(new Set(), statuses);
        const twice = mergePresence(once, statuses);
        return (
          once.size === twice.size && [...once].every((id) => twice.has(id))
        );
      })
    );
  });
});
