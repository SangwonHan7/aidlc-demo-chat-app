import type { Metadata } from "next";
import "./globals.css";
import { ErrorBoundary } from "@/components/common/ErrorBoundary";

export const metadata: Metadata = {
  title: "QuickChat",
  description: "QuickChat - 팀 실시간 메시징",
};

// 루트 레이아웃 자체는 서버 컴포넌트다 (Next.js App Router 요구사항). 실제 화면 로직은
// 전부 "use client" 컴포넌트로 구성된다 (NFR Requirements Question 1 답변 A).
// ErrorBoundary(NFR Requirements Question 4 답변 A)를 최상위에서 감싸 렌더링 예외를 방어한다 -
// Frontend Components Generation 중 컴포넌트만 만들고 실제로 연결하지 않았던 누락을 보완.
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body>
        <ErrorBoundary>{children}</ErrorBoundary>
      </body>
    </html>
  );
}
