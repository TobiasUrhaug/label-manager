# CLAUDE.md — Frontend

React SPA. Introduced as part of the gradual migration away from Thymeleaf.

## Stack

- **React 19** with JSX
- **Vite** for dev server and bundling
- **Vitest** + **Testing Library** for unit tests

## How to Run

```bash
npm install
npm run dev       # Dev server at http://localhost:5173
npm run build     # Production build
npm run test      # Unit tests
```

## API Communication

The Vite dev server proxies `/api/*` to `http://localhost:8080` (the backend).
The backend must be running for API calls to work locally.

All API shapes are defined in `../contracts/openapi.yaml` — consult it before adding or changing API calls.

## Conventions

- One component per file, named to match the file (e.g. `LabelList.jsx` exports `LabelList`)
- Co-locate tests: `src/components/LabelList.test.jsx` alongside `src/components/LabelList.jsx`
- No inline styles — use CSS modules or a utility-first approach (TBD when styling is introduced)
- Fetch data with plain `fetch` using the `/api` prefix; add an abstraction layer only when the pattern repeats

## Migration Notes

Pages are added here as they are migrated from Thymeleaf.
Each migrated page should have a corresponding REST endpoint in the backend and an entry in `../contracts/openapi.yaml`.
