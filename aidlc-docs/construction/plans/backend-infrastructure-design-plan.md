# Infrastructure Design Plan - Backend Unit

## Plan

- [ ] functional-design/nfr-design 산출물 기반으로 실제 인프라 서비스 매핑
- [ ] 아래 질문 답변 반영 (requirements.md의 NAS 리소스 임계값 Open Item 포함)
- [ ] infrastructure-design.md 작성
- [ ] deployment-architecture.md 작성
- [ ] shared-infrastructure.md 작성 (Kafka/Redis/Vault/K8s는 Frontend와도 공유되는 클러스터 자원이므로)

## Questions

각 질문에 알파벳으로 답한 뒤 채팅으로 "답변 완료"라고 알려주세요.

## Question 1 (Compute Infrastructure)

tech-env.md는 "k3s / k0s 등 경량 배포판"으로 열어두었습니다. 어느 것을 사용할까요?

A) k3s (더 널리 쓰이고 커뮤니티/문서가 많음)

B) k0s

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 2 (Compute Infrastructure - NAS 리소스 임계값, requirements.md Open Item)

NAS 리소스가 Kubernetes를 감당하지 못할 경우 Docker Compose로 축소하는 기준을 정해야 합니다. NAS 실제 사양을 알고 계신가요?

A) 사양을 알고 있음 - Other를 선택해 아래에 CPU/메모리 등 구체적 사양을 적어주세요

B) 아직 모름 - 우선 최소 리소스 가정(백엔드 1레플리카, request 0.5 vCPU/512Mi, limit 1 vCPU/1Gi 등)으로 설계하고 실제 배포 시 조정

C) NAS 준비 전이므로 이번 단계는 Docker Compose 기준으로만 설계하고 K8s 매니페스트는 최소 골격만 작성

D) Other (please describe after [Answer]: tag below)

[Answer]: D "Synology DS925+ 사용중"

## Question 3 (Storage Infrastructure)

tech-env.md에 RDBMS 종류가 명시되지 않았습니다. 어떤 데이터베이스를 사용할까요?

A) PostgreSQL

B) MySQL/MariaDB

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 4 (Networking Infrastructure)

Ingress Controller는 어떤 것을 사용할까요? (TLS는 Ingress에서 종료, tech-env.md 기준)

A) nginx-ingress 설치

B) k3s 기본 내장 Traefik 그대로 사용

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 5 (Monitoring Infrastructure)

Prometheus 메트릭 노출은 NFR Requirements에서 확정했습니다. Grafana 대시보드까지 이번 범위에 포함할까요? (vision.md 장기 통합 포인트)

A) 포함 - Prometheus + 기본 Grafana 대시보드까지 구성

B) 미포함 - 이번에는 Prometheus 메트릭 노출까지만, Grafana는 이후

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 6 (Shared Infrastructure)

K8s 네임스페이스 전략은 어떻게 할까요? (tech-env.md: "네임스페이스 단위로 팀 리소스 분리")

A) 단일 네임스페이스(quickchat)에 Backend/Frontend/Kafka/Redis/Vault 모두 배치

B) 애플리케이션(quickchat-app)과 데이터 인프라(quickchat-data)를 네임스페이스로 분리

C) Other (please describe after [Answer]: tag below)

[Answer]: B
