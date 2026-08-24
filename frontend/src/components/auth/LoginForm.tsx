"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import type { AxiosError } from "axios";
import { useAuthStore } from "@/store/authStore";
import { isValidEmail } from "@/lib/validation";
import { messageForErrorCode } from "@/lib/errorMessages";
import { connect } from "@/lib/stompClient";
import type { ApiError } from "@/types/domain";

/**
 * npm test 실행 결과(2026-08-21, 사용자 로컬)에서 발견: <input type="email">이 비어있지 않은데
 * 형식이 이상한 값(예: "not-an-email")을 담고 있으면, 제출 버튼 클릭 시 브라우저(및 jsdom)의 네이티브
 * HTML5 제약 검증이 우리 handleSubmit(onSubmit)보다 먼저 개입해 submit 이벤트 자체를 막아버려서,
 * 커스텀 한글 에러 메시지("올바른 이메일 형식을 입력해주세요.")가 전혀 뜨지 않는 문제가 있었다
 * (실제 브라우저에서는 대신 브라우저 자체의 툴팁이 뜬다 - 우리 UX 설계와 다름, business-rules.md의
 * "클라이언트 검증 메시지는 한글로 표시" 요구와 불일치). 경쟁하는 설계 대안이 없는 프레임워크 동작
 * 결함이라 판단해 <form>에 noValidate를 추가해 네이티브 검증을 끄고 우리 검증 로직만 사용하도록 했다.
 */
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
    <form
      onSubmit={handleSubmit}
      noValidate
      className="flex w-full max-w-sm flex-col gap-4"
      data-testid="login-form"
    >
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
