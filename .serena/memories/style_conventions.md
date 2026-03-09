# Style & Conventions

- Max line length: 100 characters
- Long method signatures: each param on own line, 8-space indent
- Package-private by default (no public on internal classes)
- Domain objects are Java records
- Test naming: `shouldDoX_whenY()` or `methodName_conditionDescription()`
- Tests: behavior-focused, not implementation-focused (avoid excessive verify())
- Comments: explain "why", not "what"; Javadoc on public API
- Commit messages: `feat(scope): description`, `fix(scope): description`
- All JS in separate .js files, never inline in templates
- CSRF tokens required in all POST/PUT/DELETE forms
