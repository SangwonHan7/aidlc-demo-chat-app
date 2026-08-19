# NFR Design Plan - Frontend Unit

## Plan

- [x] nfr-requirements.md 기반으로 회복성/확장성/성능/보안 패턴 설계
- [x] 아래 질문 답변 반영
- [x] nfr-design-patterns.md 작성
- [x] logical-components.md 작성 (배포 토폴로지/CORS, 정적 자산 캐싱 등)

## 후속 조치
Question 4(B) 반영을 위해 이미 승인된 Backend 코드에 CORS 설정을 보완 - api-layer-summary.md "Post-Approval Patch 2" 참고.

## Questions

각 질문에 알파벳으로 답한 뒤 채팅으로 "답변 완료"라고 알려주세요.

## Question 1 (Resilience Patterns)

REST API 호출이 일시적으로 실패하면 자동 재시도를 할까요?

A) 조회성(GET) 요청만 짧은 지연 후 1회 자동 재시도, 그래도 실패하면 인라인 에러+재시도 버튼(business-rules.md 에러 표시 정책과 연계) - 부수효과가 있는 요청(POST/DELETE, 채널 생성/초대/제외 등)은 자동 재시도하지 않아 중복 생성/중복 초대를 방지

B) 모든 요청에 자동 재시도 없음 - 실패 시 즉시 인라인 에러+재시도 버튼만 표시 (가장 단순)

C) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 2 (Scalability Patterns)

Frontend 파드 오토스케일링을 적용할까요? (Backend는 NAS 리소스 제약으로 미적용, 고정 레플리카로 결정했습니다)

A) 미적용 - Backend와 동일하게 고정 레플리카 수로 운영

B) 적용 - CPU/요청량 기준 HPA 설정

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3 (Performance Patterns)

메시지 목록(Story 1.5, 무한 스크롤)이 한 채널에서 수천 개 이상으로 누적될 경우를 대비해 가상화(virtualization)를 적용할까요?

A) 적용 - `react-window` 등으로 화면에 보이는 부분만 렌더링, 이력이 많아져도 성능 저하 없음

B) 미적용 - 실습 데모 규모(수십~수백 개)에서는 자연스러운 DOM 증가로 충분, 추가 의존성 없이 단순화

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 4 (Security Patterns / Logical Components)

Backend 코드를 확인한 결과 `WebSocketConfig`는 STOMP 엔드포인트에 `setAllowedOriginPatterns("*")`로 모든 origin을 허용하고 있지만, REST API를 보호하는 `SecurityConfig`에는 CORS 설정이 전혀 없습니다. Frontend를 Backend와 다른 origin(다른 도메인/포트)에 배포하면 REST 호출이 브라우저에서 차단됩니다. 어떻게 배치할까요?

A) 같은 origin으로 배포 - nginx-ingress에서 경로 기반 라우팅(`/`는 Frontend, `/api`·`/ws`는 Backend)으로 하나의 도메인 아래 배치. CORS 자체가 불필요해짐 (Backend 코드 변경 없음)

B) 다른 origin으로 배포 - Backend `SecurityConfig`에 명시적 CORS 설정(허용 origin을 Frontend 도메인으로 지정, credentials 허용)을 추가. 이미 승인된 Backend 코드에 소규모 보완이 필요함

C) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 5 (Logical Components)

정적 자산(JS/CSS 번들) 캐싱 전략은 어떻게 할까요?

A) Next.js 기본 캐싱 헤더 그대로 사용 - 별도 CDN/추가 설정 없음, 온프레미스 NAS 환경에 맞게 단순화

B) nginx-ingress에서 정적 자산에 장기 캐싱 헤더(`Cache-Control: max-age=...`)를 별도로 설정

C) Other (please describe after [Answer]: tag below)

[Answer]: A
