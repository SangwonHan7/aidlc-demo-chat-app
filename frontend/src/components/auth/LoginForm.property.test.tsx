import fc from "fast-check";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, fireEvent, cleanup, waitFor } from "@testing-library/react";
import { LoginForm } from "./LoginForm";
import { useAuthStore } from "@/store/authStore";
import { isValidEmail } from "@/lib/validation";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}));

// Step 6 요구사항: "폼 검증 컴포넌트에 대한 입력 속성 테스트 - 임의 문자열에 대해 validation.ts와
// 동일한 판정을 내리는지". validation.property.test.ts는 순수 함수 자체를 검증하고, 이 테스트는
// LoginForm이 그 함수를 화면에 정확히 연결했는지(오타/조건 반전 등 배선 실수가 없는지)를 검증한다.
// login()을 목으로 대체해 실제 네트워크 호출(성공 케이스) 없이 클라이언트 검증 게이트만 격리한다.
describe("LoginForm (property-based) - error banner presence must match isValidEmail", () => {
  beforeEach(() => {
    useAuthStore.setState({ login: vi.fn().mockResolvedValue(undefined) });
  });

  it("shows the email-format error iff isValidEmail(value) is false", async () => {
    await fc.assert(
      fc.asyncProperty(fc.string({ maxLength: 60 }), async (value) => {
        // cleanup()을 성공 경로 끝에서만 호출하면, 한 iteration의 assert가 실패해 예외가 던져질 때
        // cleanup을 건너뛰어 DOM에 이전 render()가 남는다 - 다음 iteration이 같은 data-testid를 둘
        // 찾아내는 "Found multiple elements" 2차 오류로 원래 실패 원인을 가려버린다(2026-08-21 npm test
        // 로그에서 실제로 관찰됨). try/finally로 성공/실패 모두 매 iteration 시작 전에 정리되도록 한다.
        cleanup();
        try {
          render(<LoginForm />);
          fireEvent.change(screen.getByTestId("login-form-email-input"), { target: { value } });
          fireEvent.click(screen.getByTestId("login-form-submit-button"));

          if (isValidEmail(value)) {
            await waitFor(() => expect(screen.queryByTestId("login-form-error")).toBeNull());
          } else {
            await waitFor(() =>
              expect(screen.getByTestId("login-form-error")).toHaveTextContent("올바른 이메일 형식")
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
