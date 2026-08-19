# Logical Components - Frontend Unit

## 배포 토폴로지 (Question 4 답변 B: 다른 origin)

| 구성요소 | Origin/도메인(예시) | 비고 |
|---|---|---|
| Frontend (Next.js) | 예: `https://chat.quickchat.local` | nginx-ingress로 노출 |
| Backend REST/WebSocket | 예: `https://api.quickchat.local` | 동일 ingress의 다른 host - Backend 코드는 CORS 설정으로만 대응, 그 외 변경 없음 |

실제 도메인/호스트명은 Infrastructure Design 단계에서 확정한다 (Backend가 Infrastructure Design에서 Synology DS925+ 실사양을 확인한 뒤 결정한 것과 동일한 순서).

## CORS 설정

| 항목 | 값 |
|---|---|
| 허용 origin | `quickchat.cors.allowed-origin` (환경변수 `CORS_ALLOWED_ORIGIN`), 로컬 개발 기본값 `http://localhost:3000` |
| 허용 메서드 | GET, POST, PUT, DELETE, OPTIONS |
| 허용 헤더 | Authorization, Content-Type |
| Credentials(쿠키) | 미허용 - Bearer 토큰만 사용 |
| 적용 범위 | Backend 전체 경로(`/**`) |

## 정적 자산 캐싱 (Question 5 답변 A)
- Next.js 기본 캐싱 헤더(빌드 해시 기반 파일명 + 기본 immutable 캐싱) 그대로 사용
- nginx-ingress에 별도 `Cache-Control` 재정의 없음 - 온프레미스 NAS 환경에서 별도 CDN 계층이 없으므로 단순화

## 메시지 목록 가상화 컴포넌트

| 항목 | 값 |
|---|---|
| 라이브러리 | react-window |
| 대상 컴포넌트 | `frontend-components.md`의 `MessageList` |
| 연동 방식 | `useChatStore.messagesByChannel[activeChannelId]` 배열을 가상화 리스트에 전달, 최상단 도달 시 이전 페이지 로드를 트리거하는 방식은 Functional Design의 무한 스크롤 흐름과 동일하게 유지 |

## 헬스체크 (NFR Requirements에서 이어짐)
- `/api/health` (Next.js Route Handler), 200 고정 응답 - k8s liveness/readiness probe 대상. probe 주기/실패 임계치는 Infrastructure Design 단계에서 확정
