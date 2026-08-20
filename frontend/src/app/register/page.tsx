"use client";

import { RegisterForm } from "@/components/auth/RegisterForm";

// frontend-components.md: /register는 앱쉘 없는 독립 페이지.
export default function RegisterPage() {
  return (
    <div className="flex h-screen items-center justify-center">
      <RegisterForm />
    </div>
  );
}
