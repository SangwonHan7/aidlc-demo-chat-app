# QuickChat NAS 배포 (docker-compose)

Synology NAS(또는 Docker/docker-compose가 설치된 다른 서버)에 QuickChat을 docker-compose로 배포하는 절차. k3s 매니페스트(`infra/k3s/`)도 저장소에 남아있지만, 이번 배포는 단순성을 우선해 docker-compose로 진행하기로 결정했다(배경은 `aidlc-docs/audit.md`, `aidlc-docs/construction/backend/infrastructure-design/deployment-architecture.md` 참고).

## 사전 조건
- NAS에 Docker + docker-compose(v2, `docker compose` 서브커맨드) 설치되어 있어야 함.
- 이 저장소가 NAS에서 접근 가능한 위치에 있어야 함(git clone 또는 파일 복사).
- NAS의 LAN IP 또는 호스트명을 미리 확인해둘 것(예: `192.168.1.50`).

## 1. 저장소를 NAS로 가져오기
NAS에서 git을 쓸 수 있다면:
```bash
git clone <repo-url>
cd aidlc-demo-chat-app/infra/docker-compose
```
git이 어렵다면 프로젝트 폴더 전체를 파일 공유/USB 등으로 복사해도 된다.

## 2. .env 설정
```bash
cp .env.example .env
```
`.env`를 열어 `NAS_HOST`를 NAS의 실제 LAN IP 또는 호스트명으로 바꾼다. 브라우저가 직접 접속하는 주소이므로 `localhost`를 그대로 두면 NAS가 아닌 다른 기기에서 접속할 때 API/WebSocket 호출이 실패한다.

`COMPOSE_PROJECT_NAME=quickchat`는 값을 그대로 두고, 6번(자동배포)을 함께 쓸 계획이라면 `~/quickchat-deploy/.env`에도 **똑같이** 이 값을 넣어야 한다 - docker compose는 기본적으로 실행 디렉터리 이름으로 프로젝트를 구분해서, 이 값이 다르면 여기서 띄운 postgres/redis/kafka/vault와 자동배포가 다루는 backend/frontend가 서로 다른 네트워크로 나뉘어 컨테이너 이름이 못 찾거나 포트가 충돌한다(6-5 참고).

## 3. 빌드 및 기동
```bash
docker compose up --build -d
```
- 이 세션에서 실제로 겪었던 이슈들: `bitnami/kafka:3.7` 이미지가 `bitnamilegacy/kafka:3.7`로 이전되어 이미 반영됨(수정 불필요), Docker Compose의 Bake 빌드가 오래된 `buildx` 플러그인에서 `unknown flag: --allow`로 실패하면 `COMPOSE_BAKE=false docker compose up --build -d`로 우회.
- `frontend` 이미지는 `NEXT_PUBLIC_*` 값을 빌드 시점에 정적으로 인라인하므로, `.env`의 `NAS_HOST`를 바꾼 뒤에는 반드시 `--build`로 재빌드해야 한다(런타임에 컨테이너를 재시작만 해서는 반영되지 않음).

## 4. 확인
```bash
docker compose ps
```
6개 컨테이너(postgres, redis, kafka, vault, backend, frontend)가 모두 `Up` 상태인지 확인. 브라우저에서 `http://<NAS_HOST>:3000` 접속 후 회원가입/로그인/채널 생성/메시지 전송/새로고침 후 이력 유지까지 확인한다.

문제가 있으면:
```bash
docker compose logs -f backend
docker compose logs -f frontend
```

## 5. 알려진 제약 (그대로 배포에 반영됨, 데모/실습 범위이므로 이번 라운드에서는 수정하지 않음)
- **평문 개발용 자격증명**: `docker-compose.yml`의 `POSTGRES_PASSWORD`/`JWT_SECRET`/`VAULT_DEV_ROOT_TOKEN_ID`가 평문으로 파일에 들어있음(보안 점검 M8 항목, `.gstack/security-reports/` 참고). 실제 운영 배포라면 `.env`로 옮기고 `.env`를 `.gitignore`에 포함해야 하지만, 이번 NAS 배포는 로컬 네트워크 내 실습 데모 목적이라 범위 밖으로 둔다.
- **Vault는 배치되지만 실제로 쓰이지 않음**: Backend는 Vault 연동 없이 환경변수를 직접 읽는다(`application.yml`). Vault 컨테이너는 원래 설계상의 컴포넌트로 함께 띄우지만, dev 모드라 재시작 시 등록된 시크릿도 사라진다.
- **Kafka 데이터 비영속**: `kafka` 서비스에 볼륨을 붙이지 않았다. 컨테이너 재시작 시 미처리 메시지가 사라질 수 있으나, 메시지의 원본은 PostgreSQL에 저장되므로(Kafka는 실시간 브로드캐스트 용도) 데이터 손실은 아니다.
- **HTTPS 미적용**: 평문 HTTP로 서빙된다(NAS Infrastructure Design에서 이미 데모 범위로 명시된 제약).

