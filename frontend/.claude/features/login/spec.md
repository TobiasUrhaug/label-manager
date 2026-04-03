# Spec: Login

## Status
Draft

## Approach

The login feature introduces authentication state into the React SPA. The approach has three parts:

1. **Session probe on startup.** On app load, `GET /api/session` is called to determine whether a valid session exists. The result is stored in a React Context (`AuthContext`) that is available to all routes. This is the single source of truth for auth state throughout the app.

2. **Route protection via a wrapper component.** A `RequireAuth` component wraps all routes that require authentication. If the session probe has not yet resolved, it renders nothing (or a minimal loader). If the user is not authenticated, it redirects to `/login`, storing the originally-requested path in router state so it can be used after a successful login. If authenticated, it renders the child route via `<Outlet>`.

3. **Login page as a standalone route outside `AppLayout`.** `/login` must not render inside the existing `AppLayout` (which shows the app nav bar). It renders in isolation: a centered card on a plain background, no navigation.

**Why `GET /api/session` rather than a client-side cookie check?** The `JSESSIONID` cookie is `HttpOnly` and not readable from JavaScript. The backend is the only party that can confirm whether a session is valid. The probe must happen before any protected route is rendered.

**Why React Context for auth state rather than TanStack Query?** Auth state is not server state in the TanStack Query sense — it does not need background re-fetching or caching across components. It is app-level state (is this user logged in or not) that changes infrequently and must be accessible throughout the tree. React Context is the right tool. The initial session probe _is_ an async fetch, but the result is stored in context, not in a query cache.

**Why `useMutation` for the login POST?** The login call (`POST /login`) is a write that produces a side-effect (sets a session cookie). `useMutation` gives clean loading and error state without boilerplate `useEffect`. On success, the mutation handler updates the `AuthContext` state and navigates forward.

---

## Components to Create or Modify

| File | Action | Purpose |
|------|--------|---------|
| `src/context/AuthContext.jsx` | Create | Provides `{ user, setUser, isLoading }` to the component tree; runs the `GET /api/session` probe on mount |
| `src/components/RequireAuth.jsx` | Create | Route-guard wrapper: redirects to `/login` (with `state.from`) if not authenticated; renders `<Outlet>` if authenticated |
| `src/pages/LoginPage.jsx` | Create | Full login screen: username + password form, inline error state, loading state, client-side empty-field validation |
| `src/api/auth.js` | Create | Thin fetch wrappers: `login(username, password)`, `logout()`, `getSession()` |
| `src/App.jsx` | Modify | Add `/login` route (outside `AppLayout`), wrap all existing routes with `RequireAuth` |
| `src/layouts/AppLayout.jsx` | Modify | Add a "Log out" button/link that calls `logout()` and navigates to `/login` |
| `src/main.jsx` | Modify | Wrap the app with `<AuthProvider>` |
| `src/context/AuthContext.jsx` | (test) | `src/context/AuthContext.test.jsx` |
| `src/components/RequireAuth.jsx` | (test) | `src/components/RequireAuth.test.jsx` |
| `src/pages/LoginPage.jsx` | (test) | `src/pages/LoginPage.test.jsx` |

---

## Routes

| Path | Component | Notes |
|------|-----------|-------|
| `/login` | `LoginPage` | Outside `AppLayout` and outside `RequireAuth`; if user is already authenticated, redirects to `/` (handled inside `LoginPage` using `AuthContext`) |
| `/` (and all other existing routes) | Wrapped by `RequireAuth` → `AppLayout` → page component | `RequireAuth` redirects to `/login` if not authenticated |

Route tree in `App.jsx` after this feature:

```jsx
<Routes>
  <Route path="/login" element={<LoginPage />} />
  <Route element={<RequireAuth />}>
    <Route element={<AppLayout />}>
      <Route path="/" element={<HomePage />} />
    </Route>
  </Route>
</Routes>
```

---

## Data Fetching

| Endpoint (from openapi.yaml) | operationId | Used by | Notes |
|------------------------------|-------------|---------|-------|
| `GET /api/session` | `getSession` | `AuthContext` (on mount) | Returns `SessionResponse { username: string }` on 200; 401 = not authenticated |
| `POST /login` | `login` | `LoginPage` via `useMutation` | `application/x-www-form-urlencoded`; 200 = success + session cookie set; 401 = wrong credentials |
| `POST /logout` | `logout` | `AppLayout` logout button | 200 = session invalidated; after success, navigate to `/login` |

### API response → state mapping

**`getSession` → `AuthContext` state**
- 200: `{ user: { username }, isLoading: false }`
- 401: `{ user: null, isLoading: false }`
- pending: `{ user: null, isLoading: true }`

**`login` (mutation)**
- 200: call `setUser({ username })` from `AuthContext`, navigate to `state.from ?? '/'`
- 401: set local `loginError = 'Invalid username or password.'`; clear both fields; refocus username input

**`logout` (mutation)**
- 200: call `setUser(null)` from `AuthContext`, navigate to `/login`

---

## Component Interfaces

### `AuthContext`

```js
// Provided value shape
{
  user: { username: string } | null,
  setUser: (user) => void,
  isLoading: boolean,   // true while GET /api/session is in flight
}
```

`AuthContext.jsx` exports:
- `AuthProvider` — wraps the app, runs `getSession()` on mount
- `useAuth` — convenience hook: `const { user, setUser, isLoading } = useAuth()`

### `RequireAuth`

