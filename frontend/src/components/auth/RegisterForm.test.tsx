import { describe, expect, it, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { RegisterForm } from "./RegisterForm";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}));

describe("RegisterForm (example-based, PBT-10 pair for RegisterForm.property.test.tsx)", () => {
  it("shows an inline error for a malformed email", () => {
    render(<RegisterForm />);
    fireEvent.change(screen.getByTestId("register-form-email-input"), {
      target: { value: "not-an-email" },
    });
    fireEvent.click(screen.getByTestId("register-form-submit-button"));

    expect(screen.getByTestId("register-form-error")).toHaveTextContent("올바른 이메일 형식");
  });

  it("shows an inline error for a too-short password once the email is valid", () => {
    render(<RegisterForm />);
    fireEvent.change(screen.getByTestId("register-form-email-input"), {
      target: { value: "user@example.com" },
    });
    fireEvent.change(screen.getByTestId("register-form-password-input"), {
      target: { value: "short" },
    });
    fireEvent.click(screen.getByTestId("register-form-submit-button"));

    expect(screen.getByTestId("register-form-error")).toHaveTextContent("8자 이상");
  });

  it("shows an inline error for a blank display name once email/password are valid", () => {
    render(<RegisterForm />);
    fireEvent.change(screen.getByTestId("register-form-email-input"), {
      target: { value: "user@example.com" },
    });
    fireEvent.change(screen.getByTestId("register-form-password-input"), {
      target: { value: "password123" },
    });
    fireEvent.change(screen.getByTestId("register-form-display-name-input"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByTestId("register-form-submit-button"));

    expect(screen.getByTestId("register-form-error")).toHaveTextContent("표시 이름");
  });

  it("reflects the display name length in the counter", () => {
    render(<RegisterForm />);
    fireEvent.change(screen.getByTestId("register-form-display-name-input"), {
      target: { value: "한상원" },
    });
    expect(screen.getByTestId("register-form-display-name-counter")).toHaveTextContent("3/50");
  });

  it("shows a password strength label only after the user starts typing a password", () => {
    render(<RegisterForm />);
    expect(screen.queryByTestId("register-form-password-strength")).toBeNull();

    fireEvent.change(screen.getByTestId("register-form-password-input"), {
      target: { value: "longerpassword123" },
    });
    expect(screen.getByTestId("register-form-password-strength")).toHaveTextContent("강함");
  });
});