## 6. GitHub Actions로 자동배포 설정 (SSH)
`main`에 push되면 GitHub Actions가 이미지를 빌드해 ghcr.io에 올린 뒤, GitHub 호스팅 러너가 SSH로 NAS에 접속해 pull+재기동까지 자동으로 수행한다(`.github/workflows/cd.yml`의 `deploy` job). NAS가 외부에서 SSH로 접근 가능하다는 전제로 동작하며, 별도 러너 설치는 필요 없다.

### 6-1. NAS 쪽 준비 (한 번만)
이미지는 GitHub에서 빌드되어 ghcr.io에 올라가므로, NAS에는 backend/frontend 소스 코드나 저장소 전체가 필요 없다. CD가 매 배포마다 `docker-compose.yml`/`docker-compose.deploy.yml` 두 파일을 `~/quickchat-deploy/`로 자동 복사해주므로, NAS에서 미리 만들어둘 것은 그 폴더와 `.env`뿐이다(CD는 `.env`를 절대 덮어쓰지 않음):
1. 배포용 `.env` 준비:
   ```bash
   mkdir -p ~/quickchat-deploy
   # .env.example 내용을 참고해 ~/quickchat-deploy/.env를 직접 만들고 NAS_HOST를 실제 주소로 채운다
   # (.env.example은 이 저장소의 infra/docker-compose/.env.example에서 내용만 복사해오면 된다)
   ```
   `COMPOSE_PROJECT_NAME=quickchat`을 반드시 포함하고, 2번(수동 배포)에서 쓴 `.env`와 **동일한 값**으로 맞춘다 - 값이 다르면 postgres/redis/kafka/vault(수동으로 띄운 것)와 backend/frontend(자동배포가 재기동하는 것)가 서로 다른 프로젝트로 인식되어 통신이 안 되거나 포트가 충돌한다.
2. 배포 전용 SSH 사용자/키를 별도로 만드는 것을 권장한다(기존 관리자 계정을 그대로 쓰기보다 권한을 좁힐 수 있음):
   ```bash
   ssh-keygen -t ed25519 -f deploy_key -C "github-actions-deploy" -N ""
   # deploy_key.pub의 내용을 NAS의 해당 사용자 ~/.ssh/authorized_keys에 추가
   ```
   그 사용자가 `docker` 그룹에 속해 있어야 `sudo` 없이 `docker compose` 명령을 실행할 수 있다.
3. (권장) NAS의 SSH 설정에서 비밀번호 로그인은 끄고 키 기반 인증만 허용, 기본 22번 포트 대신 다른 포트 사용, fail2ban 등으로 무차별 대입 시도를 막는 것을 고려한다 - SSH를 인터넷에 직접 열어두는 구성이므로 일반적인 노출 최소화 조치.

참고: 만약 위 1~5단계(수동 `docker compose up --build -d`)도 병행하고 싶다면 그건 backend/frontend를 NAS에서 직접 빌드하는 별도 경로라 저장소 전체 clone이 필요하다 - 이 경우 `~/quickchat-deploy/`와는 별개의 디렉터리(예: `~/aidlc-demo-chat-app`)를 써서 서로 섞이지 않게 한다.

### 6-2. 리포지토리 Secrets/Variables 설정
Settings → Secrets and variables → Actions에서:
- **Secrets**: `NAS_SSH_HOST`(NAS의 외부 접속 주소), `NAS_SSH_PORT`(SSH 포트), `NAS_SSH_USER`(위에서 만든 배포용 사용자), `NAS_SSH_PRIVATE_KEY`(`deploy_key`의 **개인키** 전체 내용)
- **Variables**: `NAS_HOST`(프론트엔드 이미지 빌드 시 `NEXT_PUBLIC_*`에 baked-in되는 값 - `~/quickchat-deploy/.env`의 `NAS_HOST`와 같은 값을 넣는다. 외부 접속 주소인 `NAS_SSH_HOST`와는 다를 수 있음 - 예를 들어 SSH는 공유기의 외부 포트를 통해 들어오지만, 브라우저가 접속할 주소는 사설 IP인 경우)

