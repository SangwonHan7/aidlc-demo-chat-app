# Code Generation Plan - Frontend Unit

## Unit Context
- Stories: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2 (stories.md) - 전체 스토리가 Frontend 화면 필요
- Dependencies: Backend REST API + WebSocket(STOMP) 계약 (backend/README.md, api-layer-summary.md).실제 기동/통합 검증은 Build and Test 단계에서 수행 - 이번 단계는 코드/테스트 작성까지
- 인터페이스 소비: `POST/GET /api/auth/*`, `/api/channels/*`, `/api/presence`, WebSocket `/ws`(STOMP) - 전부 이미 승인된 Backend 산출물
- 코드 위치: `frontend/` (workspace root 기준, unit-of-work.md 모노레포 구조)
- 기술 스택: Next.js 14(App Router, 전체 클라이언트 컴포넌트), TypeScript 5, Zustand, Tailwind CSS, axios, `@stomp/stompjs`, react-window, Vitest + fast-check(PBT)

## 카테고리 적용 여부 (code-generation.md 표준 카테고리 대비)
- Repository Layer: 해당 없음 - Frontend는 DB에 직접 접근하지 않음 (Infrastructure Design/NFR Requirements에서 이미 확정)
- Database Migration Scripts: 해당 없음 - Frontend가 소유한 데이터 모델 없음
- API Layer: Backend처럼 API를 "제공"하지 않으므로 별도 카테고리 없이, API를 "호출"하는 코드(axios 클라이언트, STOMP 클라이언트)는 Business Logic Generation에 포함
- Frontend Components: 표준 카테고리를 그대로 적용 (화면/컴포넌트)

## Plan

### Step 1: Project Structure Setup (Greenfield)
- [x] `frontend/package.json`, `tsconfig.json`, `next.config.js`, `tailwind.config.ts`, `postcss.config.js`, `vitest.config.ts` - Next.js 14, TypeScript 5, 의존성(react, react-dom, zustand, axios, @stomp/stompjs, react-window, tailwindcss, vitest, @testing-library/react, @testing-library/jest-dom, jsdom, fast-check)
- [x] `frontend/Dockerfile` (멀티스테이지 빌드, `next start`로 서빙 - Infrastructure Design Question 1 답변 A)
- [x] `frontend/src/app/layout.tsx`, `globals.css` (Tailwind 기본 레이아웃)

### Step 2: Business Logic Generation (domain-entities.md, business-rules.md, business-logic-model.md)
- [x] `src/types/domain.ts` - domain-entities.md의 TypeScript 타입 전체
- [x] `src/store/authStore.ts`, `src/store/chatStore.ts`, `src/store/presenceStore.ts` - Zustand 스토어 3종
- [x] `src/lib/apiClient.ts` - axios 인스턴스 + 401 인터셉터(자동 refresh, in-flight 캐싱)
- [x] `src/lib/stompClient.ts` - STOMP 싱글턴 연결 모듈(연결/구독 전환/지수 백오프 재연결)
- [x] `src/lib/backoff.ts` - 지수 백오프 계산 순수 함수
- [x] `src/lib/validation.ts` - 이메일/비밀번호/displayName 클라이언트 검증 함수(Backend 규칙과 동일 조건식)
- [x] `src/lib/errorMessages.ts` - errorCode -> 사용자 문구 매핑
- Related Stories: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2

### Step 3: Business Logic Unit Testing (PBT Full Enforcement)
- [x] Vitest 예시 기반 테스트 (각 스토어/lib 함수 핵심 시나리오)
- [x] fast-check 속성 기반 테스트 - business-logic-model.md PBT-01의 7개 속성 전체(메시지 병합 Idempotence, 이력 prepend Invariant, cursor 병합 Round-trip, presence 병합 Idempotence, 토큰 갱신 Invariant, 클라이언트 검증 Oracle, 백오프 Invariant)
- [x] PBT-10: 각 속성 테스트와 짝이 되는 example-based 테스트 최소 1개씩 확인

### Step 4: Business Logic Summary
- [x] `aidlc-docs/construction/frontend/code/business-logic-summary.md` 작성

### Step 5: Frontend Components Generation (frontend-components.md)
- [x] `src/app/page.tsx`(AppShell), `src/app/login/page.tsx`, `src/app/register/page.tsx`, `src/app/api/health/route.ts`
- [x] 레이아웃: `AppShellLayout`, `Sidebar`, `MainPanel`, `EmptyState`
- [x] 인증: `LoginForm`, `RegisterForm`
- [x] 대화: `ConversationView`, `ConversationHeader`, `MessageList`(react-window 가상화), `MessageInput`
- [x] 채널 관리: `ChannelList`, `DirectMessageList`, `CreateChannelModal`, `MemberManagementPanel`
- [x] 상태 표시: `PresenceIndicator`
- [x] 공통: `ErrorBoundary` (최상위 렌더링 오류 방어, NFR Requirements Question 4) - 최종적으로 `app/layout.tsx`에 연결
- [x] 모든 상호작용 요소에 `data-testid="{component}-{element-role}"` 부여 (자동화 친화 규칙)
- Related Stories: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2
- Post-Approval Patch 4 (Backend): `GET /api/users/me` 신설 - `authStore.user`를 채울 방법이 없던 누락 보완 (상세: `aidlc-docs/construction/backend/code/api-layer-summary.md`, `backend/README.md`)

