# Requirements 확인 질문 - QuickChat (AI-DLC 데모)

requirements/vision.md, requirements/tech-env.md를 검토했습니다. 대부분의 요구사항은 이미 두 문서에 잘 정리되어 있습니다. 아래는 vision.md에 명시된 미해결 사항(Open Questions), 이번 세션의 진행 범위, 그리고 확장(Extension) 적용 여부를 확인하기 위한 질문입니다.

각 질문에 A, B, C... 중 하나의 알파벳을 [Answer]: 뒤에 적어주세요. 맞는 선택지가 없으면 마지막 Other 옵션을 고르고 설명을 적어주세요. 모두 작성한 뒤 채팅으로 "답변 완료" 라고 알려주세요.

## Question 1 (vision.md Open Questions)

채널 참여 방식은 기본적으로 어떤 정책으로 할까요?

A) 공개 채널 - 누구나 목록을 보고 자유롭게 참여

B) 초대 전용 채널 - 채널 관리자가 초대해야 참여 가능

C) 둘 다 지원 - 채널 생성 시 공개/초대 전용을 선택 가능 (MVP 범위 확대)

D) Other (please describe after [Answer]: tag below)

[Answer]: C

## Question 2 (vision.md Open Questions)

메시지 보관 기간 정책은 어떻게 할까요?

A) 무제한 보관 (삭제 정책 없음)

B) 일정 기간만 보관 후 자동 삭제 (예: 90일)

C) MVP에서는 정책을 정하지 않고 무제한 보관, 이후 단계에서 재검토

D) Other (please describe after [Answer]: tag below)

[Answer]: C

## Question 3 (vision.md Open Questions)

NAS 리소스가 Kubernetes 클러스터를 감당하지 못할 경우 Docker Compose로 축소 운영하는 기준은 언제 정할까요?

A) 지금 바로 구체적인 리소스 임계값(CPU/메모리)을 정의

B) Construction 단계의 Infrastructure Design에서 실제 NAS 사양을 확인한 뒤 결정

C) 이번 실습 범위에서는 다루지 않음 (Kubernetes 사용을 전제로 진행)

D) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 4 (진행 범위)

이번 세션에서 AI-DLC 워크플로우를 어디까지 진행할까요? 진행 범위에 따라 이후 Workflow Planning 단계의 계획이 달라집니다.

A) Requirements Analysis까지만 - 요구사항 문서 생성까지 확인

B) Workflow Planning까지 - AI가 단계/유닛을 어떻게 나눌지까지 확인

C) Construction 중 1개 유닛의 Code Generation까지 - 실제 코드 생성까지 진행

D) 끝까지 - Construction 전체(모든 유닛) + Build and Test까지 진행

E) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 5: 보안(Security) 확장 규칙

이 프로젝트에 보안 확장 규칙을 강제로 적용할까요?

A) 예 - 모든 SECURITY 규칙을 필수(blocking) 제약으로 적용 (프로덕션급 애플리케이션에 권장)

B) 아니오 - 모든 SECURITY 규칙을 건너뜀 (PoC/프로토타입/실습성 프로젝트에 적합)

C) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 6: 복원력(Resiliency) 기준 확장

이 프로젝트에 복원력(Resiliency) 기준을 적용할까요?

이 확장의 의미: AWS Well-Architected Framework(Reliability Pillar)에서 파생된 설계 단계의 방향성 있는 모범 사례를 요구사항/설계/코드에 반영합니다 (장애 허용성, 고가용성, 관찰 가능성, 복구 가능성 등).

이 확장이 아닌 것: 프로덕션 준비 완료를 보장하거나 특정 가용성/RTO/RPO를 인증하지 않습니다. 정식 Well-Architected Review를 대체하지 않는 "출발점" 성격의 가이드입니다.

A) 예 - 복원력 기준을 방향성 있는 모범 사례/설계 가이드로 적용 (추후 검증·보강할 초안으로 활용)

B) 아니오 - 복원력 기준 적용을 건너뜀 (PoC/프로토타입/실습성 프로젝트에 적합)

C) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 7: 속성 기반 테스트(Property-Based Testing) 확장

이 프로젝트에 속성 기반 테스트(PBT) 규칙을 적용할까요?

A) 예 - 모든 PBT 규칙을 필수(blocking) 제약으로 적용 (비즈니스 로직/데이터 변환/직렬화/상태 관리가 있는 프로젝트에 권장)

B) 부분 적용 - 순수 함수와 직렬화 라운드트립에만 PBT 규칙 적용

C) 아니오 - 모든 PBT 규칙을 건너뜀 (단순 CRUD, UI 전용, 비즈니스 로직이 거의 없는 프로젝트에 적합)

D) Other (please describe after [Answer]: tag below)

[Answer]: A
