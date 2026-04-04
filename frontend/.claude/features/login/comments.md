# Review Comments: Login

## Status
Done

## Review Round 1

### Must Fix
<!-- Missing acceptance criteria, broken behavior, wrong API usage, missing screen states -->

None.

### Should Fix
<!-- Error handling gaps, test coverage, accessibility, edge cases -->

- [x] **`frontend/src/pages/LoginPage.test.jsx` — missing happy-path test**
  Resolved. Three tests added in the `success state` describe block (lines 84–142):
  1. `setUser` called with `{ username: 'alice' }` and navigates to `/` on success.
  2. Stale-closure robustness — submits as 'alice', changes field to 'bob' while mutation is in-flight, asserts `setUser` received `{ username: 'alice' }`.
  3. Navigates to `location.state.from` when set. All tests use correct TL user-centric queries and `findBy*` for async assertions.

- [x] **`frontend/src/App.test.jsx:48` — authenticated redirect test could be stronger**
  Resolved. Line 50 now asserts `expect(screen.getByText('Manage your labels here.')).toBeInTheDocument()`, confirming the redirect lands on the real home page and the full route tree rendered correctly.

### Suggestions
<!-- Style, minor refactors, naming -->

- [x] **`frontend/src/pages/LoginPage.jsx:19-21` — onSuccess closes over stale `username`**
  Resolved. `onSuccess` now uses `(_, variables) => { setUser({ username: variables.username }); ... }` (LoginPage.jsx line 19). The stale-closure test at LoginPage.test.jsx lines 98–118 proves correctness.

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
- [x] **F-01 (Successful Login)**: Login form → loading → navigate to `from ?? '/'`. Implemented and tested. Pass.
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

## Review Round 2

All Round 1 comments verified resolved against source files. No new issues found.

- [x] **Should Fix 1** — Resolved. Three tests confirmed in `LoginPage.test.jsx` lines 84–142. Correct queries, correct async patterns, all three paths covered.
- [x] **Should Fix 2** — Resolved. `App.test.jsx` line 50 asserts home page content is rendered after authenticated redirect.
- [x] **Suggestion** — Resolved. `LoginPage.jsx` line 19 uses `variables.username` in `onSuccess`. Stale-closure test at `LoginPage.test.jsx` lines 98–118 backs the fix.

No new Must Fix, Should Fix, or Suggestion items.
