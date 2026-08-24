# Unit Test Execution - QuickChat

단위 테스트/속성 기반 테스트 자체는 Code Generation 단계에서 이미 작성되었다(Backend: `backend/src/test/**`, Frontend: `frontend/src/**/*.test.ts(x)` 및 `*.property.test.ts(x)`). 이 문서는 그것을 실제로 실행하는 방법을 정리한다.

## Run Unit Tests

### 1. Execute All Unit Tests

```bash
# Backend - JUnit5(예시 기반) + jqwik(속성 기반) 함께 실행 (build.gradle의 useJUnitPlatform 설정)
cd backend && ./gradlew test

# Frontend - Vitest(예시 기반) + fast-check(속성 기반) 함께 실행
cd frontend && npm test
```

### 2. Review Test Results

- **Expected**:
  - Backend: `backend/build/test-results/test/*.xml`, HTML 리포트 `backend/build/reports/tests/test/index.html`. 대략적인 테스트 파일 수 - 서비스 레이어(`AuthService`, `ChannelService`, `MessagingService`, `PresenceService` 등) 예시+속성 테스트, `web/*ControllerTest`(Auth/Presence/Channel/User), 신규 `RedisBroadcastListenerTest`, `StompAuthChannelInterceptorTest` - 전체 개수는 `./gradlew test`가 콘솔에 출력하는 합계를 확인
  - Frontend: `frontend/coverage/`(설정 시), 콘솔에 Vitest 리포트. 스토어(authStore/chatStore/presenceStore) + lib(backoff/validation) 각각 예시+속성 테스트 쌍, 컴포넌트(LoginForm/RegisterForm/MessageInput/PresenceIndicator/CreateChannelModal) 예시 테스트 + 폼 검증 속성 테스트 2종
- **Test Coverage**: 커버리지 수치 목표는 별도로 정하지 않았음(NFR Requirements에서 정량적 커버리지 기준 미설정, Property-Based Testing Full Enforcement로 핵심 로직의 "속성" 커버리지를 우선함) - `business-logic-summary.md`(Backend/Frontend 각각), `frontend-components-summary.md`의 PBT Compliance 표 참고
- **Test Report Location**: 위 경로. jqwik 실패 시 실패를 재현한 seed가 리포트에 함께 출력됨(jqwik 기본 shrinking) - fast-check도 동일하게 실패 시 최소 반례를 출력

### 3. Fix Failing Tests

If tests fail:
1. `backend/build/reports/tests/test/index.html` 또는 `npm test` 콘솔 출력에서 실패한 테스트 클래스/케이스 확인
2. jqwik/fast-check 속성 테스트가 실패하면 리포트에 출력된 축소된 반례(shrunk counterexample) 확인 - 대부분 `business-logic-model.md`/`business-logic-summary.md`에 문서화된 속성 정의 자체를 다시 확인해볼 것(구현이 아니라 속성 서술이 틀렸을 가능성도 있음)
3. 코드 수정 후 재실행

## 알려진 제약 (투명성 목적) - Build and Test 진입 전까지 실제 실행이 전혀 없었음

이번 세션에서는 셸 샌드박스 장애로 위 두 명령을 실제로 실행한 적이 없다. 다음은 실행 시 특히 주의해서 볼 것을 권장하는 지점(정적 검토로는 확인했지만 런타임 확인은 못한 것들):

- **Backend**: `MessagingServiceTest`/`MessagingServicePropertyTest`가 `new ObjectMapper()`를 직접 생성해 사용하는데(Spring이 자동 구성하는 빈이 아님), `JavaTimeModule`을 등록하지 않았다. 두 테스트는 `Instant` 필드를 실제로 직렬화하는 `broadcastMessage()`까지는 호출하지 않아 지금까지는 문제가 되지 않았지만(운영 코드의 `ObjectMapper` 빈은 `spring-boot-starter-web`이 전이적으로 포함하는 `jackson-datatype-jsr310`을 Spring Boot가 자동 등록해 정상 동작), 신규로 추가한 `RedisBroadcastListenerTest`는 `Instant`를 실제로 직렬화하므로 로컬에서 `new ObjectMapper().registerModule(new JavaTimeModule())`로 명시적으로 등록해두었다 - 실행해서 정말 통과하는지 첫 확인이 필요하다
- **Frontend**: RTL 예시 테스트/fast-check 속성 테스트(`LoginForm`/`RegisterForm` 관련)는 `fireEvent.click`으로 폼 제출을 트리거하는데, jsdom이 실제로 이 흐름에서 constraint validation으로 제출을 막지 않는다는 전제로 작성했다 - 이 전제 자체를 이번 실행에서 처음 확인하게 된다
- 두 유닛 모두, 이번 세션에서 작성된 테스트 파일들은 "작성"과 "실행 확인"이 분리되어 있다는 점을 명확히 인지하고 검토할 것
