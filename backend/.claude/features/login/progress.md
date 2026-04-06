# Progress: login

## Current Phase
Implementation

## Last Completed Task
Task 1.4 — SpaApiAuthenticationEntryPoint created; unauthenticated GET /api/** → 401 JSON. AuthControllerTest verified passing.

## Next Action
Developer: start Task 2.1

## Blockers
None.

## Session Log
- 2026-04-04: Architect wrote spec.md and tasks.md. Feature docs from docs/features/login/ and contracts/openapi.yaml used as inputs. No Analyst phase needed — BA/UX docs already existed.
- 2026-04-06: Task 1.1 done. Added spring-boot-resttestclient dependency (Spring Boot 4 moved TestRestTemplate). Pulled CSRF cookie switch (task 1.4) forward — needed for testable login flow with TestRestTemplate. Used CsrfTokenRequestAttributeHandler (no XOR masking) for SPA compatibility. Handlers declared as @Bean methods in SecurityConfig rather than @Component to avoid @WebMvcTest context issues.
- 2026-04-06: Task 1.3 done.
- 2026-04-06: Task 1.4 done. DelegatingAuthenticationEntryPoint deprecated in Spring Boot 4; used a simple lambda dispatching on URI prefix instead. AuthControllerTest still passes. SpaLogoutSuccessHandler created and wired. Logout test required fetching a fresh XSRF-TOKEN after login (using the JSESSIONID) rather than relying on the login response token, because the token rotation after login wasn't being picked up by TestRestTemplate.
