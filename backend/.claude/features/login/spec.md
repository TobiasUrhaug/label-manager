# Spec: login

## Status
Done

## Approach

The contract defines three endpoints for the React SPA migration:

| Endpoint | Method | Responsibility |
|----------|--------|----------------|
| `/login` | POST | Authenticate credentials, start session |
| `/logout` | POST | Invalidate session |
| `/api/session` | GET | Return the authenticated user's username |

Spring Security's form login already handles `POST /login` and `POST /logout` mechanically. The issue is that its default handlers **redirect** (302) on success and failure — unusable by a SPA which expects plain 200/401. The three changes required in `SecurityConfig` are:

1. **Custom `AuthenticationSuccessHandler`** — return `200 OK` (empty body) instead of redirecting to `/dashboard`.
2. **Custom `AuthenticationFailureHandler`** — return `401` with `ErrorResponse` JSON instead of redirecting to `/login?error`.
3. **Custom `LogoutSuccessHandler`** — return `200 OK` (empty body) instead of redirecting to `/login?logout`.

A fourth change covers unauthenticated access to `/api/**`: Spring Security's default `AuthenticationEntryPoint` redirects to `/login`, which is wrong for a REST API. We add a custom entry point that returns `401 JSON` for all `/api/**` requests.

The `GET /api/session` endpoint is new — a thin controller in the `identity` module that reads the authenticated `Principal` and returns `{ username }`.

### CSRF

Spring Security enables CSRF by default. During migration the Thymeleaf app (which includes explicit CSRF tokens in forms) and the React SPA coexist. To allow the SPA to obtain and send the CSRF token without a server-side render, switch to `CookieCsrfTokenRepository.withHttpOnlyFalse()`. This causes Spring Security to set an `XSRF-TOKEN` cookie that JavaScript can read. The SPA must include that value as the `X-XSRF-TOKEN` request header on all mutating requests (`POST /login`, `POST /logout`). Thymeleaf templates are unaffected — `${_csrf}` still resolves correctly against the cookie-backed repository.

### Module placement

`GET /api/session` belongs in `identity/api/user/` — it directly exposes the authenticated user's identity. The handler classes (`SpaAuthSuccessHandler`, etc.) belong in `infrastructure/security/` as they are cross-cutting security infrastructure.

## Files to Create or Modify

| File | Action | Purpose |
|------|--------|---------|
| `src/main/java/org/omt/labelmanager/infrastructure/security/SecurityConfig.java` | Modify | Wire custom handlers and switch CSRF repository |
| `src/main/java/org/omt/labelmanager/infrastructure/security/SpaAuthSuccessHandler.java` | Create | Return 200 on login success |
| `src/main/java/org/omt/labelmanager/infrastructure/security/SpaAuthFailureHandler.java` | Create | Return 401 + ErrorResponse on login failure |
| `src/main/java/org/omt/labelmanager/infrastructure/security/SpaLogoutSuccessHandler.java` | Create | Return 200 on logout success |
| `src/main/java/org/omt/labelmanager/identity/api/user/SessionController.java` | Create | `GET /api/session` endpoint |
| `src/test/java/org/omt/labelmanager/infrastructure/security/SpaLoginLogoutIT.java` | Create | Integration tests for login/logout response shapes |
| `src/test/java/org/omt/labelmanager/identity/api/user/SessionControllerTest.java` | Create | Unit tests for the session endpoint |

## Data Models / Interfaces

### `SessionResponse` (record, lives in `SessionController.java` as a private record or inner type — no separate file needed, it's trivial)

```java
record SessionResponse(String username) {}
```

### `ErrorResponse` (same shape used for all error bodies — reuse or inline)

```java
record ErrorResponse(String message) {}
```

Both are serialised to JSON by Jackson.

### Contract shapes (from `contracts/openapi.yaml`)

`POST /login`
- Request: `application/x-www-form-urlencoded` with `username` and `password`
- Success: `200 OK`, empty body, `JSESSIONID` cookie set
- Failure: `401`, `{ "message": "..." }`

`POST /logout`
- Success: `200 OK`, empty body
- Failure: `401` (handled by Spring Security before the handler runs)

`GET /api/session`
- Success: `200 OK`, `{ "username": "<email>" }`
- Unauthenticated: `401`, `{ "message": "..." }`

## Integration Points

| System/Module | How it's used | Who owns it |
|---------------|---------------|-------------|
| Spring Security | Form login, logout, CSRF, session management | `infrastructure/security/` |
| `AppUserDetails` | Carries the authenticated user's email as `getUsername()` | `identity/application/` |

## Assumptions

- `AppUserDetails.getUsername()` returns the user's email, which is what the contract calls `username` in `SessionResponse`.
- The Thymeleaf app continues to work during migration; no Thymeleaf templates are removed in this task.
- The Vite dev server proxies `/login`, `/logout`, and `/api/**` to `localhost:8080`; no CORS configuration is needed on the backend.

## Risks

- Switching to `CookieCsrfTokenRepository` may break existing `@WebMvcTest` tests that import `SecurityConfig` directly — they will need `csrf()` post-processors or the cookie token. Check `AuthControllerTest` after the change.

## Key Decisions

- **Separate handler classes over lambdas in `SecurityConfig`**: Keeps the config readable and makes the handlers independently testable.
- **`CookieCsrfTokenRepository` over disabling CSRF**: Retains protection for the Thymeleaf app and follows the standard Spring Security pattern for SPAs.
- **`SessionResponse` as an inner/local record in `SessionController`**: Avoids a separate file for a one-field DTO; promotes it to a standalone class only if other controllers need it.
- **Custom `AuthenticationEntryPoint` scoped to `/api/**`**: Thymeleaf pages outside `/api/**` still get the browser redirect to `/login`, which is correct for that rendering model.
