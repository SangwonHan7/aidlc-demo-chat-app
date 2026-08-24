# 보안 점검 결과 제출서

제출일시  : 2026-08-20
서비스명  : QuickChat
담당자    : 한상원 (hsw190701@lgcns.com)
점검 버전 : 0dfec59c88bcc3ff6ce9029d5fa2c29960ee403a (branch `12-frontend-code-generation` + 커밋되지 않은 작업 트리 변경분 포함 — 정확한 스냅샷이 아닌 하한선 참조)
배포 대상 : production (k3s, 단일 노드 NAS 데모 클러스터)
게이트 모드 : standard (CRITICAL 0 / HIGH 2 이하 허용) — *명시적으로 지정되지 않아 감사 실행자가 채택한 기본 가정. PII·채팅 콘텐츠를 다루는 점을 고려하면 strict 재검토를 권고.*

## 점검 결과 요약

Gate 결과  : **BLOCKED**
발견 항목  : CRITICAL 0건 / HIGH 3건 / MEDIUM 9건 / LOW 8건
OSS 이슈   : 0건
차단 사유  : HIGH 3건이 standard 게이트 허용 기준(2건 이하)을 1건 초과 (CRITICAL 0건은 기준 충족)

## 점검 소스 대상 수

점검대상 소스코드 파일 수 : 145개
실제 살펴본 소스코드 파일 수 : 50개
점검 범위 차이 사유 : 인증·인가·시크릿·메시징 흐름·인프라 노출 표면 중심의 위험 기반 표본 점검(상세 목록·사유는 `cso-2026-08-20.md` 14절 참고). 시크릿/위험 Sink/원시 SQL 패턴은 Grep으로 backend/src, frontend/src 전체를 보완 스캔.

## 주요 발견 항목 (CRITICAL/HIGH만 표시)

**[HIGH] [9/10] 채널 join API에 visibility 검증 누락 — INVITE_ONLY/DM 채널 무단 참여**
파일: `backend/src/main/java/com/quickchat/backend/service/ChannelService.java:73-78`
OWASP A01 Broken Access Control / CWE-862
인증된 임의 사용자가 채널 UUID만 알면 초대 없이 INVITE_ONLY 그룹 채널이나 1:1 DM에 스스로 참여해 이력 열람·메시지 전송 권한을 얻을 수 있습니다. Fix: `channel.getVisibility() != PUBLIC` 시 거부하는 검사 1줄 추가.

**[HIGH] [8/10] STOMP 구독(SUBSCRIBE) 경로에 멤버십 인가 검증 부재 — 임의 채널 실시간 도청**
파일: `backend/src/main/java/com/quickchat/backend/websocket/WebSocketConfig.java:28-32`, `StompAuthChannelInterceptor.java:44-68`
OWASP A01 Broken Access Control / CWE-862
유효한 JWT만 있으면(=계정만 있으면) 비멤버도 `/topic/channel/{channelId}`를 구독해 실시간 메시지를 흔적 없이 엿볼 수 있습니다. Fix: SUBSCRIBE 커맨드에 멤버십 검사를 추가하는 ChannelInterceptor 보완 또는 Spring Security WebSocket 메시지 인가 도입.

**[HIGH] [7/10] Spring Boot 3.3.2 / Spring Framework 6.1.x 라인 OSS 보안 패치 지원 종료(EOL)**
파일: `backend/build.gradle:3`
OWASP A06 Vulnerable and Outdated Components / CWE-1104
3.x 라인 전체가 OSS 무상 패치 지원을 종료해 신규 CVE가 나와도 무상 패치를 받을 수 없습니다. Fix: 유지보수 중인 라인(3.5.x 이상 또는 4.x)으로 업그레이드.

(MEDIUM 9건, LOW 8건 상세는 `cso-2026-08-20.md` 참고)

## 조치 계획

*(담당자 작성란 — 각 항목별 수정 방법 및 일정을 기입하십시오)*

- [ ] H1 조치 일정: ____________________
- [ ] H2 조치 일정: ____________________
- [ ] H3 조치 일정: ____________________
- [ ] BLOCKED 상태로 조건부 배포가 필요할 경우 `.gstack/security-reports/risk-acceptance-2026-08-20.md` 작성 후 정보보안팀 승인 절차 진행

## 담당자 확인

[ ] 위 발견 항목을 확인하였으며, 조치 계획에 따라 수정하겠습니다.
담당자 서명 : _______________  날짜 : ___________

## 정보보안팀 확인

[ ] 점검 결과를 검토하였으며, 배포를 승인/반려합니다.
담당자 서명 : _______________  날짜 : ___________
