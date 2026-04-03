# Frontend Architecture

## Stack

| Technology | Version | Role |
|------------|---------|------|
| React | 19 | UI component model |
| Vite | 6 | Dev server and bundler |
| Tailwind CSS | 4 | Styling |
| React Router | 7 | Client-side routing |
| TanStack Query | 5 | Server state (data fetching, caching, loading/error) |
| Vitest | 2 | Unit test runner |
| Testing Library | 16 | Component testing utilities |

## Why these choices

**React 19** — Concurrent rendering, server component foundations, and hooks are stable and mature.

**Vite** — Near-instant HMR, ES module-native build, simple config. Standard for React SPAs.

**Tailwind CSS v4** — Utility-first CSS. No runtime cost, no CSS naming collisions, co-locates visual intent with markup. V4 integrates directly as a Vite plugin — no config file, just `@import "tailwindcss"` in your CSS. Replaces the need for CSS modules.

**React Router v7** — Declarative client-side routing for the SPA. Wrap page components in `<Route>` trees; use layouts via nested routes and `<Outlet>`.

**TanStack Query v5** — Handles all server state: fetching, background re-fetching, caching, loading and error states. Avoids ad-hoc `useEffect`/`useState` fetch boilerplate that becomes unmanageable at scale. Use `useQuery` for reads and `useMutation` for writes.

**Not included (and why)**

- **TypeScript** — The project uses plain JSX. Migrating to TypeScript is a separate, deliberate decision; don't introduce it piecemeal.
- **Zustand / Redux** — React Context is sufficient for client-side UI state. Add a dedicated store only when global state grows complex.
- **React Hook Form / Zod** — Add when forms arrive; no speculative infrastructure.

## Directory structure

```
frontend/src/
  layouts/        # Shared layout wrappers (AppLayout, etc.)
  pages/          # Top-level page components, one per route
  components/     # Shared reusable components
  hooks/          # Custom hooks
  api/            # API call functions (thin wrappers around fetch)
  App.jsx         # Root route tree
  main.jsx        # Entry point: providers, global CSS
  index.css       # Tailwind import + global overrides
```

**Naming:**
- One component per file, filename matches export (`LabelList.jsx` → `export default function LabelList`)
- Co-locate tests: `LabelList.test.jsx` next to `LabelList.jsx`
- Pages go in `pages/`, layout shells in `layouts/`, everything else in `components/`

## Routing

Routes are defined in `App.jsx`. The pattern is nested routes: a layout route wraps all page routes via `<Outlet>`.

```jsx
<Routes>
  <Route element={<AppLayout />}>
    <Route path="/" element={<HomePage />} />
    <Route path="/labels" element={<LabelListPage />} />
  </Route>
</Routes>
```

Add a new page by:
1. Creating `src/pages/NewPage.jsx`
2. Adding a `<Route>` in `App.jsx`
3. Adding a `<NavLink>` in `AppLayout.jsx` if it belongs in the nav

## Data fetching

All API calls use the `/api` prefix, which Vite proxies to `http://localhost:8080` in development.

Thin fetch wrappers live in `src/api/`:

```js
// src/api/labels.js
export async function fetchLabels() {
  const res = await fetch('/api/labels');
  if (!res.ok) throw new Error('Failed to fetch labels');
  return res.json();
}
```

Components use TanStack Query to call them:

```jsx
const { data, isLoading, error } = useQuery({
  queryKey: ['labels'],
  queryFn: fetchLabels,
});
```

For mutations:

```jsx
const mutation = useMutation({
  mutationFn: createLabel,
  onSuccess: () => queryClient.invalidateQueries({ queryKey: ['labels'] }),
});
```

## Styling

Tailwind utility classes directly on JSX elements. No CSS modules, no inline styles.

```jsx
<button className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
  Save
</button>
```

Global overrides (if any) go in `src/index.css` below the `@import "tailwindcss"` line.

## Testing

- Unit tests with Vitest + Testing Library
- Test files co-located with source: `Component.test.jsx` next to `Component.jsx`
- Test setup in `src/test/setup.js`

```bash
npm run test        # Run all unit tests once
npm run test:watch  # Watch mode
```

E2E tests live in the root `e2e/` directory (Playwright).

## Provider setup

`main.jsx` mounts three providers in order:

1. `<BrowserRouter>` — React Router context
2. `<QueryClientProvider>` — TanStack Query context
3. `<App>` — route tree

When writing tests for components that use routing or queries, wrap the component under test with the relevant providers in the test file.
