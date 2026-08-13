# Technical Environment Document: QuickChat

> AI-DLC 데모용 요구사항 문서입니다. Infra/Front-end/Back-end 스택은 "공공 with AIDD" 팀이 실제 프로젝트에서 사용할 스택과 동일하게 맞췄습니다 — 데모를 보면서 자신의 프로젝트에도 같은 스택이 어떻게 적용되는지 참고할 수 있습니다.

## Project Technical Summary

- **Project Name**: QuickChat
- **Project Type**: Greenfield
- **Primary Runtime Environment**: On-Premises (자체 NAS)
- **Cloud Provider**: None — AWS 등 퍼블릭 클라우드는 아직 지원되지 않아 현재는 미사용 (계정/권한이 준비되면 이후 단계에서 마이그레이션 검토)
- **Target Deployment Model**: Containers on Kubernetes (자체 NAS 서버에 구축한 온프레미스 클러스터, 예: k3s / k0s 등 경량 배포판. NAS 리소스가 부족하면 Docker Compose로 축소해 로컬 개발 후 배포)
- **Team Size**: 실습 팀 단위 (3-5인)
- **Team Experience**: Java/Spring Boot 및 React 기반 웹 개발 실습을 마친 교육생 수준. Kubernetes/Kafka는 실습을 통해 처음 다뤄봄.

---

## Programming Languages

### Required Languages

| Language | Version | Purpose | Rationale |
|----------|---------|---------|-----------|
| Java | 17+ | 백엔드 API, WebSocket 서버 | Spring Boot 표준, 팀 교육 과정과 일치 |
| TypeScript | 5.x | 프론트엔드 (Next.js) | 타입 안정성, React 생태계 표준 |

### Prohibited Languages

| Language | Reason | Use Instead |
|----------|--------|--------------|
| JavaScript (신규 코드) | 타입 안정성 확보를 위해 신규 코드는 TypeScript로 통일 | TypeScript |

---

## Frameworks and Libraries

### Required Frameworks

| Framework/Library | Version | Domain | Rationale |
|--------------------|---------|--------|-----------|
| Spring Boot | 3.x | 백엔드 REST + WebSocket(STOMP) API | 팀 표준 백엔드 프레임워크 |
| Spring Data JPA 또는 MyBatis | 최신 | 데이터 접근 계층 | 팀 선택에 따라 택1 |
| Next.js | 14.x | 프론트엔드 프레임워크 (React 18 기반) | 팀 표준 프론트엔드 스택 |
| Zustand | 최신 | 프론트엔드 전역 상태 관리 (대화방/메시지 상태) | 팀 표준, Redux보다 경량 |
| Vite | 최신 | 프론트엔드 빌드 도구 | 빠른 개발 서버/빌드 |

### Prohibited Libraries

| Library | Reason | Alternative |
|---------|--------|--------------|
| Redux | 팀 표준 상태관리 도구는 Zustand로 통일 | Zustand |
| Moment.js | 유지보수 종료, 번들 크기 이슈 | date-fns |
| 원시 JDBC 문자열 조합 쿼리 | SQL 인젝션 위험 | JPA / MyBatis 파라미터 바인딩 |

---

## Infrastructure Environment (자체 NAS 온프레미스)

### Hosting

- **Primary Environment**: 자체 NAS 서버 (On-Premises, Self-Hosted)
- **AWS 등 퍼블릭 클라우드**: 현재 미지원 — 계정/권한 준비 전까지는 사용하지 않음
- **위치/규모**: NAS 1대 기준 단일 노드 또는 소규모 클러스터

### Service Allow List

| Service | Approved Use Cases | Constraints |
|---------|---------------------|--------------|
| 온프레미스 Kubernetes (NAS 위 k3s/k0s 등 경량 배포판) | 백엔드/프론트엔드 컨테이너 오케스트레이션 | 네임스페이스 단위로 팀 리소스 분리. NAS 리소스(CPU/메모리)가 제한적이므로 레플리카 수는 최소화 |
| 자체 호스팅 Redis (컨테이너) | WebSocket 세션 Pub/Sub, 온라인 상태 캐시 | 파드 간 상태 공유 용도로만 사용. 관리형 서비스(ElastiCache 등) 아님 — 퍼시스턴스(AOF/RDB) 설정 직접 관리 |
| 자체 호스팅 Kafka (KRaft 모드, 컨테이너) | 메시지 이벤트 로그/비동기 알림 처리 | 토픽: chat-messages, notifications 등 목적별 분리. Zookeeper 없는 KRaft 모드로 자원 절약, 리소스 부족 시 단일 브로커로 축소 |
| HashiCorp Vault (컨테이너, 자체 호스팅) | DB 접속정보, JWT 서명 키 등 시크릿 관리 | 애플리케이션 코드에 시크릿 하드코딩 금지. 처음부터 자체 호스팅 전제였으므로 변경 없음 |

### Service Disallow List

| Service | Reason | Alternative |
|---------|--------|--------------|
| 애플리케이션 서버에 시크릿을 평문 환경변수로 직접 주입 | 시크릿 노출 위험 | Vault에서 런타임에 조회 |
| AWS/GCP/Azure 등 관리형 클라우드 서비스 | 현재 클라우드 계정/권한 미지원 | 자체 NAS 인프라의 동급 오픈소스 컴포넌트로 대체 (아래 매핑 참고) |

### 클라우드 관리형 서비스 → 자체 호스팅 대체 매핑

