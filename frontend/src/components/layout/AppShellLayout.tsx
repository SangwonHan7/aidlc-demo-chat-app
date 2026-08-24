"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/authStore";
import { useChatStore } from "@/store/chatStore";
import { connect, disconnect, setOnReconnected } from "@/lib/stompClient";
import { Sidebar } from "@/components/channel/Sidebar";
import { MainPanel } from "./MainPanel";

/**
 * `/` 진입점의 부트스트랩을 전담한다: 세션 복원 -> 내 프로필 조회(Backend Post-Approval Patch 4,
 * GET /api/users/me) -> WebSocket 연결 -> 채널 목록 로드. 미인증 상태로 판명되면 /login으로 리다이렉트.
 * frontend-components.md: `/` (인증 필요) -> AppShellLayout(Sidebar + MainPanel 상시 공존).
 */
export function AppShellLayout() {
  const router = useRouter();
  const restoreSession = useAuthStore((s) => s.restoreSession);
  const loadCurrentUser = useAuthStore((s) => s.loadCurrentUser);
  const logout = useAuthStore((s) => s.logout);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const user = useAuthStore((s) => s.user);
  const accessToken = useAuthStore((s) => s.accessToken);
  const loadChannels = useChatStore((s) => s.loadChannels);
  const [hydrated, setHydrated] = useState(false);

  // 1) 새로고침 시 localStorage에 저장된 토큰을 스토어로 복원.
  useEffect(() => {
    restoreSession();
    setHydrated(true);
  }, [restoreSession]);

  // 2) 복원 결과 미인증이면 로그인 화면으로.
  useEffect(() => {
    if (hydrated && !isAuthenticated) {
      router.replace("/login");
    }
  }, [hydrated, isAuthenticated, router]);

  // 3) 인증되어 있으나 프로필이 없으면(새로고침 직후) 내 프로필을 조회. 실패 시(토큰 만료 등) 로그아웃 처리.
  useEffect(() => {
    if (isAuthenticated && !user) {
      void loadCurrentUser().catch(() => logout());
    }
  }, [isAuthenticated, user, loadCurrentUser, logout]);

  // 4) 프로필까지 준비되면 WS 연결 + 채널 목록 로드. 재연결 시에는 REST 재조회로 갭을 보완(Question 5 답변 A).
  // 채널 목록을 다 불러온 뒤 restoreActiveChannel()로 새로고침 전에 보던 채널을 복원한다(2026-08-21
  // 발견 - 새로고침하면 activeChannelId가 초기화되어 대화가 사라지던 문제, chatStore.ts 주석 참고).
  // 복원되면 ConversationView의 기존 effect가 loadHistory()를 호출해 실제 메시지 이력을 다시 불러온다.
  useEffect(() => {
    if (!isAuthenticated || !user || !accessToken) return;
    connect(accessToken);
    void loadChannels().then(() => useChatStore.getState().restoreActiveChannel());
    setOnReconnected(() => {
      void loadChannels();
      const activeChannelId = useChatStore.getState().activeChannelId;
      if (activeChannelId) void useChatStore.getState().loadHistory(activeChannelId);
    });
    return () => setOnReconnected(null);
  }, [isAuthenticated, user, accessToken, loadChannels]);

  // 언마운트(로그아웃으로 인한 라우트 이탈 등) 시 안전망으로 연결 해제.
  useEffect(() => {
    return () => disconnect();
  }, []);

  if (!hydrated || !isAuthenticated || !user) {
    return null;
  }

  return (
    <div className="flex h-screen w-screen overflow-hidden" data-testid="app-shell-layout">
      <Sidebar />
      <MainPanel />
    </div>
  );
}
