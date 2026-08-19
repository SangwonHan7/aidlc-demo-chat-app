# Infrastructure Design - Backend Unit

## 실행 환경
- Synology DS925+ 1대 (AMD Ryzen V1500B, 4코어/8스레드 2.2GHz, 현재 20GB DDR4 ECC 설치됨 - 기본 4GB + 16GB 모듈 추가 구성으로 추정, 최대 32GB까지 확장 가능, Q2 답변에서 확인된 실제 모델)
- Virtual Machine Manager(VMM)로 Ubuntu 22.04 VM을 만들고 그 안에 k3s 설치 (DSM에 k3s를 직접 설치하는 것은 커널/의존성 제약으로 비권장 - VM 내부 설치가 커뮤니티 표준 방식). Synology CSI 드라이버로 NAS 스토리지를 K8s PV로 사용 가능
- k3s 단일 노드 클러스터 (Q1 답변 A)

## NAS 리소스 임계값 및 Docker Compose 축소 기준 (requirements.md Open Item 해결)

| NAS RAM 상태 | 권장 운영 방식 |
|---|---|
| 기본 4GB (미업그레이드) | k3s 비권장 - Docker Compose로 각 서비스 직접 실행 (Prometheus/Grafana 제외, 전 서비스 1개씩) |
| 16GB 이상 | 최소 권장선 - k3s 단일 노드 운영 가능 |
| 20GB (현재 설치됨) | 권장 구성 - VMM에 VM 14~16GB RAM/3~4 vCPU 할당, DSM/VMM 자체에는 4~6GB 남김 |
| 32GB (최대 지원) | 여유 있는 운영 |

현재 DS925+에는 20GB가 설치되어 있어 최소 권장선(16GB)을 넉넉히 넘습니다. Docker Compose 축소 운영 없이 바로 k3s로 진행 가능하며, VMM VM에 14~16GB/3~4 vCPU를 할당하는 구성을 권장합니다. (참고: 만약 향후 다른 NAS로 교체되거나 RAM이 16GB 미만으로 내려가는 경우를 대비해 Docker Compose 축소 경로도 계속 문서화해 둡니다.)

## 컴퓨트 및 네임스페이스
- k3s 단일 노드 (VMM Ubuntu 22.04 VM 내부)
- quickchat-app: Backend, Frontend / quickchat-data: PostgreSQL, Kafka, Redis, Vault (Q6 답변 B)

## 스토리지
- PostgreSQL (Q3 답변 A), quickchat-data 네임스페이스, PVC는 Synology CSI로 NAS 볼륨에 매핑
- 리소스: request 256Mi/0.25 vCPU, limit 512Mi/0.5 vCPU

## 메시징
- Kafka KRaft 단일 브로커 (tech-env.md의 "리소스 부족 시 단일 브로커로 축소" 지침과 일치)
- 토픽: chat-messages, notifications(향후)
- JVM 힙 튜닝 필요(-Xmx512m -Xms256m), request 512Mi/0.5 vCPU, limit 1Gi/1 vCPU

## 캐시 / Pub-Sub
- Redis 단일 인스턴스 (logical-components.md 참고), request 128Mi/0.1 vCPU, limit 256Mi/0.25 vCPU

## 시크릿
- Vault, quickchat-data 네임스페이스, request 128Mi/0.1 vCPU, limit 256Mi/0.25 vCPU

## 네트워킹 (2026-08-18 정정 - Frontend Infrastructure Design 참고)
- Ingress Controller: nginx-ingress (Q4 답변 A)는 클러스터에 계속 유지하지만, Backend/Frontend 두 origin의 실제 라우팅에는 사용하지 않기로 정정됨 (Frontend NFR Design Question 4=B: 다른 origin 배포 결정에 따름)
- ~~Backend: ClusterIP + Ingress 경로(/api, /ws)~~ → **Backend: Service(NodePort 30081 → containerPort 8080)**로 변경. `SecurityConfig`에 CORS 설정 추가로 대응 (`aidlc-docs/construction/backend/code/api-layer-summary.md`의 "Post-Approval Patch 2" 참고)
- ~~Frontend: ClusterIP + Ingress 경로(/)~~ → **Frontend: Service(NodePort 30080 → containerPort 3000)**로 변경 (`aidlc-docs/construction/frontend/infrastructure-design/`  참고)
- TLS 종료 지점이 사라짐에 따른 제약: 1회차 멘토링 데모(내부 LAN) 범위에서는 평문 HTTP로 단순화, TLS는 범위 밖으로 명시. 상세 근거는 `aidlc-docs/construction/frontend/infrastructure-design/infrastructure-design.md`의 "알려진 제약" 참고

## 모니터링
- Prometheus + Grafana 기본 대시보드 포함 (Q5 답변 A)
- Prometheus request 256Mi/0.25 vCPU, limit 512Mi/0.5 vCPU (짧은 retention)
- Grafana request 128Mi/0.1 vCPU, limit 256Mi/0.25 vCPU

## Backend 애플리케이션 리소스
- 고정 1레플리카 (NFR Design 결정), request 512Mi/0.5 vCPU, limit 1Gi/1 vCPU

## 리소스 총합 참고 (limit 기준, Backend+공용 인프라)
PostgreSQL 512Mi + Kafka 1Gi + Redis 256Mi + Vault 256Mi + nginx-ingress 약 256Mi + Prometheus 512Mi + Grafana 256Mi + Backend 1Gi = 약 4.0Gi. 여기에 k3s 컨트롤플레인/OS 오버헤드(약 1~1.5Gi)와 Frontend(약 512Mi)를 더하면 약 5.5~6Gi. 현재 설치된 20GB 중 VM에 할당 가능한 14~16GB 범위 안에 여유롭게 들어가므로, Prometheus/Grafana 데이터 보존 기간을 늘리거나 Backend 레플리카를 추가하는 등 향후 확장 여지도 충분합니다.

## 참고
CPU 수치는 온라인 공개 스펙(Synology DS925+: AMD Ryzen V1500B 4코어/8스레드), RAM은 사용자가 확인해 준 현재 설치값(20GB)을 기준으로 했습니다. 컨테이너별 리소스 요청/제한은 일반적인 최소 기준으로 추정한 시작값이며, 실제 배포 후 모니터링 결과로 조정이 필요합니다.
