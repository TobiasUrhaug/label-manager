# Tasks: Login

## Status
Draft

## Tasks

### Phase 1 — API layer

- [x] **1.1** Create `src/api/auth.js` with three functions: `getSession()`, `login(username, password)`, `logout()`.
  - `getSession`: `GET /api/session` — returns parsed JSON on 200, throws on non-200.
  - `login`: `POST /login` with `Content-Type: application/x-www-form-urlencoded`, body `username=...&password=...` — returns on 200, throws `{ status: 401 }` on 401.
  - `logout`: `POST /logout` — returns on 200, throws on non-200.
  - No test for this task (pure fetch wrappers; tested via component integration tests in later tasks).
  - Prerequisite: none.

- [x] **1.2** Verify that `vite.config.js` proxies `/login` and `/logout` (in addition to `/api/*`) to `http://localhost:8080`. If the proxy only covers `/api/*`, extend it.
  - No test (Vite proxy config is not unit-testable).
  - Prerequisite: 1.1.

### Phase 2 — Auth context

- [ ] **2.1** Write a failing test for `AuthContext` in `src/context/AuthContext.test.jsx`:
  - When `getSession` resolves with a user, `useAuth().user` equals `{ username }` and `isLoading` is false.
  - When `getSession` rejects (401), `useAuth().user` is null and `isLoading` is false.
  - While the probe is pending, `isLoading` is true.
  - Mock `src/api/auth.js` in the test (use `vi.mock`).
  - Prerequisite: 1.1.

- [ ] **2.2** Create `src/context/AuthContext.jsx` implementing `AuthProvider` and `useAuth` hook to make the tests from 2.1 pass.
  - `AuthProvider` calls `getSession()` on mount (plain `useEffect` + `fetch`, not TanStack Query).
  - Stores `{ user, setUser, isLoading }` in context.
  - Prerequisite: 2.1.

### Phase 3 — Route guard

- [ ] **3.1** Write a failing test for `RequireAuth` in `src/components/RequireAuth.test.jsx`:
  - When `isLoading` is true: renders nothing.
  - When `user` is null: redirects to `/login` and includes `state.from` equal to the current path.
  - When `user` is set: renders child content (via `<Outlet>`).
  - Wrap the component under test with `MemoryRouter` and a mock `AuthContext`.
  - Prerequisite: 2.2.

- [ ] **3.2** Create `src/components/RequireAuth.jsx` to make the tests from 3.1 pass.
  - Reads `useAuth()`.
  - Renders `null` while `isLoading`.
  - Renders `<Navigate to="/login" state={{ from: location.pathname }} replace />` when `user` is null.
  - Renders `<Outlet />` when authenticated.
  - Prerequisite: 3.1.

### Phase 4 — Login page

- [ ] **4.1** Write a failing test for `LoginPage` in `src/pages/LoginPage.test.jsx` covering:
  - Default state: username field, password field, and "Log in" button are rendered; username field has autofocus.
  - Already-authenticated: when `AuthContext` has a user, component renders a redirect (does not render the form). Use `MemoryRouter` with an initial entry of `/login`.
  - Prerequisite: 2.2.

- [ ] **4.2** Write a failing test for the loading state:
  - When the form is submitted, the "Log in" button becomes disabled while the mutation is pending.
  - Mock `src/api/auth.js` `login` to return a never-resolving promise.
  - Prerequisite: 4.1.

- [ ] **4.3** Write a failing test for the error state:
  - When `login` rejects with a 401-like error, the inline message "Invalid username or password." appears.
  - Both fields are cleared after the error.
  - Focus moves to the username field after the error.
  - Prerequisite: 4.2.

- [ ] **4.4** Create `src/pages/LoginPage.jsx` to make the tests from 4.1–4.3 pass.
  - Renders a centered card (Tailwind: full-height neutral background, card centered with `max-w-sm mx-auto`).
  - Application name at the top of the card.
  - Inline error message area between the name and the form (hidden when no error).
  - Username text input: label "Username", `required`, `autoFocus`.
  - Password input: label "Password", `required`, `type="password"`.
  - "Log in" submit button: full width, disabled while `mutation.isPending`.
  - On submit: calls `mutation.mutate({ username, password })`.
  - On mutation success: `setUser({ username })` + navigate to `location.state?.from ?? '/'`.
  - On mutation error: set `loginError`, clear both fields, focus username input.
  - If `useAuth().user` is already set on render: `<Navigate to="/" replace />`.
  - Prerequisite: 4.3.

### Phase 5 — Logout action in AppLayout

- [ ] **5.1** Write a failing test for the logout action in `src/layouts/AppLayout.test.jsx`:
  - A "Log out" button is rendered when a user is authenticated.
  - Clicking it calls `logout()` and navigates to `/login`.
  - Mock `src/api/auth.js` `logout` and `AuthContext` `setUser`.
  - Prerequisite: 2.2.

- [ ] **5.2** Modify `src/layouts/AppLayout.jsx` to make the test from 5.1 pass.
  - Add a "Log out" button (or link) to the nav bar.
  - Use `useMutation` for `logout()`.
  - On success: `setUser(null)` + `navigate('/login')`.
  - Prerequisite: 5.1.

### Phase 6 — Wire up routes

- [ ] **6.1** Modify `src/main.jsx` to wrap the app with `<AuthProvider>` (inside `<BrowserRouter>`, outside `<App>`).
  - No new test needed (provider mounting is covered by context tests in 2.1).
  - Prerequisite: 2.2.

- [ ] **6.2** Modify `src/App.jsx` to restructure the route tree:
  - Add `<Route path="/login" element={<LoginPage />} />` at the top level.
  - Wrap the existing `<Route element={<AppLayout />}>` block with `<Route element={<RequireAuth />}>`.
  - Write or extend a smoke test in `src/App.test.jsx`:
    - Unauthenticated user navigating to `/` is redirected to `/login`.
    - Unauthenticated user navigating to `/login` sees the login form (not redirected further).
    - Authenticated user navigating to `/login` is redirected to `/`.
  - Prerequisite: 3.2, 4.4, 6.1.

## Blockers

None.
