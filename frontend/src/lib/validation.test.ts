import { describe, expect, it } from "vitest";
import { isValidDisplayName, isValidEmail, isValidPassword, passwordStrength } from "./validation";

describe("validation (example-based, PBT-10 pair for property #6)", () => {
  it("accepts a well-formed email", () => {
    expect(isValidEmail("user@example.com")).toBe(true);
  });

  it("rejects an email without @", () => {
    expect(isValidEmail("user-example.com")).toBe(false);
  });

  it("accepts passwords between 8 and 100 characters", () => {
    expect(isValidPassword("password123")).toBe(true);
  });

  it("rejects passwords shorter than 8 characters", () => {
    expect(isValidPassword("short")).toBe(false);
  });

  it("rejects blank display names", () => {
    expect(isValidDisplayName("   ")).toBe(false);
  });

  it("reports increasing password strength for longer, mixed passwords", () => {
    expect(passwordStrength("weak").label).toBe("약함");
    expect(passwordStrength("longerpassword123").label).toBe("강함");
  });
});
