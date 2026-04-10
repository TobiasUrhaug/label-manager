# CLAUDE.md — Frontend

React SPA. The sole UI layer for the label-manager application.

## Key References

| Topic | File |
|-------|------|
| Architecture, directory structure, conventions, patterns | [ARCHITECTURE.md](ARCHITECTURE.md) |

## How to Run

```bash
npm install
npm run dev           # Dev server at http://localhost:5173
npm run build         # Production build
npm run test          # Unit tests
npm run lint          # ESLint
npm run lint:fix      # ESLint with auto-fix
npm run format:check  # Prettier check
npm run format        # Prettier auto-fix
```

## API Communication

The Vite dev server proxies `/api/*` to `http://localhost:8080` (the backend).
The backend must be running for API calls to work locally.

All API shapes are defined in `../contracts/openapi.yaml` — consult it before adding or changing API calls.

## Feature Workflow

Features are implemented by three agents in sequence. Prerequisites (BA, UX, contract)
must be complete before the architect starts — see root `CLAUDE.md` for the full flow.

```
frontend-architect  → frontend/.claude/features/<feature>/spec.md, tasks.md, index.md, progress.md
frontend-developer  → implements, checks off tasks, updates index.md + progress.md
frontend-reviewer   → comments.md, updates index.md + progress.md
frontend-developer  → addresses comments, updates progress.md
frontend-reviewer   → resolves comments → sets index.md status to Done
```

Agent definitions: `.claude/agents/frontend-architect.md`, `frontend-developer.md`, `frontend-reviewer.md`
Templates: `frontend/.claude/templates/`

