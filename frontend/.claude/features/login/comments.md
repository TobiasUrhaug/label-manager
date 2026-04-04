# Review Comments: Login

## Status
In Review

## Review Round 1

### Must Fix
<!-- Missing acceptance criteria, broken behavior, wrong API usage, missing screen states -->

None.

### Should Fix
<!-- Error handling gaps, test coverage, accessibility, edge cases -->

- [ ] **`frontend/src/pages/LoginPage.test.jsx` — missing happy-path test**
  The test file covers default state, already-authenticated redirect, loading state, and error state, but there is no test for the successful login path. AC-01 and F-01 both require that on successful login `setUser` is called with the username and the user is navigated to `location.state?.from ?? '/'`. Add two tests to the `LoginPage` describe block:
  1. On successful login, `setUser` is called with `{ username }` and the user is navigated to `/` (the default target).
  2. On successful login when `location.state.from` is set, the user is navigated to that path rather than `/`.
  Mock `login` to resolve, mock `useAuth` to return a `setUser` spy, and use `MemoryRouter` with an initial state entry for the second case.

- [ ] **`frontend/src/App.test.jsx:48` — authenticated redirect test could be stronger**
  The test `'authenticated user navigating to /login is redirected to /'` only asserts that the Log in button is absent. It does not assert that the home page content is shown. This means the test would pass even if the redirect landed on a blank page. Add `expect(screen.getByText('Home page')).toBeInTheDocument()` (the stub route element is already defined in `renderApp`).

### Suggestions
<!-- Style, minor refactors, naming -->

- [ ] **`frontend/src/pages/LoginPage.jsx:19-21` — onSuccess closes over stale `username`**
  The `onSuccess` handler captures `username` from the outer `useState` closure. If a user submits, the mutation is inflight, and the component re-renders with a different value (unlikely for a login form, but possible), the wrong username could be stored in context. Consider passing the username through the mutation variables: `onSuccess: (_, variables) => { setUser({ username: variables.username }); ... }`. This is a minor robustness improvement, not a correctness issue for normal usage.

---

### Acceptance Criteria Check

- [x] **AC-01**: Successful login — `LoginPage.jsx` calls `setUser({ username })` on mutation success and navigates to `location.state?.from ?? '/'`. Route is wired in `App.jsx`. Pass.
- [x] **AC-02**: Failed login (wrong credentials) — mutation `onError` sets `loginError` to "Invalid username or password." (generic, does not reveal which field). Rendered via `role="alert"` paragraph. Pass.
- [x] **AC-03**: Failed login (empty fields) — both inputs have `required` attribute; HTML5 validation prevents submission without a network call. Pass.
- [x] **AC-04**: Logout — `AppLayout.jsx` renders a "Log out" button, uses `useMutation` for `logout()`, calls `setUser(null)` and `navigate('/login')` on success. Pass.
- [x] **AC-05**: Unauthenticated redirect — `RequireAuth.jsx` redirects to `/login` with `state.from` when `user` is null and `isLoading` is false. Wrapped around all routes in `App.jsx`. Pass.
- [x] **AC-06**: Already-authenticated bypass — `LoginPage.jsx` returns `<Navigate to="/" replace />` when `useAuth().user` is set. Pass.

### UX Compliance

- [x] **Default state**: Both fields empty, no error, username autofocused (`autoFocus` on input, `ref` available for programmatic focus), button enabled. Pass.
- [x] **Loading state**: Submit button has `disabled={mutation.isPending}`. Field values are preserved (controlled inputs). Pass.
- [x] **Error state**: Inline `role="alert"` paragraph above the form. Both fields cleared (`setUsername('')`, `setPassword('')`). Focus moved to username via `usernameRef.current?.focus()`. Pass.
- [x] **Already-authenticated redirect**: `<Navigate to="/" replace />` rendered before the form. Pass.
- [x] **F-01 (Successful Login)**: Login form → loading → navigate to `from ?? '/'`. Implemented. (Test gap noted in Should Fix above.)
- [x] **F-02 (Wrong Credentials)**: Error state wired to mutation `onError`. Pass.
- [x] **F-03 (Empty Fields)**: HTML5 `required` on both inputs. Pass.
- [x] **F-04 (Logout)**: AppLayout logout action implemented. Pass.
- [x] **F-05 (Redirect When Unauthenticated)**: RequireAuth stores `location.pathname` in `state.from`. Post-login `navigate(location.state?.from ?? '/')` uses it. Pass.
- [x] **F-06 (Already Authenticated)**: LoginPage redirects immediately. Pass.
- [x] **Login page renders outside AppLayout**: `/login` route is at the top level in `App.jsx`, outside both `RequireAuth` and `AppLayout`. Pass.

---

## Developer Responses (Round 1)

**Should Fix 1** — Added two tests to the `success state` describe block in `LoginPage.test.jsx`:
1. "calls setUser with { username } and navigates to / on successful login" — mocks `login` to resolve, passes a `setUser` spy via the auth value, asserts `setUser` was called with `{ username: 'alice' }` and that the Home page is rendered.
2. "navigates to location.state.from on successful login when from is set" — uses `MemoryRouter` with `initialEntries` containing `state: { from: '/labels' }`, asserts the Labels page is rendered after submission.
A third test was also added to cover the stale-closure robustness fix (see Suggestion response below).

**Should Fix 2** — Added `expect(screen.getByText('Manage your labels here.')).toBeInTheDocument()` to the authenticated redirect test in `App.test.jsx`. Note: the comment referenced `'Home page'` and mentioned "the stub route element is already defined in `renderApp`" — but `renderApp` uses the real `App` (with `HomePage`), not stubs. The real `HomePage` renders "Manage your labels here." which unambiguously confirms the redirect landed on the home page and the full route tree rendered.

**Suggestion** — Fixed the stale-closure issue in `LoginPage.jsx`. The `onSuccess` callback now uses `(_, variables) => { setUser({ username: variables.username }); ... }` so it reads the submitted username from mutation variables rather than the state closure. A Red test was written first to prove the bug (submitting as 'alice', changing field to 'bob' while in-flight, asserting `setUser` receives `'alice'`) — it failed with the old closure code and passes with the fix.

---

## Review Round 2 (if needed)
<!-- Reviewer adds new comments or follow-ups here -->
