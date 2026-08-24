# Deployment Architecture - Backend Unit

## 배포 단위
- Backend: Deployment(1 replica) + Service(~~ClusterIP~~ → **NodePort 30081 → containerPort 8080**, 2026-08-18 정정 - Frontend NFR Design Q4=B에 따라 Backend/Frontend가 다른 origin으로 배포되며 nginx-ingress 경로 라우팅을 사용하지 않기로 함. 상세는 `aidlc-docs/construction/frontend/infrastructure-design/`, 본 문서의 "RAM 임계값" 하단 참고) - backend/ 폴더에서 이미지 빌드
- PostgreSQL, Kafka, Redis, Vault: StatefulSet + PVC(Synology CSI) - infra/ 폴더의 매니페스트로 관리, quickchat-data 네임스페이스
- nginx-ingress, Prometheus, Grafana: 클러스터 공용 인프라 - infra/ 폴더 (shared-infrastructure.md 참고)

## 매니페스트 디렉터리 구조 (infra/ 폴더, unit-of-work.md 코드 구조와 일치)
```
infra/
├── docker-compose/            (NAS RAM 4GB 등 축소 운영용)
│   └── docker-compose.yml
├── k3s/
│   ├── namespaces.yaml
│   ├── data/                  (postgresql.yaml, kafka.yaml, redis.yaml, vault.yaml)
│   ├── app/                   (backend-deployment.yaml, backend-service.yaml,
│   │                            frontend-deployment.yaml, frontend-service.yaml, ingress.yaml)
│   └── observability/         (prometheus.yaml, grafana.yaml)
└── scripts/                   (vault 시크릿 등록, kafka 토픽 생성 스크립트)
```

## 배포 순서
1. namespaces.yaml 적용 (quickchat-app, quickchat-data)
2. quickchat-data: PostgreSQL, Kafka, Redis, Vault 기동 (StatefulSet, PVC 바인딩 확인)
3. Vault 초기화 및 시크릿 등록 스크립트 실행 (DB 접속정보, JWT 서명 키)
4. quickchat-app: Backend Deployment (Vault에서 시크릿 조회 후 기동)
5. nginx-ingress, Prometheus/Grafana 설치
6. Frontend Deployment (Backend API 준비 완료 후 - unit-of-work.md의 개발 순서와 일치)

## RAM 임계값에 따른 분기 (원래 설계, 2026-08-24 아래 정정 참고)
- 현재 상태(20GB 설치됨, 16GB 권장선 이상): k3s/ 매니페스트로 바로 진행
- 참고용 축소 경로 - NAS RAM이 16GB 미만인 경우: docker-compose/docker-compose.yml로 로컬/NAS Docker에서 직접 실행 (레플리카 1개씩, Prometheus/Grafana 제외)

## 2026-08-24 정정 - 실제 NAS 배포 경로는 docker-compose로 변경
RAM 임계값상으로는 k3s가 권장 경로였고 실제로 `infra/k3s/`에 전체 매니페스트(namespaces, PostgreSQL/Kafka/Redis/Vault StatefulSet, Backend/Frontend Deployment, Prometheus/Grafana)를 작성했으나, 사용자가 최종적으로 단순성을 우선해 docker-compose 배포로 되돌리기로 결정했다. k3s 매니페스트는 삭제하지 않고 참고/향후 확장용으로 저장소에 남겨둔다. 실제 배포 절차는 `infra/docker-compose/README.md` 참고. 이 변경에 따라 `infra/docker-compose/docker-compose.yml`에 `NAS_HOST` 변수(`.env` 기반)를 추가해 `CORS_ALLOWED_ORIGIN`/`NEXT_PUBLIC_API_BASE_URL`/`NEXT_PUBLIC_WS_URL`이 로컬 개발과 NAS 배포 양쪽에서 재사용 가능하도록 했다.
