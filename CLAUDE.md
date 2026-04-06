# CLAUDE.md — Monorepo Root

This is the root of the label-manager monorepo.

## Structure

| Directory | Purpose |
|-----------|---------|
| `backend/` | Spring Boot REST API (Java). Also contains Thymeleaf templates and static JS — these are **temporary** and will be removed when the React migration is complete. |
| `frontend/` | React SPA. Grows as Thymeleaf pages are migrated. |
| `e2e/` | Playwright end-to-end tests. |
| `contracts/` | OpenAPI spec — the source of truth for the REST API contract between frontend and backend. |
| `docs/features/` | Per-feature documentation produced before implementation: business rules, user stories, acceptance criteria, UX flows, and screens. See `docs/features/README.md`. |
| `docs/ux/` | App-wide UX documentation. `app-flow.md` is the high-level navigation structure, domain model, screen inventory, and primary user flows — read this before designing any feature UX. |

Each directory has its own `CLAUDE.md` with context specific to that area:
- `backend/CLAUDE.md` — Spring Boot API conventions, testing, and build details
- `frontend/CLAUDE.md` — React SPA conventions and tooling
- `e2e/CLAUDE.md` — Playwright test conventions
- `contracts/CLAUDE.md` — OpenAPI spec conventions

Specialist agents are defined in `.claude/agents/` and invoked via slash commands in `.claude/commands/`:

| Command | Agent | When to use |
|---------|-------|-------------|
| `/ba feature=<name> description=<need>` | `ba.md` | Start BA work on a new feature |
| `/ux feature=<name>` | `ux.md` | After BA documents are complete |
| `/contract feature=<name>` | `contract.md` | After all six BA/UX documents are complete |
| `/frontend-architect feature=<name>` | `frontend-architect.md` | After BA, UX, and contract are done |
| `/frontend-developer feature=<name>` | `frontend-developer.md` | After frontend spec and tasks exist |
| `/frontend-reviewer feature=<name>` | `frontend-reviewer.md` | After all tasks are checked off |
| `/tester feature=<name>` | `tester.md` | After frontend and backend are both implemented |

## Feature Development Flow

New features follow this order. Steps 1–3 must be complete before any implementation begins.

1. **BA Agent** — fills `docs/features/<feature>/README.md` (context), `business-rules.md`, `user-stories.md`, `acceptance-criteria.md`
2. **UX Agent** — fills `ux-flows.md` and `screens.md`
3. **Contract Agent** — derives REST endpoints and schemas from the feature docs and writes them into `contracts/openapi.yaml`
4. **Frontend Agent** (`frontend/CLAUDE.md`) and **Backend Agent** (`backend/CLAUDE.md`) implement independently, consuming the feature docs and contract
5. **Tester Agent** — reads `acceptance-criteria.md` and decides whether Playwright e2e tests are needed; scopes scenarios if so
6. **E2E Agent** (`e2e/CLAUDE.md`) writes Playwright tests for the scenarios scoped by the Tester (only invoked if the Tester calls for it)

**Before starting as an agent in step 4 or 5**, verify prerequisites are in place:
- `docs/features/<feature>/` contains all six documents, all non-empty (no placeholder text):
  - `README.md`, `business-rules.md`, `user-stories.md`, `acceptance-criteria.md` (from BA Agent)
  - `ux-flows.md`, `screens.md` (from UX Agent)
- `contracts/openapi.yaml` includes the endpoints for this feature

Use `docs/features/_template/` as the starting point for each new feature (all six template files exist there).

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
