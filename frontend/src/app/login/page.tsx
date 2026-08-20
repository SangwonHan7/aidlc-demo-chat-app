"use client";

import { LoginForm } from "@/components/auth/LoginForm";

// frontend-components.md: /login은 앱쉘 없는 독립 페이지.
export default function LoginPage() {
  return (
    <div className="flex h-screen items-center justify-center">
      <LoginForm />
    </div>
  );
}
