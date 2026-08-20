import { create } from "zustand";
import { apiClient, setAccessToken, setRefreshHandlers } from "@/lib/apiClient";
import type { TokenPair, User } from "@/types/domain";

const ACCESS_TOKEN_KEY = "quickchat.accessToken";
const REFRESH_TOKEN_KEY = "quickchat.refreshToken";

export interface AuthTokenState {
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
}

export type RefreshOutcome = { ok: true; tokens: TokenPair } | { ok: false };

/**
 * 순수 함수로 분리한 토큰 갱신 상태 전이 - PBT-01 속성 #5(Invariant) 테스트 대상.
 * 성공 시 항상 유효한 토큰으로 교체하고, 실패 시에는 기존 토큰을 그대로 유지한다
 * (명시적 로그아웃 전까지 지우지 않음).
 */
export function nextAuthState(current: AuthTokenState, outcome: RefreshOutcome): AuthTokenState {
  if (!outcome.ok) {
    return { ...current, isAuthenticated: current.accessToken !== null };
  }
  return {
    accessToken: outcome.tokens.accessToken,
    refreshToken: outcome.tokens.refreshToken,
    isAuthenticated: true,
  };
}

interface AuthState extends AuthTokenState {
  user: User | null;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<User>;
  logout: () => void;
  restoreSession: () => void;
  loadCurrentUser: () => Promise<void>;
}

function persistTokens(tokens: TokenPair): void {
  window.localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken);
  window.localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
}

function clearPersistedTokens(): void {
  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
  window.localStorage.removeItem(REFRESH_TOKEN_KEY);
}

export const useAuthStore = create<AuthState>((set, get) => {
  // apiClient의 401 인터셉터가 재발급이 필요할 때 호출할 핸들러를 등록한다 (business-rules.md).
  setRefreshHandlers(
    async () => {
      const { refreshToken } = get();
      if (!refreshToken) {
        throw new Error("No refresh token available");
      }
      try {
        const { data } = await apiClient.post<TokenPair>("/api/auth/refresh", { refreshToken });
        const next = nextAuthState(get(), { ok: true, tokens: data });
        persistTokens(data);
        setAccessToken(next.accessToken);
        set(next);
        return data.accessToken;
      } catch (err) {
        const next = nextAuthState(get(), { ok: false });
        set(next);
        throw err;
      }
    },
    () => {
      clearPersistedTokens();
      setAccessToken(null);
      set({ user: null, accessToken: null, refreshToken: null, isAuthenticated: false });
    }
  );

  return {
    user: null,
    accessToken: null,
    refreshToken: null,
    isAuthenticated: false,

    login: async (email, password) => {
      const { data } = await apiClient.post<TokenPair>("/api/auth/login", { email, password });
      persistTokens(data);
      setAccessToken(data.accessToken);
      set({ accessToken: data.accessToken, refreshToken: data.refreshToken, isAuthenticated: true });
    },

    register: async (email, password, displayName) => {
      const { data } = await apiClient.post<User>("/api/auth/register", {
        email,
        password,
        displayName,
      });
      return data;
    },

    logout: () => {
      clearPersistedTokens();
      setAccessToken(null);
      set({ user: null, accessToken: null, refreshToken: null, isAuthenticated: false });
    },

    restoreSession: () => {
      if (typeof window === "undefined") return;
      const accessToken = window.localStorage.getItem(ACCESS_TOKEN_KEY);
      const refreshToken = window.localStorage.getItem(REFRESH_TOKEN_KEY);
      if (accessToken && refreshToken) {
        setAccessToken(accessToken);
        set({ accessToken, refreshToken, isAuthenticated: true });
      }
    },

    // Frontend Code Generation 중 발견된 누락 보완(Backend Post-Approval Patch 4): 로그인/새로고침 후
    // "나는 누구인가"를 알아낼 API가 없어 user가 항상 null로 남는 문제. GET /api/users/me 신설에 대응.
    loadCurrentUser: async () => {
      const { data } = await apiClient.get<User>("/api/users/me");
      set({ user: data });
    },
  };
});
