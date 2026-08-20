import { NextResponse } from "next/server";

// Infrastructure Design(Frontend): k8s liveness/readiness probe 대상. 모니터링 도구는 아니다
// (NFR Requirements Question 4 답변 A - 별도 모니터링 도구 미도입).
export function GET() {
  return NextResponse.json({ status: "ok" });
}
