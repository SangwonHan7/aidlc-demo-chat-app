# Security Test Instructions & Results - QuickChat

## Purpose
운영 배포 전 취약점 점검 게이트. 이 프로젝트는 `source-code-security-check`(v4.0.2) 스킬을 이용해 이미 1회 전체 점검을 실행했다 - 이 문서는 그 결과 요약 + 재실행 방법이다.

## 실행 결과 (2026-08-20)

- **전체 보고서**: `.gstack/security-reports/cso-2026-08-20.md`
- **제출용 JSON**: `.gstack/security-reports/cso-2026-08-20-summary.json`
- **정보보안팀 제출서**: `.gstack/security-reports/cso-2026-08-20-submit.md` (서명란 미작성)
- **위험수용서**: `.gstack/security-reports/risk-acceptance-2026-08-20.md` (BLOCKED로 자동 생성됨)
- **SBOM**: `.gstack/sbom/sbom-2026-08-20.cdx.json` (CycloneDX 1.5, 직접 의존성 44개), `.gstack/sbom/sbom-2026-08-20.spdx`

**최초 판정**: BLOCKED (게이트 모드 standard, CRITICAL 0 / **HIGH 3**건 - 허용치 2건 초과 / MEDIUM 9 / LOW 8)

**가장 중요한 발견 2건은 이미 수정 완료** (같은 날, 이 보고서 제출 직후):
1. H1 - `ChannelService.joinChannel()`이 채널 가시성을 확인하지 않아 INVITE_ONLY/DM 채널에 초대 없이 무단 참여 가능 → 수정 완료
2. H2 - STOMP SUBSCRIBE에 멤버십 인가가 전혀 없어 임의 채널 실시간 도청 가능 → 수정 완료
3. (MEDIUM) M1 - WebSocket CORS 전면 허용 → REST와 동일한 단일 origin으로 제한 완료

상세 수정 내역은 `.gstack/security-reports/cso-2026-08-20.md`의 "사후 조치 업데이트" 인용 블록과 `aidlc-docs/construction/backend/code/api-layer-summary.md`의 "Post-Approval Patch 6" 참고.

**아직 열려 있는 항목** (코드 수정이 아니거나, 이번 라운드에서 판단을 보류한 것들):
- H3: Spring Boot 3.3.2 / Spring Framework 6.1.x OSS 패치 지원 종료(EOL) - 버전 업그레이드 계획 필요(코드 한 줄 수정이 아니라 회귀테스트 전체 재확인이 따라오는 작업이라 별도 계획으로 분리)
- M2: `GET /api/users/search`, `GET /api/users?ids=` 관계 기반 인가 누락 - 이미 aidlc-docs에 "데모 단계의 알려진 제약"으로 여러 번 문서화된 항목을 이번에 정식 등급(MEDIUM)으로 재확인한 것. 데모 목적상 완전한 관계 검증 도입은 범위 밖으로 판단해 이번 라운드에서는 미수정 - 최소 rate limit 추가를 권고안으로 남김
- M3-M9, L1-L8: 의존성 버전(axios/Next.js), lock 파일 부재, localStorage 토큰 저장, Docker root 실행/devDependencies 포함, docker-compose 평문 자격증명, 계정 잠금 표적형 DoS, Swagger 무인증 공개, 이메일 열거, 대소문자 정규화, 입력 길이 검증, rate limiter 버스트, k3s 매니페스트 부재 등 - 전부 `.gstack/security-reports/cso-2026-08-20.md`에 CWE/OWASP/ISMS-P 매핑과 구체적 Fix 포함해 기록됨. 담당자(한상원) 검토 및 조치 계획 수립 필요

## 재실행 방법

수정 사항을 반영해 게이트를 다시 판정받으려면:

```
/cso --service="QuickChat" --owner="한상원" --target=production --gate=standard
```

(`source-code-security-check` 스킬을 다시 호출하는 것과 동일 - Claude에게 "보안 점검 다시 실행해줘"로 요청해도 됨). H1/H2/M1이 수정되었으므로 남는 HIGH는 H3 1건뿐일 것으로 예상되며, standard 게이트(HIGH ≤2)는 통과(PASS)할 가능성이 높다 - 다만 이는 추정이며 실제 재실행으로 확정해야 한다(`.gstack/security-reports/cso-2026-08-20-summary.json`의 `remediation_addendum.recalculated_gate_estimate` 참고).

## Deploy Gate 최종 판단 기준
- **strict**로 재평가할지 여부: 이 앱은 이메일(PII)과 사용자 간 채팅 콘텐츠를 다루므로, 원 보고서는 standard보다 strict(HIGH 0건 허용)가 더 보수적인 선택일 수 있다고 명시했다 - 실제 운영 전환 시 담당자/정보보안팀이 게이트 모드를 재확인할 것을 권고
- M2(사용자 조회 API)는 strict 기준에서는 별도 검토가 필요할 수 있음

## 알려진 제약 (투명성 목적)
- 이 점검은 `git log`/`npm audit`/`./gradlew dependencies`/실제 동적 침투 테스트 없이, 정적 코드 열람(Read/Grep/Glob) + WebSearch(CVE 조회)만으로 수행되었다 - 145개 대상 파일 중 50개를 위험 기반으로 정독했고, 나머지도 시크릿/XSS/SQL 패턴은 전체 grep으로 보완 확인했다(상세 근거는 `cso-2026-08-20-summary.json`의 `source_review_scope`)
- k3s 매니페스트가 저장소에 아직 없어(L6) 실제 운영 인프라의 Pod 보안 설정/NetworkPolicy 등은 이번 점검 범위 밖이다 - Infra 유닛 마무리 단계에서 매니페스트 작성 후 재점검 필요
