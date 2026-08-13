# Components - QuickChat (Backend)

Application Design Plan 답변 반영: Q1=A(4개 컴포넌트 + Presence 분리), Q3=A(Event 컴포넌트 분리), Q4=B(백엔드 중심, 프론트엔드는 Construction Functional Design에서 다룸)

## AuthComponent
- 책임: 회원가입, 로그인, JWT(Access/Refresh) 발급 및 검증, 5회 실패 시 계정 잠금
- 인터페이스: 회원가입, 로그인, 토큰 검증, 토큰 재발급
- Related FR: FR-1

## ChannelComponent
- 책임: 채널 생성(공개/초대 전용), 멤버십 관리(초대/제외/참여), 채널 목록/멤버 조회
- 인터페이스: 채널 생성, 멤버 초대/제외, 채널 참여(공개), 멤버십 확인, 사용자별 채널 목록 조회
- Related FR: FR-3, FR-4

## MessagingComponent
- 책임: 메시지 저장, 메시지 이력 페이지 조회, 브로드캐스트 트리거(Redis Pub/Sub 경유)
- 인터페이스: 메시지 저장, 이력 조회, 브로드캐스트
- Related FR: FR-2, FR-5, FR-7, FR-8

## PresenceComponent
- 책임: 사용자 온라인/오프라인 상태 추적(Redis 기반), 상태 조회 및 변경 구독
- 인터페이스: 온라인 표시, 오프라인 표시, 상태 조회, 상태 변경 구독
- Related FR: FR-6

## EventComponent
- 책임: Kafka 발행/구독 캡슐화 - 다른 컴포넌트가 Kafka 클라이언트를 직접 다루지 않도록 함 (Q3 답변 A)
- 인터페이스: 토픽 발행, 토픽 구독
- Related FR: FR-2, FR-3 (비동기 처리 기반)

## 참고
프론트엔드(Next.js) 컴포넌트 구조는 이번 단계 범위에 포함하지 않음 (Q4 답변 B). Construction 단계에서 Frontend 유닛의 Functional Design 시 정의.