No props. Reads `useAuth()`. Behaviour:
- `isLoading === true` → render `null` (prevents flash of redirect before session probe resolves)
- `user === null` → `<Navigate to="/login" state={{ from: location.pathname }} replace />`
- otherwise → `<Outlet />`

### `LoginPage`

No props. Local state:
```js
const [loginError, setLoginError] = useState(null);
```

Uses `useMutation` for the login POST. Uses `useAuth()` to check if already authenticated (redirect away if so). Uses `useLocation()` to read `state.from`.

Form uses native HTML5 `required` attributes for empty-field validation (no external form library). The submit handler runs `mutation.mutate()` only if both fields have values (though HTML5 `required` handles the UI).

### `AppLayout` logout action

Reads `useAuth()`. Uses `useMutation` for `logout()`. On success: `setUser(null)` + `navigate('/login')`.

---

## Integration Points

| Component/Module | How it's used |
|------------------|---------------|
| `src/main.jsx` | `<AuthProvider>` is added around `<App>` (inside `<BrowserRouter>` and `<QueryClientProvider>`) |
| `src/App.jsx` | Route tree restructured: `/login` added outside the guard; existing routes wrapped with `<RequireAuth>` |
| `src/layouts/AppLayout.jsx` | Logout action added; reads `useAuth().user` to display the current username (optional enhancement) |
| React Router `useNavigate`, `useLocation`, `Navigate` | Used in `LoginPage` and `RequireAuth` for redirects |
| TanStack Query `useMutation` | Used in `LoginPage` and `AppLayout` for `login` and `logout` mutations |

---

## Screen States

### `/login` — `LoginPage`

| State | Trigger | UI |
|-------|---------|----|
| Default | Page load (not authenticated) or redirect after logout | Both fields empty, no error, username autofocused, button enabled |
| Loading | Form submitted, awaiting server response | Submit button disabled; field values preserved |
| Error | Server returns 401 | Inline message "Invalid username or password." shown above fields; both fields cleared; focus returned to username |
| Already-authenticated redirect | `useAuth().user` is non-null when page renders | Immediately redirected to `/` — `LoginPage` content never shown |

Client-side empty-field validation (F-03): HTML5 `required` attributes show browser-native validation UI without sending a request. No network call is made.

---

## Assumptions

- The backend Spring Security endpoint at `POST /login` returns HTTP 200 on success and HTTP 401 on failure. The contract (`openapi.yaml`) confirms this.
- The backend endpoint at `GET /api/session` returns 200 with `{ username }` when a session is active, and 401 when it is not. The contract confirms this.
- `POST /login` accepts `application/x-www-form-urlencoded` with fields `username` and `password`. The contract confirms this (schema: `LoginRequest`).
- The `JSESSIONID` cookie is set automatically by the browser from the `Set-Cookie` response header — no JavaScript cookie manipulation is needed.
- The Vite dev-server proxy forwards both `/login` and `/logout` to the backend (not just `/api/*`). This must be verified in `vite.config.js` — if only `/api/*` is proxied, the proxy config needs updating.
- No existing frontend routes need authentication today other than `/`. The `RequireAuth` wrapper is added now so all future routes inherit protection automatically.

---

## Risks

- **Vite proxy scope:** The current proxy rule may only cover `/api/*`. `POST /login` and `POST /logout` are at root paths, not under `/api/`. The developer must confirm and extend the proxy config if needed.
- **Flash of unauthenticated content:** If `RequireAuth` renders child routes before the `getSession` probe resolves, protected content flashes briefly. Mitigated by rendering `null` while `isLoading === true`.
- **Browser-native validation appearance:** HTML5 `required` validation bubbles vary by browser. This is acceptable for the current iteration per the tech stack decisions (no form library).

---

## Key Decisions

- Decision: Use React Context (`AuthContext`) for auth state rather than TanStack Query.
  - Why: Auth state is global client state, not server cache state. It is read throughout the tree, changes rarely, and must be available synchronously after initial load. TanStack Query's caching model is designed for repeated, background-refreshable data — not for a single on-mount probe whose result drives routing.
  - Alternatives considered: TanStack Query `useQuery` for `getSession` — rejected because it would complicate the `RequireAuth` guard (need to inspect query state rather than context) and would require propagating the query result globally anyway.

- Decision: Use HTML5 `required` for empty-field validation rather than a form library.
  - Why: The architecture doc explicitly defers React Hook Form / Zod until forms justify them. A two-field login form does not justify the dependency.
  - Alternatives considered: Manual `useState` validation with custom error text — possible, but HTML5 handles the AC requirement with zero code and is consistent with the stack constraint.

- Decision: `/login` route is placed outside both `RequireAuth` and `AppLayout`.
  - Why: The login page must be accessible without a session, and it must not render the app nav bar. Nesting it inside either wrapper would break one of these requirements.
  - Alternatives considered: Rendering `AppLayout` conditionally on auth state — rejected because it tangles layout concerns with auth logic.

- Decision: Store the originally-requested URL in React Router location state (`state.from`) rather than `sessionStorage` or a query parameter.
  - Why: React Router location state is the idiomatic approach in React Router v7, requires no manual cleanup, and disappears when the user closes the tab (appropriate for a transient redirect target).
  - Alternatives considered: `sessionStorage` — more persistent, but adds cleanup responsibility and is unnecessary here.

---

## Open Questions

None. All business rules, acceptance criteria, UX flows, and API shapes are clear and internally consistent.
