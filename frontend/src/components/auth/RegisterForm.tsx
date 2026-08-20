"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import type { AxiosError } from "axios";
import { useAuthStore } from "@/store/authStore";
import { isValidDisplayName, isValidEmail, isValidPassword, passwordStrength } from "@/lib/validation";
import { messageForErrorCode } from "@/lib/errorMessages";
import type { ApiError } from "@/types/domain";

export function RegisterForm() {
  const router = useRouter();
  const register = useAuthStore((s) => s.register);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const strength = passwordStrength(password);

  async function handleSubmit(e: FormEvent): Promise<void> {
    e.preventDefault();
    setError(null);

    if (!isValidEmail(email)) {
      setError("올바른 이메일 형식을 입력해주세요.");
      return;
    }
    if (!isValidPassword(password)) {
      setError("비밀번호는 8자 이상 100자 이하로 입력해주세요.");
      return;
    }
    if (!isValidDisplayName(displayName)) {
      setError("표시 이름을 1자 이상 50자 이하로 입력해주세요.");
      return;
    }

    setSubmitting(true);
    try {
      await register(email, password, displayName);
      router.push("/login");
    } catch (err) {
      const apiError = (err as AxiosError<ApiError>).response?.data;
      setError(apiError ? messageForErrorCode(apiError.errorCode, apiError.message) : "회원가입에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex w-full max-w-sm flex-col gap-4" data-testid="register-form">
      <h1 className="text-xl font-semibold">QuickChat 회원가입</h1>
      <label className="flex flex-col gap-1">
        <span className="text-sm text-gray-600">이메일</span>
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="rounded border px-3 py-2"
          data-testid="register-form-email-input"
        />
      </label>
      <label className="flex flex-col gap-1">
        <span className="text-sm text-gray-600">비밀번호</span>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="rounded border px-3 py-2"
          data-testid="register-form-password-input"
        />
        {password.length > 0 && (
          <span className="text-xs text-gray-500" data-testid="register-form-password-strength">
            비밀번호 강도: {strength.label}
          </span>
        )}
      </label>
      <label className="flex flex-col gap-1">
        <span className="text-sm text-gray-600">표시 이름</span>
        <input
          type="text"
          value={displayName}
          onChange={(e) => setDisplayName(e.target.value)}
          maxLength={50}
          className="rounded border px-3 py-2"
          data-testid="register-form-display-name-input"
        />
        <span className="text-xs text-gray-500" data-testid="register-form-display-name-counter">
          {displayName.length}/50
        </span>
      </label>
      {error && (
        <p className="text-sm text-red-600" data-testid="register-form-error">
          {error}
        </p>
      )}
      <button
        type="submit"
        disabled={submitting}
        className="rounded bg-blue-600 px-3 py-2 text-white disabled:opacity-50"
        data-testid="register-form-submit-button"
      >
        회원가입
      </button>
      <a href="/login" className="text-sm text-blue-600" data-testid="register-form-login-link">
        이미 계정이 있으신가요? 로그인
      </a>
    </form>
  );
}
