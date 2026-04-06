# Progress: login

## Current Phase
Implementation

## Last Completed Task
Task 1.2 — SpaAuthFailureHandler created and wired; POST /login with wrong password → 401 + JSON error body.

## Next Action
Developer: start Task 1.3

## Blockers
None.

## Session Log
- 2026-04-04: Architect wrote spec.md and tasks.md. Feature docs from docs/features/login/ and contracts/openapi.yaml used as inputs. No Analyst phase needed — BA/UX docs already existed.
- 2026-04-06: Task 1.1 done. Added spring-boot-resttestclient dependency (Spring Boot 4 moved TestRestTemplate). Pulled CSRF cookie switch (task 1.4) forward — needed for testable login flow with TestRestTemplate. Used CsrfTokenRequestAttributeHandler (no XOR masking) for SPA compatibility. Handlers declared as @Bean methods in SecurityConfig rather than @Component to avoid @WebMvcTest context issues.