### 6-3. 동작 확인
`main`에 커밋을 push한 뒤 저장소의 Actions 탭에서 `CD - Build, Push & Deploy` 워크플로우가 `build-backend`/`build-frontend`/`deploy` 3개 job 모두 성공하는지 확인한다. `deploy` job 로그에서 SSH 접속, compose 파일 전달, `docker compose pull/up`, 헬스체크(NAS 자신이 `localhost:8080`/`:3000`으로 확인) 결과를 볼 수 있다.

### 6-4. 이 방식의 알려진 제약
- 완전 자동화가 필요 없다면 이 섹션은 건너뛰고 위 1~5단계(수동 `docker compose up --build -d`)만으로도 충분하다.
- SSH를 인터넷에 노출하는 구성이므로, 배포 전용 사용자/키로 권한을 좁히고 6-1의 4번 권장 조치를 적용할 것.
- `deploy` job은 백엔드/프런트엔드 컨테이너만 재기동한다(`pull backend frontend`, `up -d --no-build backend frontend`) - PostgreSQL/Kafka/Redis/Vault는 건드리지 않는다. 단, 이게 실제로 성립하려면 6-5처럼 프로젝트 이름이 일치해야 한다.
- NAS가 SSH로만 접근 가능하고 외부에서 열려 있지 않다면(포트포워딩 미설정 등), 이 방식 대신 NAS에 GitHub Actions self-hosted runner를 설치하는 방식(NAS가 GitHub 쪽으로 나가는 연결만 사용, 인바운드 포트 불필요)을 고려할 수 있다.

### 6-5. 문제 해결 - 포트 충돌("address already in use") / 서비스를 못 찾는 문제
`docker compose up`은 실행 디렉터리 이름을 프로젝트 이름으로 쓴다(`-p`/`COMPOSE_PROJECT_NAME` 미지정 시). 수동 배포는 보통 `~/aidlc-demo-chat-app/infra/docker-compose`에서, 자동배포는 `~/quickchat-deploy`에서 실행되므로 디렉터리 이름이 다르면 서로 다른 프로젝트로 인식된다. 이 상태에서 자동배포가 `up -d`를 실행하면 이미 수동으로 띄워둔 postgres 등과 같은 호스트 포트(5432 등)를 새 프로젝트에서 또 열려고 해서 `bind: address already in use` 에러가 난다(실제로 겪은 에러).

**해결**: 두 위치의 `.env`에 `COMPOSE_PROJECT_NAME=quickchat`을 동일하게 넣어 프로젝트 이름을 고정한다(`.env.example`에 이미 포함됨). 이미 서로 다른 프로젝트 이름으로 컨테이너가 떠 있는 상태라면 한쪽을 정리해야 한다:
```bash
# 지금 떠 있는 컨테이너와 프로젝트 이름을 확인
docker ps -a --format "table {{.Names}}\t{{.Ports}}"

# 잘못된 프로젝트 이름으로 뜬 스택 정리(예: quickchat-deploy로 뜬 것)
docker compose -p quickchat-deploy -f docker-compose.yml -f docker-compose.deploy.yml down

# 그 다음 postgres/redis/kafka/vault가 있는 원래 디렉터리에서 .env에 COMPOSE_PROJECT_NAME=quickchat을
# 넣은 뒤 전체를 한 번 다시 올린다
docker compose up --build -d
```
이후 자동배포가 실행되면 같은 `quickchat` 프로젝트 안에서 backend/frontend만 갈아끼우게 된다.

**NAS에 docker가 아닌 서비스가 이미 그 포트를 쓰고 있는 경우**(예: NAS 자체 PostgreSQL 패키지가 5432를 쓰고 있어 kill해도 다시 떠 있는 경우): docker 컨테이너 쪽 문제가 아니라 포트가 이미 다른 프로세스 소유라 근본적으로 피해야 한다. `docker-compose.yml`의 `postgres` 서비스는 호스트 포트를 `5433:5432`로 바꿔뒀다(컨테이너 내부 포트는 여전히 5432이고 backend는 `postgres:5432`라는 내부 네트워크 주소로 접속하므로 이 변경과 무관하게 동작함 - 5433은 NAS에서 `psql` 등으로 직접 접속하고 싶을 때만 쓰는 포트). redis(6379)/kafka(9092)/vault(8200)도 같은 방식으로 겹치면 왼쪽(호스트) 포트만 다른 값으로 바꾸면 된다.

## 참고: k3s 매니페스트
`infra/k3s/`에 namespaces/data/app/observability 매니페스트가 이미 작성되어 있다(원래 설계에 따른 k3s 배포 경로). 이번에는 사용하지 않지만, 추후 다중 노드/오토스케일링/롤링 업데이트가 필요해지면 참고할 수 있도록 남겨둔다.
