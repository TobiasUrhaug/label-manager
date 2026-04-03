# Review Comments: Login

## Status
In Review

## Review Round 1

### Must Fix
<!-- Missing acceptance criteria, broken behavior, wrong API usage, missing screen states -->
- [ ] **[File:Line]** Description of issue.

### Should Fix
<!-- Error handling gaps, test coverage, accessibility, edge cases -->
- [ ] **[File:Line]** Description of issue.

### Suggestions
<!-- Style, minor refactors, naming -->
- [ ] **[File:Line]** Description of suggestion.

### Acceptance Criteria Check
<!-- Verify each AC from acceptance-criteria.md -->
- [ ] AC-01: Successful login → redirected to `/` (or `state.from`)
- [ ] AC-02: Failed login with wrong credentials → error message shown, neither field named
- [ ] AC-03: Failed login with empty fields → client-side validation fires, no network request
- [ ] AC-04: Logout → session invalidated, redirected to `/login`
- [ ] AC-05: Unauthenticated navigation to protected route → redirected to `/login`
- [ ] AC-06: Already-authenticated user visits `/login` → redirected to `/`

### UX Compliance
<!-- Verify screen states and flows from ux-flows.md and screens.md -->
- [ ] All screen states implemented (Default, Loading, Error, already-authenticated redirect)?
- [ ] User flows match ux-flows.md (F-01 through F-06)?

---

## Developer Responses (Round 1)
<!-- Developer adds responses here; Reviewer resolves above -->

---

## Review Round 2 (if needed)
<!-- Reviewer adds new comments or follow-ups here -->
