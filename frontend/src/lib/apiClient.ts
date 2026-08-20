import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";

// NFR Design(Frontend) Question 4 답변 B: Backend는 다른 origin에 배포된다.
// Infrastructure Design에서 결정한 NodePort 30081(컨테이너 내부 8080)을 가리킨다.
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export const apiClient = axios.create({ baseURL: API_BASE_URL });

let currentAccessToken: string | null = null;
let refreshInFlight: Promise<string> | null = null;
let performRefresh: (() => Promise<string>) | null = null;
let onRefreshFailed: (() => void) | null = null;

/** authStore가 로그인/복원/로그아웃 시 호출해 현재 Access Token을 갱신한다. */
export function setAccessToken(token: string | null): void {
  currentAccessToken = token;
}

/**
 * authStore가 초기화 시 1회 등록한다.
 * - refresh: 실제 /api/auth/refresh 호출 + 스토어 갱신을 수행하고 새 accessToken을 반환
 * - onFailure: refresh 자체가 실패했을 때(만료/무효) 호출 - 로그아웃 처리
 */
export function setRefreshHandlers(refresh: () => Promise<string>, onFailure: () => void): void {
  performRefresh = refresh;
  onRefreshFailed = onFailure;
}

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (currentAccessToken) {
    config.headers.set("Authorization", `Bearer ${currentAccessToken}`);
  }
  return config;
});

type RetryableConfig = InternalAxiosRequestConfig & { _retried?: boolean };

// business-rules.md 인증/토큰: 401 수신 시 refresh 1회 자동 호출 - 동시 요청은 in-flight promise를 공유해
// 중복 재발급을 방지한다. NFR Design Question 1 답변 B: 그 외 실패에 대한 자동 재시도는 하지 않는다.
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryableConfig | undefined;
    const isAuthEndpoint = originalRequest?.url?.includes("/api/auth/");

    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retried &&
      !isAuthEndpoint &&
      performRefresh
    ) {
      originalRequest._retried = true;
      try {
        if (!refreshInFlight) {
          refreshInFlight = performRefresh().finally(() => {
            refreshInFlight = null;
          });
        }
        const newToken = await refreshInFlight;
        originalRequest.headers.set("Authorization", `Bearer ${newToken}`);
        return apiClient.request(originalRequest);
      } catch (refreshError) {
        onRefreshFailed?.();
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);
