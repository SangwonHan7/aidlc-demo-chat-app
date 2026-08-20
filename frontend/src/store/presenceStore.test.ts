import { describe, expect, it } from "vitest";
import { mergePresence } from "./presenceStore";

describe("mergePresence (example-based, PBT-10 pair for property #4)", () => {
  it("adds a user marked online", () => {
    const result = mergePresence(new Set(), [{ userId: "u1", online: true }]);
    expect(result.has("u1")).toBe(true);
  });

  it("removes a user marked offline", () => {
    const result = mergePresence(new Set(["u1"]), [{ userId: "u1", online: false }]);
    expect(result.has("u1")).toBe(false);
  });

  it("leaves unrelated users untouched", () => {
    const result = mergePresence(new Set(["u1"]), [{ userId: "u2", online: true }]);
    expect(result.has("u1")).toBe(true);
    expect(result.has("u2")).toBe(true);
  });
});
