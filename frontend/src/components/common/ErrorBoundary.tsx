"use client";

import { Component, type ReactNode } from "react";

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
}

// NFR Requirements Question 4 답변 A: 별도 모니터링 도구 없이 콘솔 로그 + 최상위 Error Boundary로만 방어.
export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: unknown, info: unknown): void {
    // eslint-disable-next-line no-console
    console.error("[QuickChat] Unhandled rendering error", error, info);
  }

  render(): ReactNode {
    if (this.state.hasError) {
      return (
        <div
          data-testid="error-boundary-fallback"
          className="flex h-screen items-center justify-center p-6 text-center"
        >
          <p className="text-gray-600">문제가 발생했습니다. 페이지를 새로고침해주세요.</p>
        </div>
      );
    }
    return this.props.children;
  }
}
