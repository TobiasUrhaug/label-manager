# Tasks: login

## Status
In Progress

## Tasks

### 1. Custom Spring Security handlers

- [x] **1.1** Create `SpaAuthSuccessHandler` in `infrastructure/security/`
  - Implements `AuthenticationSuccessHandler`
  - Writes `200 OK` with empty body; does not redirect
  - Test in `SpaLoginLogoutIT`: `POST /login` with valid credentials → `200`; response has `Set-Cookie: JSESSIONID`

- [x] **1.2** Create `SpaAuthFailureHandler` in `infrastructure/security/`
  - Implements `AuthenticationFailureHandler`
  - Writes `401` with `Content-Type: application/json` and body `{ "message": "Invalid username or password." }`
  - Test in `SpaLoginLogoutIT`: `POST /login` with wrong password → `401`; body matches `ErrorResponse`

- [ ] **1.3** Create `SpaLogoutSuccessHandler` in `infrastructure/security/`
  - Implements `LogoutSuccessHandler`
  - Writes `200 OK` with empty body; does not redirect
  - Test in `SpaLoginLogoutIT`: authenticated `POST /logout` → `200`

- [ ] **1.4** Wire handlers into `SecurityConfig`
  - Replace `defaultSuccessUrl` with `.successHandler(spaAuthSuccessHandler())`
  - Replace `failureUrl` / default failure handling with `.failureHandler(spaAuthFailureHandler())`
  - Replace `logoutSuccessUrl` with `.logoutSuccessHandler(spaLogoutSuccessHandler())`
  - Switch CSRF token repository to `CookieCsrfTokenRepository.withHttpOnlyFalse()`
  - Add `DelegatingAuthenticationEntryPoint` (or a simple `RequestMatcher`-scoped entry point) that returns `401 JSON` for requests under `/api/**` while keeping the existing login-page redirect for browser requests
  - Verify `AuthControllerTest` (existing) still passes after the CSRF repo change

### 2. Session endpoint

- [ ] **2.1** Create `SessionController` in `identity/api/user/`
  - `GET /api/session` — reads `Principal` from `SecurityContext`, returns `200` with `SessionResponse { username }`
  - Uses a private record `SessionResponse(String username)` declared inside the controller file
  - Test in `SessionControllerTest` (`@WebMvcTest`):
    - Authenticated request → `200`, body `{ "username": "<value>" }`
    - Unauthenticated request → `401` (exercised by the entry point added in 1.4; assert via MockMvc without auth)

## Blockers
None.
