# CLAUDE.md — Monorepo Root

This is the root of the label-manager monorepo.

## Structure

| Directory | Purpose |
|-----------|---------|
| `backend/` | Spring Boot REST API (Java). Also contains Thymeleaf templates and static JS — these are **temporary** and will be removed when the React migration is complete. |
| `frontend/` | React SPA. Grows as Thymeleaf pages are migrated. |
| `e2e/` | Playwright end-to-end tests. |
| `contracts/` | OpenAPI spec — the source of truth for the REST API contract between frontend and backend. |

Each directory has its own `CLAUDE.md` with context specific to that area.

## Migration Strategy

The app is migrating from Thymeleaf (server-side rendered) to a React SPA backed by a REST API.

- During migration, a page may exist in **both** Thymeleaf and React simultaneously.
- The OpenAPI spec in `contracts/openapi.yaml` must be updated whenever a new REST endpoint is introduced.
- When all pages have been migrated, everything Thymeleaf-related in `backend/` is deleted in one cleanup pass:
  - `src/main/resources/templates/`
  - `src/main/resources/static/js/`
  - `src/test/js/`
  - `backend/package.json`, `backend/vitest.config.js`
  - Thymeleaf and Bootstrap dependencies in `build.gradle.kts`

## Common Commands

```bash
make build          # Build backend
make test           # Backend Java tests
make test-js        # JS unit tests for Thymeleaf static JS (temporary)
make test-e2e       # Playwright e2e tests
make start-backend  # Run backend dev server (port 8080)
make start-frontend # Run frontend dev server (port 5173)
make install        # Install all npm dependencies
```

## Git Workflow

- Feature branches: `feature/<FEATURE_NAME>`
- Merge to `main` via pull request
- Always show the proposed commit message and ask for confirmation before committing
