# Infrastructure Design Plan - Frontend Unit

## 이미 확정된 사항 (Backend Infrastructure Design / shared-infrastructure.md에서 상속)

- k3s 단일 노드 클러스터 (Synology DS925+, VMM Ubuntu VM, 20GB RAM 중 14~16GB 할당) - Frontend도 동일 클러스터 사용
- `quickchat-app` 네임스페이스에 Backend와 함께 배치 (Backend infrastructure-design.md Q6 답변 B)
- nginx-ingress + TLS 종료 - 두 번째 호스트(Frontend)에도 동일하게 적용
- Prometheus/Grafana는 공유하되, Frontend는 NFR Requirements 결정(Q4=A, 별도 모니터링 도구 미도입)에 따라 커스텀 메트릭을 노출하지 않음 - `/api/health`만 liveness/readiness 대상으로 등록
- Storage Infrastructure: 해당 없음 - Frontend는 상태 없는(stateless) 서버이며 자체 DB/PVC가 불필요
- Messaging Infrastructure: 해당 없음 - Kafka는 Backend를 통해서만 간접적으로 연관되고, Frontend가 직접 발행/구독하지 않음
- 레플리카 수: NFR Design에서 결정한 "고정 레플리카"(오토스케일링 미적용) 패턴을 그대로 적용, Backend와 동일하게 1레플리카로 시작 - 별도 질문 없이 진행(리소스 여유가 충분하고 언제든 조정 가능한 값이므로)

## 정정이 필요한 기존 문서

Backend Infrastructure Design(`deployment-architecture.md`)은 "Frontend: Ingress 경로 `/`", "Backend: Ingress 경로 `/api`, `/ws`"로 **경로 기반 단일 호스트(같은 origin)** 라우팅을 가정하고 작성되었습니다. 그런데 Frontend NFR Design(Question 4=B)에서 이미 **다른 origin + 명시적 CORS**로 결정했습니다. 두 문서가 서로 맞지 않으므로, 아래 Question 2 답변을 반영해 Backend 쪽 `deployment-architecture.md`/`shared-infrastructure.md`도 host 기반 라우팅으로 함께 정정합니다.

## Plan

- [x] 위 "이미 확정된 사항"을 Frontend 인프라 문서에 반영
- [x] 아래 질문 답변 반영
- [x] infrastructure-design.md 작성
- [x] deployment-architecture.md 작성
- [x] shared-infrastructure.md 갱신 (host+포트 기반 라우팅으로 정정)
- [x] Backend의 infrastructure-design.md/deployment-architecture.md도 host+포트 기반 라우팅으로 일치시켜 정정

## Questions

각 질문에 알파벳으로 답한 뒤 채팅으로 "답변 완료"라고 알려주세요.

## Question 1 (Compute Infrastructure / Deployment Environment)

Next.js를 컨테이너에서 어떻게 서빙할까요? (NFR Requirements에서 App Router 전체 클라이언트 컴포넌트로 결정했고, `/api/health` Route Handler를 두기로 했습니다)

A) Next.js Node 서버 컨테이너(`next start`) - Route Handler(`/api/health`)가 정상 동작. Node 런타임 리소스가 약간 필요(추정 request 256Mi/limit 512Mi 수준)

B) 정적 export(`output: 'export'`) + nginx로 직접 서빙 - 리소스 사용은 더 가볍지만 Route Handler를 지원하지 않아, `/api/health`를 Next.js 밖(예: nginx의 정적 200 응답)으로 옮겨야 함

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 2 (Networking Infrastructure)

Frontend와 Backend를 다른 origin(다른 host)으로 배포하기로 했습니다(NFR Design Q4=B). 이번 데모/실습 환경에서 실제 호스트를 어떻게 구분할까요?

A) 서브도메인 분리 - 예: `chat.quickchat.local`(Frontend), `api.quickchat.local`(Backend). NAS/개발 머신의 hosts 파일 또는 내부 DNS에 등록해 사용, nginx-ingress의 host 기반 라우팅으로 구분

B) 같은 호스트, 다른 포트 - 예: `quickchat.local:3000`(Frontend), `quickchat.local:8080`(Backend). 포트 자체가 origin을 구분하므로 CORS 관점에서는 A와 동등하게 "다른 origin"이 됨

C) Other (please describe after [Answer]: tag below)

[Answer]: B
