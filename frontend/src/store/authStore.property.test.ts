import fc from "fast-check";
import { describe, it } from "vitest";
import { nextAuthState, type AuthTokenState } from "./authStore";

const tokenStateArb: fc.Arbitrary<AuthTokenState> = fc.record({
  accessToken: fc.option(fc.string(), { nil: null }),
  refreshToken: fc.option(fc.string(), { nil: null }),
  isAuthenticated: fc.boolean(),
});

// PBT-01 속성 #5 (Invariant): 성공 시 항상 유효한 토큰이 저장되고, 실패 시에는 명시적 로그아웃 전까지
// 기존 토큰을 지우지 않는다.
describe("nextAuthState (property-based)", () => {
  it("always stores the new, defined tokens on success", () => {
    fc.assert(
      fc.property(tokenStateArb, fc.string(), fc.string(), (current, accessToken, refreshToken) => {
        const result = nextAuthState(current, { ok: true, tokens: { accessToken, refreshToken } });
        return (
          result.accessToken === accessToken && result.refreshToken === refreshToken && result.isAuthenticated
        );
      })
    );
  });

  it("never changes the stored tokens on failure", () => {
    fc.assert(
      fc.property(tokenStateArb, (current) => {
        const result = nextAuthState(current, { ok: false });
        return result.accessToken === current.accessToken && result.refreshToken === current.refreshToken;
      })
    );
  });
});