| 원래 계획 (관리형) | 자체 호스팅 대체 | 비고 |
|----------------------|--------------------|------|
| Amazon EKS | k3s / k0s (NAS 위 경량 Kubernetes) | 필요 시 Docker Compose로 로컬 개발 후 배포 |
| Amazon ElastiCache (Redis) | Redis 공식 컨테이너 이미지 | 퍼시스턴스(AOF/RDB) 설정 직접 관리 |
| Amazon MSK (Kafka) | Kafka 공식/Bitnami 컨테이너 (KRaft 모드) | 클러스터 리소스가 부족하면 단일 브로커로 축소 가능 |
| AWS Secrets Manager | HashiCorp Vault (자체 호스팅) | 변경 없음 — Vault는 처음부터 자체 호스팅 전제 |

> AWS 계정/권한이 준비되면 위 매핑을 역으로 적용해 관리형 서비스로 이전할 수 있도록, 애플리케이션 코드는 특정 클라우드 SDK에 직접 의존하지 않고 표준 프로토콜(Redis/Kafka 클라이언트, HTTP)로만 통신하도록 설계합니다.

---

## Preferred Technologies and Patterns

### Architecture Pattern

- **모듈형 백엔드 서비스** (MVP는 단일 Spring Boot 서비스, 필요 시 채팅/알림 서비스로 분리 가능한 구조로 패키지 분리)
- **실시간 통신**: WebSocket + STOMP 프로토콜. HTTP 폴링 방식은 사용하지 않음.
- **비동기 이벤트**: 메시지 전송/알림은 Kafka 토픽을 경유해 비동기 처리 가능하도록 설계

### API Design Standards

- **Style**: REST (일반 CRUD) + WebSocket(STOMP) (실시간 메시지)
- **Naming Convention**: URL은 kebab-case, JSON 필드는 camelCase
- **Error Format**: `{ "errorCode": "...", "message": "..." }` 형태의 표준 에러 응답

### Frontend Patterns

- **State Management**: Zustand (채팅방 목록, 현재 대화, 메시지 목록 등 전역 상태)
- **Build Tool**: Vite (Next.js 내부 빌드/개발 서버)

---

## Security Requirements

### Authentication and Authorization

- **Authentication Method**: 이메일/비밀번호 로그인 후 JWT 발급 (Access + Refresh Token)
- **Authorization Model**: 본인 계정의 채널/DM만 접근 가능 (소유자 기반 검증)

### Data Protection

- **Encryption in Transit**: TLS (Ingress 단에서 종료)
- **Secrets Management**: DB 접속정보, JWT 서명 키는 Vault에 저장하고 런타임에 조회

### Input Validation

- 메시지 본문은 저장/렌더링 전 XSS 방지를 위한 이스케이프 처리 필수
- 로그인 API는 비밀번호 5회 실패 시 짧은 시간 동안 잠금 처리 (기본적인 브루트포스 방지)

### Security Compliance Framework

- **Framework 선택**: OWASP Top 10 (2021) — 웹 애플리케이션/API 기준으로 팀이 이해하기 쉬운 범용 프레임워크이기 때문에 채택
- 상세 매핑은 AI-DLC의 NFR Requirements/Design 단계에서 생성

---

## Testing Requirements

### Test Strategy Overview

| Test Type | Required | Coverage Target | Tooling |
|-----------|----------|-------------------|---------|
| Unit Tests | Yes | 핵심 서비스 로직 80% 이상 | JUnit5 + Mockito (백엔드), Vitest (프론트엔드) |
| Integration Tests | Yes | 메시지 송수신, 인증 API | Spring Boot Test |
| E2E Tests | Should Have | 1:1 대화, 채널 대화 핵심 시나리오 | Playwright (선택) |

---

## Example and Template Code

### Example 1: WebSocket 메시지 전송 엔드포인트 (Spring Boot)

```java
@Controller
public class ChatController {

    private final ChatMessageService chatMessageService;

    public ChatController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @MessageMapping("/chat.send/{channelId}")
    @SendTo("/topic/channel/{channelId}")
    public ChatMessageResponse sendMessage(
            @DestinationVariable String channelId,
            @Payload ChatMessageRequest request) {
        return chatMessageService.saveAndPublish(channelId, request);
    }
}
```

### Example 2: 프론트엔드 채팅 상태 관리 (Zustand)

```typescript
import { create } from "zustand";

interface ChatMessage {
  id: string;
  senderId: string;
  content: string;
  sentAt: string;
}

interface ChatState {
  messagesByChannel: Record<string, ChatMessage[]>;
  addMessage: (channelId: string, message: ChatMessage) => void;
}

export const useChatStore = create<ChatState>((set) => ({
  messagesByChannel: {},
  addMessage: (channelId, message) =>
    set((state) => ({
      messagesByChannel: {
        ...state.messagesByChannel,
        [channelId]: [...(state.messagesByChannel[channelId] ?? []), message],
      },
    })),
}));
```

### Example 3: 백엔드 단위 테스트 패턴

```java
@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatMessageRepository repository;

    @InjectMocks
    private ChatMessageService service;

    @Test
    void savesMessageAndReturnsResponseWithTimestamp() {
        var request = new ChatMessageRequest("hello");
        var result = service.saveAndPublish("channel-1", request);

        assertThat(result.content()).isEqualTo("hello");
        verify(repository).save(any());
    }
}
```

---

## How This Document Feeds Into AI-DLC

이 문서는 QuickChat 데모의 Construction 단계에서 언어/프레임워크 선택, 인프라 설계, 보안 패턴, 테스트 전략의 기준이 됩니다. 실제 "공공 with AIDD" 팀 프로젝트에서는 이 문서와 같은 형식으로, 팀이 확정한 서비스 주제에 맞춰 별도의 vision.md / tech-env.md를 새로 작성해 사용하면 됩니다.
