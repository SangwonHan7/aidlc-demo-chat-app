import { describe, expect, it } from "vitest";
import { nextBackoffDelayMs } from "./backoff";

describe("nextBackoffDelayMs (example-based, PBT-10 pair for property #7)", () => {
  it("returns 1s, 2s, 4s, 8s for the first four attempts", () => {
    expect(nextBackoffDelayMs(1)).toBe(1000);
    expect(nextBackoffDelayMs(2)).toBe(2000);
    expect(nextBackoffDelayMs(3)).toBe(4000);
    expect(nextBackoffDelayMs(4)).toBe(8000);
  });

  it("clamps to 30s once the exponential value exceeds the cap", () => {
    expect(nextBackoffDelayMs(6)).toBe(30000);
    expect(nextBackoffDelayMs(20)).toBe(30000);
  });

  it("rejects non-positive or non-integer attempts", () => {
    expect(() => nextBackoffDelayMs(0)).toThrow();
    expect(() => nextBackoffDelayMs(-1)).toThrow();
    expect(() => nextBackoffDelayMs(1.5)).toThrow();
  });
});
