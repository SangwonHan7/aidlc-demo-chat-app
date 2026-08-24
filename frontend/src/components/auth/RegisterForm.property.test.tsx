import fc from "fast-check";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, fireEvent, cleanup, waitFor } from "@testing-library/react";
import { RegisterForm } from "./RegisterForm";
import { useAuthStore } from "@/store/authStore";
import { isValidDisplayName, isValidPassword } from "@/lib/validation";
import type { User } from "@/types/domain";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}));

const VALID_EMAIL = "user@example.com";
const VALID_PASSWORD = "password123";
const VALID_DISPLAY_NAME = "Tester";
const RESOLVED_USER: User = { id: "u1", email: VALID_EMAIL, displayName: VALID_DISPLAY_NAME };

// register()를 목으로 대체해, 필드가 전부 유효한 경우(register 실제 호출)에도 네트워크를 타지 않게 한다.
// 각 속성은 나머지 두 필드를 고정된 유효값으로 두어(RegisterForm의 검증 순서: email -> password ->
// displayName) 대상 필드 하나만 격리해서 검증한다.
describe("RegisterForm (property-based) - error banner presence must match validation.ts", () => {
  beforeEach(() => {
    useAuthStore.setState({ register: vi.fn().mockResolvedValue(RESOLVED_USER) });
  });

  it("shows the password error iff isValidPassword(value) is false (email/displayName fixed valid)", async () => {
    await fc.assert(
      fc.asyncProperty(fc.string({ maxLength: 120 }), async (password) => {
        // cleanup()을 성공 경로 끝에서만 부르면 실패 시 DOM이 남아 다음 iteration이 원인을 알 수 없는
        // "multiple elements" 오류로 가려진다(LoginForm.property.test.tsx에서 실제 관찰됨) - 매
        // iteration 시작 전 정리 + try/finally로 성공/실패 모두 정리되게 한다.
        cleanup();
        try {
          render(<RegisterForm />);
          fireEvent.change(screen.getByTestId("register-form-email-input"), {
            target: { value: VALID_EMAIL },
          });
          fireEvent.change(screen.getByTestId("register-form-password-input"), {
            target: { value: password },
          });
          fireEvent.change(screen.getByTestId("register-form-display-name-input"), {
            target: { value: VALID_DISPLAY_NAME },
          });
          fireEvent.click(screen.getByTestId("register-form-submit-button"));

          if (isValidPassword(password)) {
            await waitFor(() => expect(screen.queryByTestId("register-form-error")).toBeNull());
          } else {
            await waitFor(() =>
              expect(screen.getByTestId("register-form-error")).toHaveTextContent("8자 이상")
            );
          }
        } finally {
          cleanup();
        }
      }),
      { numRuns: 25 }
    );
  });

  it("shows the display-name error iff isValidDisplayName(value) is false (email/password fixed valid)", async () => {
    await fc.assert(
      fc.asyncProperty(fc.string({ maxLength: 60 }), async (displayName) => {
        cleanup();
        try {
          render(<RegisterForm />);
          fireEvent.change(screen.getByTestId("register-form-email-input"), {
            target: { value: VALID_EMAIL },
          });
          fireEvent.change(screen.getByTestId("register-form-password-input"), {
            target: { value: VALID_PASSWORD },
          });
          fireEvent.change(screen.getByTestId("register-form-display-name-input"), {
            target: { value: displayName },
          });
          fireEvent.click(screen.getByTestId("register-form-submit-button"));

          if (isValidDisplayName(displayName)) {
            await waitFor(() => expect(screen.queryByTestId("register-form-error")).toBeNull());
          } else {
            await waitFor(() =>
              expect(screen.getByTestId("register-form-error")).toHaveTextContent("표시 이름")
            );
          }
        } finally {
          cleanup();
        }
      }),
      { numRuns: 25 }
    );
  });
});
