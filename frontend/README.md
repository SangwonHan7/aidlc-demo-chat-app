# QuickChat Frontend

Next.js 14(App Router, 전체 클라이언트 컴포넌트) 기반 프론트엔드. 상세 설계는 `../aidlc-docs/construction/frontend/`를 참고하세요.

## 실행 방법 (로컬 개발)

1. Backend가 먼저 떠 있어야 합니다 (`../backend/README.md`).
2. `npm install`
3. `.env.local`에 아래 환경변수 설정 (기본값은 Backend가 `localhost:8080`에 떠 있는 경우와 동일해서 생략 가능)
4. `npm run dev` (http://localhost:3000)

## 실행 방법 (Docker)

```
docker build --build-arg NEXT_PUBLIC_API_BASE_URL=http://localhost:8080 \
             --build-arg NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws \
             -t quickchat-frontend .
docker run -p 3000:3000 quickchat-frontend
```

**주의**: `NEXT_PUBLIC_API_BASE_URL`/`NEXT_PUBLIC_WS_URL`은 이 앱의 모든 컴포넌트가 `"use client"`이기 때문에 `next build` 시점에 클라이언트 JS로 정적 인라인됩니다. 컨테이너를 `docker run -e ...`로 실행하거나 k8s Pod에 환경변수로 주입해도 이미 빌드된 결과물에는 반영되지 않습니다 - 반드시 이미지 빌드 시점에 `--build-arg`로 넘겨야 합니다 (`Dockerfile` 참고). 배포 환경(호스트/포트)이 바뀌면 이미지를 다시 빌드해야 한다는 뜻이며, 상세 배경은 `../aidlc-docs/construction/frontend/infrastructure-design/deployment-architecture.md`의 "환경변수 주입" 절 참고.

## 환경변수

| 변수 | 설명 | 기본값 |
|---|---|---|
| NEXT_PUBLIC_API_BASE_URL | Backend REST API base URL (브라우저가 직접 호출) | http://localhost:8080 |
| NEXT_PUBLIC_WS_URL | Backend STOMP WebSocket 엔드포인트 | ws://localhost:8080/ws |

## 화면 개요

- `/login`, `/register` - 인증 화면 (앱쉘 없는 독립 페이지)
- `/` - 인증 필요. `AppShellLayout`(Sidebar + MainPanel) - 미인증 시 `/login`으로 리다이렉트
  - Sidebar: 참여 중인 채널 + 참여 가능한 공개 채널(story 1.3), DM 목록(story 1.2), 새 채널 만들기
  - MainPanel: 선택된 채널/DM의 대화 화면(무한 스크롤 이력, 실시간 수신, 멤버 관리) 또는 빈 상태 안내
- `/api/health` - k8s liveness/readiness probe용 Route Handler (`{ "status": "ok" }`)

## 상태 관리 / 통신

- Zustand 스토어 3종(`authStore`, `chatStore`, `presenceStore`) - `src/store/`
- REST: `src/lib/apiClient.ts` (axios, 401 시 자동 refresh)
- WebSocket(STOMP): `src/lib/stompClient.ts` (앱 전역 연결 1개 유지, 재연결은 자체 지수 백오프)
- 로그아웃 시 `chatStore.reset()`/`presenceStore.reset()`을 호출해 스토어에 남은 이전 사용자 데이터를 비웁니다.

## 테스트

- `npm test` - Vitest (예시 기반 + fast-check 속성 기반 테스트 함께 실행)
- 대상과 근거: `../aidlc-docs/construction/frontend/code/business-logic-summary.md`, `frontend-components-summary.md`
- 알려진 제약: 이 리포지토리 작업 세션에서는 샌드박스 제약으로 `npm test`를 실제로 실행해 통과를 확인하지 못했습니다. Build and Test 단계에서 최초 실행이 필요합니다.
