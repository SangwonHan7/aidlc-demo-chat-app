# Unit of Work Story Map - QuickChat

| Story | Backend | Frontend | Infra |
|---|---|---|---|
| 1.1 회원가입/로그인 | Auth API(JWT 발급/검증) | 로그인/가입 화면 | Vault(JWT 서명키) |
| 1.2 1:1 DM | Messaging API + WebSocket | DM 화면 | Kafka(chat-messages), Redis |
| 1.3 그룹 채널 참여/메시지 | Channel+Messaging API + WebSocket | 채널 화면 | Kafka, Redis |
| 1.4 온라인 상태 확인 | Presence API | 상태 표시 UI | Redis |
| 1.5 메시지 이력 조회 | Messaging 이력 API | 이력 스크롤 UI | - |
| 2.1 채널 생성 및 공개범위 설정 | Channel API | 채널 생성 UI | - |
| 2.2 채널 구성원 초대/관리 | Channel API | 멤버 관리 UI | - |

## 참고
Infra 유닛은 특정 스토리에 직접 매핑되지 않고, 전체 스토리 실행에 필요한 런타임 환경(Kafka/Redis/Vault/K8s)을 제공하는 지원 유닛입니다.
모든 스토리는 Backend(API/비즈니스 로직)와 Frontend(UI) 양쪽 작업이 필요합니다.
