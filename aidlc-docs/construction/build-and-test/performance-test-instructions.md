# Performance Test Instructions - QuickChat

## Purpose
requirements.md에 정의된 성능 목표를 실습 데모 규모에 맞게 가볍게 확인한다. Frontend NFR Requirements(Question 3 답변 A)에서 이미 "정량적 성능 기준(Lighthouse 점수 등)은 설정하지 않음 - 실습 데모 수준 목표 유지"로 범위를 좁혀뒀으므로, 이 문서도 엔터프라이즈급 부하 테스트 셋업이 아니라 목표 달성 여부를 확인할 수 있는 최소한의 스크립트만 다룬다.

## Performance Requirements (requirements.md 그대로)
- **응답 시간**: 메시지 전달 지연 p50 500ms 미만 (전송 -> 수신자 화면 반영까지, Frontend 렌더링 오버헤드 포함)
- **처리량**: 별도 수치 목표 없음(채팅 앱 특성상 요청/초보다는 동시 세션 수가 더 의미 있는 지표)
- **동시 사용자**: 500 동시 접속 WebSocket 세션 안정 처리
- **에러율**: 메시지 유실률 0% (Kafka 기반 재처리로 보장한다는 전제 - 재처리 로직 자체가 실제로 동작하는지는 integration-test-instructions.md Scenario 7에서 별도 확인)
- **가동률**: 99.5%(실습 기간 - 장기 내구성 테스트 대상이 아니므로 이 문서에서는 다루지 않음)

## Setup Performance Test Environment

### 1. Prepare Test Environment
```bash
cd infra/docker-compose && docker compose up -d
# 또는 k3s 배포본에서: kubectl get pods -n quickchat-app
```
NAS(Synology DS925+, VM에 14~16GB 할당) 단일 노드라는 것을 감안해, 500 동시 세션 테스트는 운영 환경과 동일한 리소스 제약 하에서 수행해야 결과가 의미 있다 - 개발자 로컬 머신에서 돌리면 낙관적인 결과가 나올 수 있다.

### 2. Configure Test Parameters
- **Test Duration**: 5분 (짧은 데모성 부하 테스트 - 장시간 내구성 테스트는 범위 밖)
- **Ramp-up Time**: 60초 (0 -> 500 동시 연결)
- **Virtual Users**: 500 (WebSocket 연결 500개, 그중 일부만 활성 송신)

## Run Performance Tests

### 1. Execute Load Tests (메시지 전달 지연, k6 예시)

k6는 WebSocket 프로토콜은 지원하지만 STOMP 프레임을 직접 지원하지 않으므로, STOMP CONNECT/SUBSCRIBE/SEND 프레임을 raw text로 조립해야 한다. 아래는 뼈대 스크립트 예시(실제 실행 전 완성 필요):

```javascript
// perf/message-latency.js (신규 작성 필요 - 아직 존재하지 않음)
import ws from 'k6/ws';
import { check } from 'k6';

export const options = {
  scenarios: {
    concurrent_sessions: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '60s', target: 500 },
        { duration: '240s', target: 500 },
      ],
    },
  },
};

export default function () {
  const url = __ENV.WS_URL || 'ws://localhost:8080/ws';
  const accessToken = __ENV.ACCESS_TOKEN; // 사전에 로그인해서 발급받은 토큰을 주입
  ws.connect(url, {}, function (socket) {
    socket.on('open', () => {
      socket.send(`CONNECT\naccept-version:1.2\nAuthorization:Bearer ${accessToken}\n\n\0`);
    });
    // CONNECTED 수신 후 SUBSCRIBE, 이후 SEND 프레임의 송신 타임스탬프와 수신 타임스탬프 차이를 기록
    // ... (STOMP 프레임 파싱/조립 로직 추가 필요)
  });
}
```

```bash
k6 run --env WS_URL=ws://localhost:8080/ws --env ACCESS_TOKEN=<token> perf/message-latency.js
```

### 2. Execute Stress Tests
500을 목표치로 두고 있으므로 스트레스 테스트는 700~1000 동시 연결까지 램프업해 실패 지점(연결 거부, 응답 지연 급증)을 확인하는 정도로 충분하다 - 별도 스크립트 없이 위 k6 스크립트의 `target` 값만 올려서 재사용.

### 3. Analyze Performance Results
- **Response Time**: p50 500ms 미만 달성 여부 (k6 출력의 커스텀 메트릭 또는 애플리케이션 로그 타임스탬프 대조)
- **동시 연결**: 500개 연결이 전부 성공적으로 CONNECTED 상태에 도달하는지, Redis(`presence:{userId}` 키)와 PostgreSQL 커넥션 풀(`HikariCP` 기본 설정 - `application.yml`에 명시적 풀 크기 설정이 없다면 기본값이 500 동시 연결을 감당하기에 부족할 수 있음, 확인 필요)
- **Bottlenecks**: 단일 노드 NAS 환경이라 Kafka/Redis/PostgreSQL이 모두 같은 물리 자원을 공유 - CPU/메모리 경합이 가장 유력한 병목
- **Results Location**: k6 콘솔 출력, 필요시 `--out json=perf/results.json`으로 저장

## Performance Optimization

If performance doesn't meet requirements:
1. HikariCP 풀 크기, Redis 커넥션 풀, Kafka consumer 설정(`spring.kafka.consumer.*`) 확인 - 기본값 그대로면 500 동시 연결에 부족할 가능성이 가장 높음
2. STOMP 하트비트 미설정(알려진 제약)이 유휴 연결 정리에 영향을 주는지 확인
3. 고정 1 레플리카(NFR Design "고정 레플리카" 패턴)라는 것을 감안 - 500 동시 세션이 단일 파드/단일 JVM에서 안정적이지 않다면, 이 결정 자체를 재검토해야 할 수 있음(오토스케일링 미적용 결정을 뒤집는 것은 이번 Build and Test 범위를 넘는 아키텍처 변경이므로, 결과를 사용자에게 보고하고 별도 논의 필요)

## 알려진 제약 (투명성 목적)
- 위 k6 스크립트는 뼈대만 제공했고 STOMP 프레임 파싱까지 완성된 실행 가능 스크립트는 아니다 - 샌드박스에서 k6/WebSocket을 실제로 띄워 검증할 수 없었기 때문에, 실행 가능한 스크립트를 완성하는 것 자체가 이 문서를 실행하는 사람의 첫 작업이 된다
- 500 동시 세션이라는 목표치가 이 NAS 단일 노드 환경에서 현실적인지 자체도 아직 검증된 적이 없다 - 최초 실행 결과가 목표에 크게 못 미치면 목표 자체의 재협상이 필요할 수 있다
