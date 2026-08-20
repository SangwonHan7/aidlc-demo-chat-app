"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import type { AxiosError } from "axios";
import { useAuthStore } from "@/store/authStore";
import { isValidEmail } from "@/lib/validation";
import { messageForErrorCode } from "@/lib/errorMessages";
import { connect } from "@/lib/stompClient";
import type { ApiError } from "@/types/domain";

export function LoginForm() {
  const router = useRouter();
  const login = useAuthStore((s) => s.login);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent): Promise<void> {
    e.preventDefault();
    setError(null);

    if (!isValidEmail(email)) {
      setError("올바른 이메일 형식을 입력해주세요.");
      return;
    }

    setSubmitting(true);
    try {
      await login(email, password);
      const accessToken = useAuthStore.getState().accessToken;
      if (accessToken) connect(accessToken);
      router.push("/");
    } catch (err) {
      const apiError = (err as AxiosError<ApiError>).response?.data;
      setError(apiError ? messageForErrorCode(apiError.errorCode, apiError.message) : "로그인에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex w-full max-w-sm flex-col gap-4" data-testid="login-form">
      <h1 className="text-xl font-semibold">QuickChat 로그인</h1>
      <label className="flex flex-col gap-1">
        <span className="text-sm text-gray-600">이메일</span>
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="rounded border px-3 py-2"
          data-testid="login-form-email-input"
        />
      </label>
      <label className="flex flex-col gap-1">
        <span className="text-sm text-gray-600">비밀번호</span>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="rounded border px-3 py-2"
          data-testid="login-form-password-input"
        />
      </label>
      {error && (
        <p className="text-sm text-red-600" data-testid="login-form-error">
          {error}
        </p>
      )}
      <button
        type="submit"
        disabled={submitting}
        className="rounded bg-blue-600 px-3 py-2 text-white disabled:opacity-50"
        data-testid="login-form-submit-button"
      >
        로그인
      </button>
      <a href="/register" className="text-sm text-blue-600" data-testid="login-form-register-link">
        계정이 없으신가요? 회원가입
      </a>
    </form>
  );
}
