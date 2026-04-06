# Review Comments: login

## Status
Done

## Review Round 1

### 🔴 Must Fix

- [x] **[SpaLoginLogoutIT.java:95-108]** The logout test only asserts HTTP 200. It does not verify that the session is actually invalidated — there is no follow-up request using the old `JSESSIONID` that asserts a 401 response. If session invalidation were broken, the existing test would not catch it. Add a follow-up assertion:
  ```java
  // after logout, the old session must be rejected
  HttpHeaders staleHeaders = new HttpHeaders();
  staleHeaders.add(HttpHeaders.COOKIE, "JSESSIONID=" + jsessionId);
  ResponseEntity<String> staleResponse = restTemplate.exchange(
          "/api/session", HttpMethod.GET, new HttpEntity<>(staleHeaders), String.class);
  assertThat(staleResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  ```
  **Resolved.** `postLogout_whenAuthenticated_returns200AndInvalidatesSession()` in `SpaLoginLogoutSystemTest` includes the stale-session assertion at lines 110–114. ✅

### 🟡 Should Fix

- [x] **[SpaAuthFailureHandler.java:14, SpaApiAuthenticationEntryPoint.java:14]** Both classes declare an identical private `record ErrorResponse(String message) {}`. Duplicate definitions will drift silently if one is changed. Extract to a package-private class `ErrorResponse.java` in `infrastructure/security/` shared by both handlers.
  **Resolved.** `ErrorResponse.java` is a package-private record in `infrastructure/security/`; both handlers use it. ✅

- [x] **[SpaAuthFailureHandler.java:22, SpaApiAuthenticationEntryPoint.java:22]** `response.setContentType("application/json")` sets no charset. `HttpServletResponse.getWriter()` defaults to ISO-8859-1 unless the encoding is set explicitly. Use `"application/json;charset=UTF-8"` (or call `response.setCharacterEncoding("UTF-8")` before `getWriter()`) to guarantee correct encoding.
  **Resolved.** Both handlers now use `"application/json;charset=UTF-8"`. ✅

- [x] **[SpaLoginLogoutIT.java:1]** This class uses `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`, which is the **System Test** pattern per `TESTING.md`. The suffix `IT` is not a recognised convention in this project (the defined suffixes are `*IntegrationTest` and `*SystemTest`). Rename to `SpaLoginLogoutSystemTest.java`.
  **Resolved.** Renamed to `SpaLoginLogoutSystemTest.java`. ✅

- [x] **[SpaLoginLogoutIT.java:36-38]** `catch (Exception ignored)` in `createTestUser()` is too broad. A DB connectivity failure or any unexpected error during setup will be silently swallowed, causing the test to fail later with a misleading error. Narrow to the specific duplicate-user exception (e.g. `EmailAlreadyExistsException`).
  **Resolved.** Now catches `EmailAlreadyExistsException` only. ✅

### 🟢 Suggestions

- [x] **[SecurityConfig.java:63-73]** The `authenticationEntryPoint()` bean is an inline lambda while all other handlers are extracted to separate named classes. Extracting it to a `SpaAwareAuthenticationEntryPoint` class would make it independently testable and consistent with the established pattern.
  **Resolved.** `SpaAwareAuthenticationEntryPoint.java` created; `SecurityConfig` declares a typed `@Bean` returning it. ✅

- [x] **[SecurityConfigTest.java:37-40]** `securityFilterChain_isConfigured()` asserts only that the bean is non-null — a wiring smoke test with negligible value. Remove it or replace with a meaningful assertion (e.g. that an unauthenticated request to a protected resource returns 3xx or 401).
  **Resolved.** Test removed. ✅

- [x] **[SpaLoginLogoutIT.java:82]** `assertThat(response.getBody()).contains("message")` checks for the key string but not the value. Consider asserting the exact error message from the spec (`"Invalid username or password."`) to catch regressions if the message text changes.
  **Resolved.** Assertion now checks the full message value. ✅

