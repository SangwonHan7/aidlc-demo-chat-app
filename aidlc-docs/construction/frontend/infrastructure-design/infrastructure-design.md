# Infrastructure Design - Frontend Unit

## 실행 환경
- Backend와 동일한 k3s 단일 노드 클러스터 사용 (Synology DS925+, VMM Ubuntu VM, 20GB RAM 중 14~16GB 할당 - 상세는 `aidlc-docs/construction/backend/infrastructure-design/infrastructure-design.md` 참고)
- `quickchat-app` 네임스페이스에 Backend와 함께 배치

## 컴퓨트 (Question 1 답변 A)
- Next.js Node 서버 컨테이너(`next start`)로 서빙한다 - 정적 export(`output: 'export'`)는 사용하지 않는다. NFR Requirements에서 결정한 `/api/health` Route Handler가 정상 동작해야 하기 때문
- 고정 1레플리카 (NFR Design "고정 레플리카" 패턴 - Backend와 동일하게 오토스케일링 미적용)
- 리소스: request 256Mi/0.25 vCPU, limit 512Mi/0.5 vCPU (Backend의 512Mi/1Gi보다 가벼움 - 정적 자산 서빙 + 얇은 Node 서버 역할만 수행하기 때문)

## 네트워킹 (Question 2 답변 B: 같은 호스트, 다른 포트)
- 호스트는 `quickchat.local` 하나만 사용하고, Frontend와 Backend를 포트로 구분한다 (포트가 다르면 origin도 달라지므로 CORS 관점에서는 서브도메인 분리와 동등)
- 구현: 두 서비스를 각각 `NodePort` 타입으로 노출한다 - k3s 단일 노드 환경에서 가장 단순한 방식
  - Frontend Service: NodePort **30080** → containerPort 3000
  - Backend Service: NodePort **30081** → containerPort 8080
  - (참고: k8s NodePort는 기본적으로 30000~32767 범위만 허용되어, 컨테이너 내부 포트(3000/8080)와 외부 노출 포트(30080/30081)가 다르다 - Question 2의 예시 포트는 컨테이너 내부 포트로 그대로 유지하고, 외부 노출 포트만 NodePort 범위에 맞게 조정했다)
- nginx-ingress는 계속 존재하지만(다른 경로/향후 확장을 위해 유지), 이번 Frontend/Backend 두 origin의 라우팅에는 사용하지 않는다 - Backend `deployment-architecture.md`가 원래 가정했던 "경로 기반 단일 호스트" 라우팅은 이번 결정으로 대체된다 (하단 "Backend 문서 정정" 참고)

### 알려진 제약 (투명성 목적)
Backend Infrastructure Design(Q4 답변 A)에서 "TLS는 nginx-ingress 단에서 종료"로 결정했으나, NodePort로 직접 노출하는 이 두 포트는 nginx-ingress를 거치지 않아 TLS 종료 지점이 없다. 이번 1회차 멘토링 데모(내부 LAN, Synology NAS 단일 노드)에서는 평문 HTTP로 단순화하고 TLS는 범위 밖으로 명시한다. 실서비스로 확장할 때는 nginx-ingress의 TCP passthrough(`tcp-services` ConfigMap) 또는 각 서비스 자체 TLS 종료를 재검토해야 한다.

## 스토리지 / 메시징
해당 없음 - Frontend는 상태 없는(stateless) 서버로, 자체 DB/PVC가 불필요하고 Kafka를 직접 발행/구독하지 않는다 (`frontend-infrastructure-design-plan.md`의 "이미 확정된 사항" 참고)

## 모니터링
- Prometheus/Grafana는 계속 공유하지만 Frontend는 커스텀 메트릭을 노출하지 않는다 (NFR Requirements Question 4 답변 A)
- `/api/health`를 k8s `livenessProbe`/`readinessProbe` 대상으로 등록한다 (HTTP GET, 초기 지연 5s, 주기 10s)

## Backend 문서 정정
Backend의 `deployment-architecture.md`(경로 기반 단일 호스트 가정)와 `shared-infrastructure.md`(같은 가정 반복)를 이번 결정(같은 호스트, 포트로 구분, NodePort 직접 노출)에 맞게 갱신했다. Backend 애플리케이션 코드/리소스 사이징 자체는 변경하지 않았고, 네트워킹 계층 설명만 정정했다.
