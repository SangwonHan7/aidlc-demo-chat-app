import fc from "fast-check";
import { describe, it } from "vitest";
import { nextBackoffDelayMs } from "./backoff";

// PBT-01 속성 #7 (Invariant): n번째 재시도 대기시간은 이전 시도보다 크거나 같고, 상한(30s)을 넘지 않는다.
describe("nextBackoffDelayMs (property-based)", () => {
  it("never exceeds the 30s cap", () => {
    fc.assert(
      fc.property(fc.integer({ min: 1, max: 100 }), (attempt) => {
        return nextBackoffDelayMs(attempt) <= 30000;
      })
    );
  });

  it("is monotonically non-decreasing across consecutive attempts", () => {
    fc.assert(
      fc.property(fc.integer({ min: 1, max: 99 }), (attempt) => {
        return nextBackoffDelayMs(attempt + 1) >= nextBackoffDelayMs(attempt);
      })
    );
  });
});
