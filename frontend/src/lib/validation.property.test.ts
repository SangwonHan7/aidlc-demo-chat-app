import fc from "fast-check";
import { describe, it } from "vitest";
import { isValidDisplayName, isValidEmail, isValidPassword } from "./validation";

// PBT-01 속성 #6 (Oracle): 이메일은 fast-check의 내장 fc.emailAddress() 생성기(우리 구현과는 독립적인
// 참조 기준)가 만들어낸 값에 대해 항상 true여야 한다. 비밀번호/표시이름은 Backend의 Hibernate
// Validator를 JS에서 재현할 독립 오라클이 없으므로(validation.ts 상단 "알려진 제약" 참고),
// 문서화된 길이 불변식을 직접 검증하는 것으로 대체했다 - 엄밀한 의미의 Oracle보다는 약한 보증이다.
describe("validation (property-based)", () => {
  it("accepts every address produced by an independent email generator", () => {
    fc.assert(fc.property(fc.emailAddress(), (email) => isValidEmail(email)));
  });

  it("always rejects strings without an @ character", () => {
    fc.assert(
      fc.property(
        fc.string().filter((s) => !s.includes("@")),
        (value) => isValidEmail(value) === false
      )
    );
  });

  it("accepts exactly the documented password length range (8-100)", () => {
    fc.assert(
      fc.property(fc.string({ minLength: 8, maxLength: 100 }), (value) => isValidPassword(value) === true)
    );
    fc.assert(
      fc.property(fc.string({ minLength: 0, maxLength: 7 }), (value) => isValidPassword(value) === false)
    );
  });

  it("accepts exactly the documented display name length range (1-50, trimmed)", () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 1, maxLength: 50 }).filter((s) => s.trim().length >= 1),
        (value) => isValidDisplayName(value) === true
      )
    );
  });
});
