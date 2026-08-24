# 보안 점검 결과 제출서

제출일시  : 2026-08-21
서비스명  : QuickChat
담당자    : 한상원 (hsw190701@lgcns.com)
점검 버전 : 0dfec59c88bcc3ff6ce9029d5fa2c29960ee403a (branch 12-frontend-code-generation, + 커밋되지 않은 작업 트리 변경분 포함 — 08-20 점검과 동일 커밋, H1/H2/M1 수정은 여전히 미커밋 상태)
배포 대상 : production
비고      : 이 제출서는 2026-08-20 점검(BLOCKED)의 후속 재점검입니다. 선행 문서: `.gstack/security-reports/cso-2026-08-20.md`

## 점검 결과 요약
Gate 결과  : **PASS** (2026-08-20: BLOCKED → 2026-08-21: PASS)
발견 항목  : CRITICAL 0건 / HIGH 1건 / MEDIUM 8건 / LOW 8건
OSS 이슈   : 0건

## 이전 점검 대비 변동
- **해결(Resolved) 3건**: H1(채널 join 가시성 검증 누락), H2(STOMP SUBSCRIBE 인가 부재), M1(WebSocket CORS 전면 허용) — 세 건 모두 이번 점검에서 코드를 직접 재확인해 실제로 수정되었음을 독립적으로 검증했습니다(단순히 담당자 보고를 신뢰한 것이 아님).
- **지속(Persisting) 17건**: H3(Spring Boot EOL), M2–M9, L1–L8. 전 항목 근거 파일을 다시 열어 현재 코드에도 동일한 결함이 남아있음을 재확인했습니다. L4(입력 검증)는 CreateChannelRequest에 `@NotBlank`/`@NotNull`이 추가되어 부분 개선되었으나 `@Size(max)` 및 STOMP 경로 검증 누락은 그대로입니다.
- **신규(New) 0건**: 08-20 이후 발견된 다른 코드 변경(RedisBroadcastListener/MessageResponse의 WebSocket 메시지 필드명 버그 수정)도 검토했으며, 정보 노출 등 새로운 보안 결함을 만들지 않음을 확인했습니다.

## 점검 소스 대상 수
점검대상 소스코드 파일 수 : 145개
실제 살펴본 소스코드 파일 수 : 27개 (이번 재점검 세션에서 신규로 열람한 파일 기준. 08-20에 이미 정독하고 이번에 변경 언급이 없는 파일은 재열람하지 않고 baseline 결과를 승계)
점검 범위 차이 사유 : 최초 감사가 아니라 baseline 대비 변경분 재검증이므로, H1/H2/M1 수정 검증 대상과 M2-M9/L1-L8 전 항목의 근거 파일을 위험 기반으로 재열람했습니다.

## 주요 발견 항목 (HIGH만 표시, CRITICAL 없음)

```
[HIGH] [7/10] H3 — Spring Boot 3.3.2 / Spring Framework 6.1.x 라인 OSS 보안 패치 지원 종료(EOL)
(backend/build.gradle:3)
OWASP  : A06 Vulnerable and Outdated Components
ISMS-P : 2.10.4 보안시스템 운영(패치관리), 2.11.2 취약점 점검 및 조치
CWE    : CWE-1104
상태   : PERSISTING (08-20 대비 변경 없음)
Fix    : Spring Boot를 유지보수 중인 라인(3.5.x 또는 4.x)으로 업그레이드
기한   : 배포 전 필수 (standard 게이트 차단 기준에는 미달하나 강한 권고)
```

standard 게이트 기준(HIGH 2건 이하)은 충족했으나, PII 및 사용자 간 채팅 콘텐츠를 다루는 서비스 특성상 strict 게이트(HIGH 0건 허용)를 적용하면 이 1건으로 여전히 BLOCKED입니다. 이 서비스에 어느 게이트 모드가 적절한지 정보보안팀 확인을 권고합니다.

## 조치 계획
- H3(Spring Boot EOL): 다음 스프린트 내 3.5.x 또는 4.x 업그레이드 착수 (기한: 배포 전 권고, 지연 시 별도 사유 기록)
- M2–M9, L1–L8: 각 항목 기한(MEDIUM 배포 후 2주, LOW 배포 후 1개월) 내 순차 조치. 상세 Fix는 `.gstack/security-reports/cso-2026-08-21.md` 4절 및 `cso-2026-08-20.md` 3절 참고.
- H1/H2/M1 수정분을 정식 커밋으로 확정하고, 그 커밋 해시로 최종 재점검 1회 수행 권고.

## 담당자 확인
[ ] 위 발견 항목을 확인하였으며, 조치 계획에 따라 수정하겠습니다.
담당자 서명 : _______________  날짜 : ___________

## 정보보안팀 확인
[ ] 점검 결과를 검토하였으며, 배포를 승인/반려합니다.
담당자 서명 : _______________  날짜 : ___________
