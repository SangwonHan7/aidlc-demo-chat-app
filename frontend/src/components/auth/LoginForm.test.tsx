import { describe, expect, it, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { LoginForm } from "./LoginForm";

// LoginForm은 App Router의 useRouter()를 항상 호출하므로(제출 성공 여부와 무관), RTL 단독 환경에는
// Router context가 없어 목으로 대체한다.
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}));

describe("LoginForm (example-based, PBT-10 pair for LoginForm.property.test.tsx)", () => {
  it("shows an inline validation error for a malformed email and blocks submission", () => {
    render(<LoginForm />);

    fireEvent.change(screen.getByTestId("login-form-email-input"), {
      target: { value: "not-an-email" },
    });
    fireEvent.click(screen.getByTestId("login-form-submit-button"));

    expect(screen.getByTestId("login-form-error")).toHaveTextContent("올바른 이메일 형식");
  });

  it("shows no error banner before any submit attempt", () => {
    render(<LoginForm />);
    expect(screen.queryByTestId("login-form-error")).toBeNull();
  });
});
