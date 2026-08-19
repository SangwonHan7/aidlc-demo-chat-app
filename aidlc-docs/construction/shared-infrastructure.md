# Shared Infrastructure - QuickChat

Backend와 Frontend 유닛이 함께 사용하는 인프라 자원입니다 (Infra 유닛의 산출물, infra/ 폴더).

## 공유 자원
- k3s 클러스터 자체 (단일 노드, Synology DS925+의 VMM 안 Ubuntu VM)
- nginx-ingress (클러스터에 유지되지만, Backend/Frontend 두 origin의 실제 라우팅에는 사용하지 않음 - 2026-08-18 정정, 아래 "네트워킹 정정" 참고)
- Prometheus + Grafana (두 유닛의 메트릭을 함께 수집/시각화하되, Frontend는 커스텀 메트릭을 노출하지 않고 `/api/health`만 probe 대상)
- Namespace 전략: quickchat-app(Backend+Frontend), quickchat-data(Backend 전용 데이터 인프라)

## 네트워킹 정정 (2026-08-18)
최초 설계 시 Backend/Frontend를 같은 호스트 아래 경로(`/api`, `/ws`, `/`)로 라우팅한다고 가정했으나, Frontend NFR Design(Question 4=B)에서 **다른 origin**으로 배포하기로 결정했다. 이에 따라 Frontend Infrastructure Design 단계에서 다음으로 정정했다.
- Backend: Service(NodePort 30081 → containerPort 8080)
- Frontend: Service(NodePort 30080 → containerPort 3000)
- 두 서비스 모두 같은 호스트(`quickchat.local`)에 포트로 구분해 접근하며, nginx-ingress를 거치지 않음
- Backend `SecurityConfig`에 CORS 설정을 추가해 다른 origin 간 REST 호출을 허용 (`quickchat.cors.allowed-origin` / `CORS_ALLOWED_ORIGIN`)
- 알려진 제약: nginx-ingress가 담당하던 TLS 종료 지점이 사라짐 - 1회차 멘토링 데모(내부 LAN) 범위에서는 평문 HTTP로 단순화, TLS는 범위 밖으로 명시. 상세는 `aidlc-docs/construction/frontend/infrastructure-design/infrastructure-design.md` 참고

## Backend 전용 (Frontend는 직접 접근하지 않음)
- PostgreSQL, Kafka, Redis, Vault - quickchat-data 네임스페이스, Backend를 통해서만 접근

## NAS 리소스 임계값 (requirements.md Open Item 해결)
- 기본 4GB RAM: Docker Compose 축소 운영 (참고용 축소 경로)
- 16GB 이상: k3s 단일 노드 운영 권장 (최소 권장선)
- 20GB (현재 설치됨): 권장 구성 - VM에 14~16GB 할당해 k3s 운영
- 32GB(최대): 여유 있는 운영
- 상세 근거는 construction/backend/infrastructure-design/infrastructure-design.md 참고
