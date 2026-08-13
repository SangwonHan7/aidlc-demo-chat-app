# Unit of Work Dependency - QuickChat

## Dependency Matrix

| From | To | Reason |
|---|---|---|
| Backend | Infra (로컬 개발 환경) | Kafka/Redis/Vault 실제 의존성에 대해 개발/테스트 필요 |
| Frontend | Backend | REST/WebSocket API 계약 (Question 2 답변 A - Backend 우선 완료 후 시작) |
| Infra (K8s 배포 매니페스트) | Backend, Frontend | 컨테이너 이미지가 있어야 배포 매니페스트 완성 가능 |

## 순서
Infra(로컬 개발 환경) -> Backend -> Frontend -> Infra(K8s 배포 매니페스트 마무리)

## 참고
Backend와 Frontend는 순차 진행(Question 2 답변 A)이므로 병렬 진행 시의 API 계약 동기화 이슈는 해당 없음.
