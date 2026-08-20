import { describe, expect, it } from "vitest";
import { nextAuthState } from "./authStore";

describe("nextAuthState (example-based, PBT-10 pair for property #5)", () => {
  it("replaces tokens and marks authenticated on success", () => {
    const current = { accessToken: "old-access", refreshToken: "old-refresh", isAuthenticated: true };
    const result = nextAuthState(current, {
      ok: true,
      tokens: { accessToken: "new-access", refreshToken: "new-refresh" },
    });
    expect(result).toEqual({
      accessToken: "new-access",
      refreshToken: "new-refresh",
      isAuthenticated: true,
    });
  });

  it("keeps the previous tokens when refresh fails", () => {
    const current = { accessToken: "old-access", refreshToken: "old-refresh", isAuthenticated: true };
    const result = nextAuthState(current, { ok: false });
    expect(result).toEqual(current);
  });

  it("stays unauthenticated when there was no token to begin with and refresh fails", () => {
    const current = { accessToken: null, refreshToken: null, isAuthenticated: false };
    const result = nextAuthState(current, { ok: false });
    expect(result.isAuthenticated).toBe(false);
  });
});
