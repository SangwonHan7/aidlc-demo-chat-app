// business-rules.md WebSocket 재연결 정책: 1s, 2s, 4s, 8s, ... 최대 30s.
const BASE_DELAY_MS = 1000;
const MAX_DELAY_MS = 30000;

/**
 * n번째(1-based) 재시도 대기시간(ms)을 계산한다.
 * PBT-01 속성 #7(Invariant): 대기시간은 항상 이전 시도보다 크거나 같고, 상한(30s)을 넘지 않는다.
 */
export function nextBackoffDelayMs(attempt: number): number {
  if (attempt < 1 || !Number.isInteger(attempt)) {
    throw new Error("attempt must be a positive integer");
  }
  const delay = BASE_DELAY_MS * 2 ** (attempt - 1);
  return Math.min(delay, MAX_DELAY_MS);
}