### Step 6: Frontend Components Unit Testing
- [x] React Testing Library 기반 컴포넌트 테스트 (LoginForm/RegisterForm 검증 메시지, MessageInput 빈 내용 비활성화, PresenceIndicator 상태별 렌더링, CreateChannelModal 글자수 카운터)
- [x] fast-check: 폼 검증 컴포넌트에 대한 입력 속성 테스트(임의 문자열에 대해 validation.ts와 동일한 판정을 내리는지)

### Step 7: Frontend Components Summary
- [x] `aidlc-docs/construction/frontend/code/frontend-components-summary.md` 작성

### Step 8: Repository Layer - 해당 없음
- [x] Frontend는 DB/Repository에 직접 접근하지 않아 생성 대상 없음 (근거: Infrastructure Design/NFR Requirements)

### Step 9: Database Migration Scripts - 해당 없음
- [x] Frontend가 소유한 데이터 모델이 없어 생성 대상 없음

### Step 10: Documentation Generation
- [x] `frontend/README.md` (실행 방법, 환경변수, 화면 개요) - Docker 빌드 시 `NEXT_PUBLIC_*`를 build-arg로 넘겨야 한다는 주의사항 포함

### Step 11: Deployment Artifacts Generation
- [x] `frontend/Dockerfile` (Step 1에서 생성) 확인/보완 - 확인 중 발견: 전체 컴포넌트가 "use client"라 `NEXT_PUBLIC_API_BASE_URL`/`NEXT_PUBLIC_WS_URL`이 `next build` 시점에 정적 인라인되는데, 기존 Dockerfile은 이를 받을 `ARG`가 없어 컨테이너 실행 시 환경변수를 주입해도 무시되는 상태였음 - `ARG`+`ENV`를 `npm run build` 이전에 추가해 보완 (아래 "Frontend 자체 정정" 참고)
- [x] `infra/docker-compose/docker-compose.yml`에 frontend 서비스 항목 추가 (Infra 유닛 소유 파일이지만 Backend와 동일하게 최소 반영) - build-arg로 `NEXT_PUBLIC_API_BASE_URL`/`NEXT_PUBLIC_WS_URL` 명시, backend 서비스에도 `CORS_ALLOWED_ORIGIN` 명시
- [x] 참고: `infra/k3s/app/frontend-deployment.yaml` 등 전체 K8s 매니페스트는 unit-of-work.md의 개발 순서에 따라 Infra 마무리 단계에서 생성 (지금은 범위 아님) - Backend Code Generation과 동일한 패턴. 단, k3s 배포 시에는 이미지가 배포 환경(호스트/포트)마다 build-arg를 다시 넣어 재빌드되어야 한다는 제약을 `deployment-architecture.md`에 기록해 Infra 마무리 단계에 인계

### Frontend 자체 정정 (Backend 패치와는 별개, 투명성 목적)
Step 11에서 Dockerfile을 다시 확인하던 중, 이미 승인된 Frontend Infrastructure Design의 "환경변수 주입" 절이 `NEXT_PUBLIC_API_BASE_URL`/`NEXT_PUBLIC_WS_URL`을 일반 컨테이너 런타임 환경변수처럼 서술하고 있었으나, 이 앱은 전체가 "use client" 컴포넌트라 해당 값들이 `next build` 시점에 클라이언트 JS로 정적 인라인된다는 것을 발견했다(Next.js의 프레임워크 제약, 설계 선택의 문제가 아님). 즉 실제로는 Pod 실행 시점에 값을 바꿔도 반영되지 않는 잘못된 설명이었다. `frontend/Dockerfile`에 `ARG`/`ENV`를 추가하고, `deployment-architecture.md`/`frontend/README.md`를 build-arg 방식으로 정정했다 - Backend 문서의 NodePort 라우팅 정정과 같은 성격(경쟁하는 설계 대안이 없는 기술적 사실 정정)이라 판단해 질문 없이 바로 수정했다.

## 실행 방식
Part 2(Generation)에서 위 단계를 순서대로 실행하며, 각 단계 완료 시 체크박스를 [x]로 표시하고 aidlc-state.md를 갱신합니다. 테스트 파일은 Backend(별도 src/test 트리)와 달리 JS/TS 생태계 관례에 따라 소스 파일 옆에 `*.test.ts(x)`로 배치합니다 (Vitest 표준 관례).
