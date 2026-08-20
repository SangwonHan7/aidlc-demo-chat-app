import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";
import { nextBackoffDelayMs } from "./backoff";

// business-rules.md WebSocket 연결 정책 (Question 4 답변 A): 앱 전역 연결 1개를 유지하고
// 채널/DM 전환 시에는 구독만 교체한다. Backend WebSocketConfig의 `/ws`, `/app`, `/topic` 계약과 일치.
const WS_BASE_URL = process.env.NEXT_PUBLIC_WS_URL ?? "ws://localhost:8080/ws";

let client: Client | null = null;
let currentSubscription: StompSubscription | null = null;
let reconnectAttempt = 0;
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
let onReconnected: (() => void) | null = null;

function scheduleReconnect(accessToken: string) {
  if (reconnectTimer) return;
  reconnectAttempt += 1;
  const delay = nextBackoffDelayMs(reconnectAttempt);
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null;
    connect(accessToken);
  }, delay);
}

/** 로그인 성공 시 1회 호출. 이미 연결되어 있으면 아무 것도 하지 않는다. */
export function connect(accessToken: string): void {
  if (client?.active) return;

  client = new Client({
    brokerURL: WS_BASE_URL,
    connectHeaders: { Authorization: `Bearer ${accessToken}` },
    reconnectDelay: 0, // 자체 지수 백오프(backoff.ts)를 사용하므로 STOMP 기본 재연결은 끈다
    onConnect: () => {
      reconnectAttempt = 0;
      onReconnected?.();
    },
    onWebSocketClose: () => {
      scheduleReconnect(accessToken);
    },
    onStompError: () => {
      scheduleReconnect(accessToken);
    },
  });
  client.activate();
}

export function disconnect(): void {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
  reconnectAttempt = 0;
  currentSubscription?.unsubscribe();
  currentSubscription = null;
  void client?.deactivate();
  client = null;
}

/** 재연결 성공 시(활성 채널 이력 재조회 트리거용) 호출될 콜백을 등록한다. */
export function setOnReconnected(callback: (() => void) | null): void {
  onReconnected = callback;
}

/** 채널/DM 전환 시 이전 구독을 해지하고 새 채널을 구독한다 (연결 자체는 재사용). */
export function subscribeToChannel(channelId: string, onMessage: (payload: unknown) => void): void {
  currentSubscription?.unsubscribe();
  currentSubscription = client?.subscribe(`/topic/channel/${channelId}`, (message: IMessage) => {
    onMessage(JSON.parse(message.body));
  }) ?? null;
}

export function sendMessage(channelId: string, content: string): void {
  client?.publish({
    destination: `/app/chat.send/${channelId}`,
    body: JSON.stringify({ content }),
  });
}

export function isConnected(): boolean {
  return client?.active ?? false;
}
