# Shared Infrastructure - QuickChat

Backend와 Frontend 유닛이 함께 사용하는 인프라 자원입니다 (Infra 유닛의 산출물, infra/ 폴더).

## 공유 자원
- k3s 클러스터 자체 (단일 노드, Synology DS925+의 VMM 안 Ubuntu VM)
- nginx-ingress (Backend API/WebSocket 경로와 Frontend 경로를 함께 라우팅)
- Prometheus + Grafana (두 유닛의 메트릭을 함께 수집/시각화)
- Namespace 전략: quickchat-app(Backend+Frontend), quickchat-data(Backend 전용 데이터 인프라)

## Backend 전용 (Frontend는 직접 접근하지 않음)
- PostgreSQL, Kafka, Redis, Vault - quickchat-data 네임스페이스, Backend를 통해서만 접근

## NAS 리소스 임계값 (requirements.md Open Item 해결)
- 기본 4GB RAM: Docker Compose 축소 운영 (참고용 축소 경로)
- 16GB 이상: k3s 단일 노드 운영 권장 (최소 권장선)
- 20GB (현재 설치됨): 권장 구성 - VM에 14~16GB 할당해 k3s 운영
- 32GB(최대): 여유 있는 운영
- 상세 근거는 construction/backend/infrastructure-design/infrastructure-design.md 참고
