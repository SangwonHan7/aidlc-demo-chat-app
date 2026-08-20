# Deployment Architecture - Frontend Unit

## 배포 단위
- Frontend: Deployment(1 replica, Next.js Node 서버 `next start`) + Service(NodePort 30080 → containerPort 3000) - `frontend/` 폴더에서 이미지 빌드

## 매니페스트 디렉터리 구조 (infra/ 폴더, Backend와 공유 - unit-of-work.md 코드 구조와 일치)
```
infra/
├── docker-compose/            (NAS RAM 부족 시 축소 운영용, Backend 서비스 이미 포함)
│   └── docker-compose.yml
├── k3s/
│   ├── namespaces.yaml
│   ├── data/                  (postgresql.yaml, kafka.yaml, redis.yaml, vault.yaml)
│   ├── app/                   (backend-deployment.yaml, backend-service.yaml,
│   │                            frontend-deployment.yaml, frontend-service.yaml)
│   │                          (ingress.yaml은 이번 Frontend/Backend 라우팅에는 사용하지 않음 -
│   │                           향후 다른 경로 노출이 필요해지면 재사용)
│   └── observability/         (prometheus.yaml, grafana.yaml)
└── scripts/                   (vault 시크릿 등록, kafka 토픽 생성 스크립트)
```

## 배포 순서 (Backend 순서에 Frontend 단계 추가)
1. `namespaces.yaml` 적용 (quickchat-app, quickchat-data)
2. quickchat-data: PostgreSQL, Kafka, Redis, Vault 기동
3. Vault 초기화 및 시크릿 등록 스크립트 실행
4. quickchat-app: Backend Deployment + Service(NodePort 30081 → 8080)
5. Prometheus/Grafana 설치 (nginx-ingress는 유지하되 이번 두 origin 라우팅에는 사용하지 않음)
6. quickchat-app: Frontend Deployment + Service(NodePort 30080 → 3000) - Backend API 준비 완료 후 (unit-of-work.md 개발 순서와 일치)

## 환경변수 주입
- Backend 컨테이너: `CORS_ALLOWED_ORIGIN=http://quickchat.local:30080` (기존 `application.yml`의 `quickchat.cors.allowed-origin` 프로퍼티에 매핑) - 이건 일반적인 컨테이너 실행 시점(runtime) 환경변수라 Pod env로 주입하면 된다
- Frontend: `NEXT_PUBLIC_API_BASE_URL=http://quickchat.local:30081`, `NEXT_PUBLIC_WS_URL=ws://quickchat.local:30081/ws` **다만 이 둘은 Pod 실행 시점 환경변수로 주입해도 반영되지 않는다** - Frontend Code Generation 중 발견한 정정: 전체 컴포넌트가 "use client"인 Next.js 앱에서 `NEXT_PUBLIC_*` 값은 `next build` 시점에 클라이언트 JS로 정적 인라인되고, 컨테이너 실행 시 환경변수를 바꿔도 이미 빌드된 결과물에는 영향이 없다. 따라서 이 두 값은 **Docker 이미지 빌드 시점의 `--build-arg`**로 넘겨야 한다(`frontend/Dockerfile` 참고 - `ARG`/`ENV`로 받아 `npm run build`보다 먼저 설정). 실무적 함의: 이 이미지는 배포 환경(호스트/포트)마다 다시 빌드해야 한다는 뜻이며, 환경별로 재빌드하지 않으려면 런타임에 설정을 읽어오는 방식(예: 클라이언트가 별도 `/api/config` 같은 엔드포인트를 호출)으로 바꿔야 하는데, 이는 이번 데모 범위를 벗어나는 아키텍처 변경이라 적용하지 않았다 - Infra 유닛이 실제 k3s 매니페스트/빌드 파이프라인을 작성하는 마무리 단계에서 재검토 필요

## RAM 임계값에 따른 분기
Backend와 동일한 기준 적용 (현재 20GB 설치됨, 16GB 권장선 이상이므로 k3s로 바로 진행). 참고용 축소 경로(Docker Compose)에서는 Frontend도 `infra/docker-compose/docker-compose.yml`에 서비스로 추가하고, 포트는 호스트에 그대로 노출(`3000:3000`, `8080:8080`)해 k8s NodePort 제약 없이 단순하게 구성한다.
