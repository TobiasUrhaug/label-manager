# CLAUDE.md — Contracts

OpenAPI specification for the Label Manager REST API.

## File

`openapi.yaml` — the single source of truth for the contract between `frontend/` and `backend/`.

## Conventions

- **Spec-first**: add or update the endpoint in `openapi.yaml` before implementing it in the backend or consuming it in the frontend.
- Use `$ref` for shared schemas to avoid duplication.
- Paths follow REST conventions: `/api/labels`, `/api/labels/{id}`, etc.
- All endpoints require authentication (`cookieAuth` security scheme).
- Use `operationId` on every operation (e.g. `getLabel`, `createLabel`) — the frontend may use these for typed client generation later.

## Versioning

No versioning strategy yet. When breaking changes are needed, discuss before changing existing paths.
