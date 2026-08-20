// business-rules.md 클라이언트 검증 규칙 - Backend Bean Validation과 동일한 조건식을 목표로 한다.
// (RegisterRequest: @Email @NotBlank / @Size(min=8,max=100) / @NotBlank @Size(max=50))
//
// 알려진 제약(투명성 목적): 이메일 검증은 Backend가 실제 사용하는 Hibernate Validator의 @Email
// 정규식을 바이트 단위로 복제한 것이 아니라, 동등한 의도를 가진 단순화된 정규식이다. 아주 드문
// edge-case 이메일 문자열(예: 특수한 quoted-string 로컬파트)에서는 두 판정이 달라질 수 있다.
// 완전한 일치를 보장하려면 두 언어가 검증기를 공유해야 하므로 Frontend 단독 패스의 범위를 벗어난다.
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function isValidEmail(value: string): boolean {
  return value.length > 0 && EMAIL_REGEX.test(value);
}

export function isValidPassword(value: string): boolean {
  return value.length >= 8 && value.length <= 100;
}

export function isValidDisplayName(value: string): boolean {
  const trimmed = value.trim();
  return trimmed.length >= 1 && value.length <= 50;
}

export interface PasswordStrength {
  label: "약함" | "보통" | "강함";
  score: 0 | 1 | 2 | 3;
}

/** 순수 표시용 UX 강화 - 서버 전송 값이나 검증 결과에는 영향 없음 (NFR Requirements Question 6 답변 B). */
export function passwordStrength(value: string): PasswordStrength {
  let score = 0;
  if (value.length >= 8) score += 1;
  if (value.length >= 12) score += 1;
  if (/[0-9]/.test(value) && /[a-zA-Z]/.test(value)) score += 1;

  if (score >= 3) return { label: "강함", score: 3 };
  if (score >= 2) return { label: "보통", score: 2 };
  return { label: "약함", score: score as 0 | 1 };
}
