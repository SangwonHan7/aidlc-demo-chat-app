# Build Instructions - QuickChat

## Prerequisites

- **Build Tools**: JDK 17, Gradle (wrapper included, `backend/gradlew`) / Node.js 20, npm (frontend)
- **Dependencies**: PostgreSQL 16, Redis 7, Kafka(KRaft, 브로커+컨트롤러 단일 프로세스), HashiCorp Vault - 전부 `infra/docker-compose/docker-compose.yml`에 정의되어 있음
- **Environment Variables**: `backend/README.md`, `frontend/README.md`의 표 참고. 특히 Frontend의 `NEXT_PUBLIC_API_BASE_URL`/`NEXT_PUBLIC_WS_URL`은 **Docker 이미지 빌드 시점**의 `--build-arg`로 넘겨야 하며, 컨테이너 실행 시 환경변수로 주입해도 반영되지 않음(Next.js가 `next build` 시점에 클라이언트 JS로 정적 인라인하기 때문 - `frontend/README.md` "실행 방법 (Docker)" 절 참고)
- **System Requirements**: 로컬 개발은 8GB+ RAM 권장(PostgreSQL+Redis+Kafka+Vault+Backend+Frontend 동시 기동). k3s 배포 대상은 Synology DS925+ NAS(20GB RAM 중 VM에 14~16GB 할당) - `aidlc-docs/construction/backend/infrastructure-design/infrastructure-design.md` 참고

## Build Steps

### 1. Install Dependencies

```bash
# Backend
cd backend && ./gradlew dependencies   # 의존성 다운로드만 확인하려면; 실제로는 build/test 시 자동 해결됨

# Frontend
cd frontend && npm install
```

### 2. Configure Environment

```bash
# 의존 서비스 기동 (PostgreSQL/Redis/Kafka/Vault만 - backend/frontend는 아직 올리지 않음)
cd infra/docker-compose && docker compose up -d postgres redis kafka vault

# Backend 환경변수 (로컬 실행 시, docker-compose와 별개로 IDE/CLI에서 직접 뜨울 때)
export DB_URL=jdbc:postgresql://localhost:5432/quickchat
export DB_USERNAME=quickchat
export DB_PASSWORD=quickchat-local-dev-only
export REDIS_HOST=localhost REDIS_PORT=6379
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export JWT_SECRET=local-dev-only-jwt-secret-32-bytes-minimum   # 32바이트 이상, 실서비스는 Vault에서 주입
export CORS_ALLOWED_ORIGIN=http://localhost:3000

# Frontend 환경변수(.env.local) - Backend가 localhost:8080에 떠 있다면 기본값과 동일해 생략 가능
export NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
export NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws
```

### 3. Build All Units

```bash
# Backend: 컴파일 + 테스트 실행 + jar 생성
cd backend && ./gradlew clean build

# Frontend: 타입체크 + 빌드(NEXT_PUBLIC_* 인라인 확인 포함)
cd frontend && npm run build

# 컨테이너 이미지 (전체 스택을 docker-compose로 한 번에 올리는 경우)
cd infra/docker-compose && docker compose build
docker compose up -d
```

### 4. Verify Build Success

- **Expected Output**: `./gradlew build` → `BUILD SUCCESSFUL`, `backend/build/libs/backend-0.1.0.jar` 생성. `npm run build` → `.next/` 생성, "Compiled successfully" 출력, 빌드 로그에 `NEXT_PUBLIC_API_BASE_URL`/`NEXT_PUBLIC_WS_URL`이 의도한 값으로 인라인됐는지 `.next/static/chunks/*.js`에서 grep으로 확인 권장
- **Build Artifacts**: `backend/build/libs/*.jar`, `frontend/.next/`, Docker 이미지 `quickchat-backend`, `quickchat-frontend`(compose 기준 `docker-compose-backend`, `docker-compose-frontend`)
- **Common Warnings**: Lombok 어노테이션 프로세서 경고는 무시 가능. npm 설치 시 peer dependency 경고(react-window/@types 버전 범위)는 무시 가능 - 둘 다 이번 세션에서 실행 확인은 못했음(아래 알려진 제약 참고)

## Troubleshooting

### Build Fails with Dependency Errors
- **Cause**: `backend/build.gradle`/`frontend/package.json`에 lockfile이 없음(Gradle은 `dependency-management` 플러그인으로 BOM만 고정, npm은 `package-lock.json` 미생성) - 실행 시점에 따라 마이너 버전이 달라질 수 있음
- **Solution**: 최초 빌드 성공 시 `npm install`로 생성된 `package-lock.json`을 커밋해 버전을 고정할 것. Gradle은 필요시 `./gradlew dependencies --write-locks` 검토

### Build Fails with Compilation Errors
- **Cause**: 이 세션에서 작성된 모든 코드는 `mcp__workspace__bash` 샌드박스 장애(`VM_DISK_SPACE_INSUFFICIENT`)로 실제 컴파일러를 통과한 적이 없음(Read/Write/Edit 파일 도구로만 작성) - 정적 계약 감사(contract-test-instructions.md)로 타입/필드 수준 불일치는 별도로 확인했지만, 문법 오류나 import 누락 같은 순수 컴파일 오류는 배제하지 못함
- **Solution**: 이 문서 실행 시 첫 실제 컴파일이 이뤄지므로, 에러 발생 시 파일:라인을 특정해 보고 - 대부분 import 경로/제네릭 타입 불일치일 가능성이 높음

## 알려진 제약 (투명성 목적)
이 프로젝트는 AI가 파일 도구(Read/Write/Edit)만으로 생성했고, 세션 내내 셸 샌드박스가 `VM_DISK_SPACE_INSUFFICIENT`로 사용 불가해 `./gradlew build`/`npm run build`/`npm test`/`./gradlew test` 중 어느 것도 실제로 실행해보지 못했다. 이 문서에 적힌 명령/경로/버전은 각 유닛의 `build.gradle`, `package.json`, Dockerfile, docker-compose.yml을 직접 읽어 정확히 옮긴 것이지만, "실행하면 성공한다"는 보장은 이번 문서 자체가 아니라 이 문서를 실제로 실행하는 사람(또는 CI)이 처음으로 확인하게 된다.