### NFR Checks

- [x] **Password storage**: BCrypt via `PasswordEncoder` bean. ✅
- [x] **Auth failure message**: Generic "Invalid username or password." — does not reveal whether username or password was wrong. ✅
- [x] **CSRF**: `CookieCsrfTokenRepository.withHttpOnlyFalse()` lets the SPA read the `XSRF-TOKEN` cookie and send it as `X-XSRF-TOKEN`. `CsrfTokenRequestAttributeHandler` (no XOR masking) is a deliberate and documented trade-off for SPA compatibility, endorsed by Spring Security. ✅
- [x] **Session fixation**: Spring Security's default `changeSessionId()` strategy is active — session ID rotates on login. ✅
- [x] **Session invalidation on logout (mechanism)**: `SecurityContextLogoutHandler` (a `LogoutHandler`, not the `LogoutSuccessHandler`) is still invoked and invalidates the session server-side. The custom handler only replaces the redirect step. ✅
- [x] **Session invalidation on logout (test coverage)**: Verified by `postLogout_whenAuthenticated_returns200AndInvalidatesSession()` — stale `JSESSIONID` returns 401. ✅
- [x] **Information leakage**: Both 401 paths return generic messages with no internal detail or stack traces. ✅
- [x] **Path traversal in entry point**: `getRequestURI().startsWith("/api/")` — low risk; Spring Security's `StrictHttpFirewall` blocks path traversal before this code is reached. ✅
- [x] **`SessionController` — `Principal` injection**: If an unauthenticated request somehow reaches `session(Principal principal)`, Spring MVC will inject `null`, causing a `NullPointerException`. However, the `anyRequest().authenticated()` rule in `SecurityConfig` and the custom entry point ensure unauthenticated requests never reach the controller. Verified by `SessionControllerTest.unauthenticatedRequest_returns401()`. ✅

---

## Developer Responses (Round 1)

**🔴 [SpaLoginLogoutIT.java:95-108]** — Added the follow-up assertion to `postLogout_whenAuthenticated_returns200AndInvalidatesSession()`. After logout returns 200, a GET to `/api/session` using the stale `JSESSIONID` now asserts 401. Test renamed as part of the 🟡 rename below.

**🟡 [SpaAuthFailureHandler.java:14, SpaApiAuthenticationEntryPoint.java:14]** — Extracted `ErrorResponse` to a package-private `ErrorResponse.java` in `infrastructure/security/`. Both handlers now use the shared record; their local declarations are removed.

**🟡 [SpaAuthFailureHandler.java:22, SpaApiAuthenticationEntryPoint.java:22]** — Changed `"application/json"` to `"application/json;charset=UTF-8"` in both handlers.

**🟡 [SpaLoginLogoutIT.java:1]** — Renamed `SpaLoginLogoutIT.java` → `SpaLoginLogoutSystemTest.java` (deleted old file, created new).

**🟡 [SpaLoginLogoutIT.java:36-38]** — Narrowed `catch (Exception ignored)` to `catch (EmailAlreadyExistsException ignored)` in `createTestUser()`.

**🟢 [SecurityConfig.java:63-73]** — Extracted the inline lambda to `SpaAwareAuthenticationEntryPoint.java`. `SecurityConfig` now declares a typed `@Bean` returning `SpaAwareAuthenticationEntryPoint`.

**🟢 [SecurityConfigTest.java:37-40]** — Removed `securityFilterChain_isConfigured()`. The two remaining tests (`passwordEncoder_isBCrypt` and `passwordEncoder_encodeAndMatchWorks`) provide meaningful coverage.

**🟢 [SpaLoginLogoutIT.java:82]** — Changed `contains("message")` (key check) to `contains("Invalid username or password.")` (value check) in `postLogin_withWrongPassword_returns401WithErrorBody()`.
