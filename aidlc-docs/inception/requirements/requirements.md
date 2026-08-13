# Requirements Document: QuickChat (AI-DLC 데모)

## Intent Analysis Summary
- User Request: "ai dlc 사용하여 작업 시작해줘" (requirements/vision.md, requirements/tech-env.md 기반 신규 프로젝트 시작)
- Request Type: New Project (Greenfield)
- Scope Estimate: System-wide (인증, 1:1/그룹 실시간 메시징, WebSocket, Kafka, Redis, Vault, Kubernetes 인프라 전체)
- Complexity Estimate: Moderate (요구사항 자체는 vision.md/tech-env.md에 명확히 정리되어 있으나, 다중 컴포넌트/온프레미스 인프라 통합이 필요)
- Requirements Depth: Standard (기존 vision.md/tech-env.md가 상세하여 미해결 사항 확인 위주로 진행)

## Functional Requirements (MVP)

| ID | Requirement | Priority | Source |
|---|---|---|---|
| FR-1 | 이메일+비밀번호 회원가입/로그인, JWT(Access+Refresh) 발급 | Must Have | vision.md |
| FR-2 | 1:1 다이렉트 메시지 실시간 송수신 | Must Have | vision.md |
| FR-3 | 그룹 채널 생성/참여/메시지 송수신 | Must Have | vision.md |
| FR-4 | 채널 공개 범위 선택 - 채널 생성 시 "공개"(누구나 참여) 또는 "초대 전용" 중 선택 | Must Have | Q1 답변 C (vision.md 대비 범위 확대) |
| FR-5 | WebSocket(STOMP) 기반 실시간 전송 (폴링 미사용) | Must Have | tech-env.md |
| FR-6 | 온라인/오프라인 상태 표시 | Should Have | vision.md |
| FR-7 | 채널/DM 메시지 이력 페이지 조회 | Must Have | vision.md |
| FR-8 | 메시지 보관: 별도 삭제 정책 없이 무제한 보관 (MVP) | Must Have | Q2 답변 C |

Out of Scope (MVP, 변경 없음): 파일/이미지 첨부, 메시지 검색, 읽음 확인, 음성/영상 통화, 모바일 앱, 외부 SSO 연동.

## Non-Functional Requirements

### Performance & Reliability
- 메시지 전달 지연 p50 500ms 미만
- 500 동시 접속 세션 안정 처리
- 서비스 가동률 99.5%(실습 기간), 메시지 유실률 0%(Kafka 기반 재처리)

### Infrastructure
- 자체 NAS 온프레미스, k3s/k0s 경량 Kubernetes, 자체 호스팅 Redis(Pub/Sub)/Kafka(KRaft)/Vault
- NAS 리소스가 Kubernetes를 감당하지 못할 경우의 Docker Compose 축소 기준은 지금 정하지 않고, Construction 단계 Infrastructure Design에서 실제 NAS 사양 확인 후 결정 (Q3 답변 B)

### Security
- 기본 보안 요구사항(tech-env.md 그대로 유지, Extension 여부와 무관하게 적용): JWT 인증, TLS(Ingress 종료), Vault 시크릿 관리, 메시지 XSS 이스케이프, 로그인 5회 실패 시 잠금
- Security Baseline Extension(추가 블로킹 감사 규칙)은 미적용 - 실습성 프로젝트로 판단 (Q5 답변 B)
- Resiliency Baseline Extension 미적용 - 실습성 프로젝트로 판단 (Q6 답변 B)

### Testing
- Unit Test 80%+ (JUnit5+Mockito 백엔드, Vitest 프론트엔드), Integration Test(메시지 송수신/인증 API), E2E(Playwright, Should Have)
- Property-Based Testing Extension 전체 규칙(PBT-01~10) 적용, Full Enforcement (Q7 답변 A) - 예시 기반 테스트를 대체하지 않고 보완. Functional Design 단계부터 컴포넌트별 테스트 가능한 속성(라운드트립/불변량/멱등성 등)을 식별해 Code Generation까지 이어감
  - 프레임워크는 NFR Requirements 단계에서 최종 확정 (백엔드 Java -> jqwik 권장, 프론트엔드 TypeScript -> fast-check 권장, 각각 JUnit5/Vitest와 통합 가능)

## Open Items Carried Forward (Blocking 아님, 후속 단계에서 결정)
- NAS 리소스 임계값 및 Docker Compose 축소 기준 -> Infrastructure Design 단계
- PBT 프레임워크 최종 선택 -> NFR Requirements 단계

## Extension Configuration Summary
| Extension | Enabled | 근거 |
|---|---|---|
| Security Baseline | No | Q5 답변 B - 실습성 프로젝트, 기본 보안 요구사항은 유지 |
| Resiliency Baseline | No | Q6 답변 B - 실습성 프로젝트 |
| Property-Based Testing | Yes (Full) | Q7 답변 A |

## Session Scope
이번 세션은 Requirements Analysis 단계까지만 진행합니다 (Q4 답변 A). User Stories/Workflow Planning 등 이후 단계는 준비되는 대로 이어서 진행합니다.
