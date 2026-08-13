# Services - QuickChat (Backend)

Application Design Plan 답변 Q2=A: 여러 컴포넌트를 가로지르는 흐름은 별도 Orchestrator/Facade 서비스가 조정합니다.

## ChatFacadeService (Orchestrator)
- 책임: 메시지 전송처럼 여러 컴포넌트에 걸친 흐름을 조정. 개별 컴포넌트는 서로를 직접 호출하지 않고 Facade를 통해서만 조율됨
- 주요 오케스트레이션: 메시지 전송(sendMessage) - ChannelComponent(멤버십 확인) -> MessagingComponent(저장) -> EventComponent(발행) -> MessagingComponent(브로드캐스트)
- Related Components: ChannelComponent, MessagingComponent, EventComponent, PresenceComponent

## 컴포넌트 자체 서비스
- AuthComponent, ChannelComponent(멤버십 확인 제외한 단순 CRUD), PresenceComponent, EventComponent는 각자 컴포넌트 내부에서 자체 완결되는 단순 흐름(예: 로그인, 채널 생성)에는 Facade를 거치지 않고 Controller에서 직접 호출

## 판단 기준
- 컴포넌트 2개 이상을 가로지르는 흐름 -> ChatFacadeService 경유
- 단일 컴포넌트로 완결되는 흐름 -> 해당 컴포넌트 서비스 직접 호출
