# CLAUDE.md — E2E Tests

Playwright end-to-end tests covering critical user journeys.

## Stack

- **Playwright** with Chromium, Firefox, and WebKit

## How to Run

```bash
npm install
npm run test          # Run all tests (headless)
npm run test:headed   # Run with visible browser
npm run test:ui       # Open Playwright UI
```

## Target URL

By default, tests run against `http://localhost:8080`.
The backend is started automatically by Playwright when targeting localhost.

To test against a different environment:
```bash
E2E_TARGET_URL=https://staging.example.com npm run test
```

## Test Files

| File | What it covers |
|------|----------------|
| `login.spec.js` | Login, logout, and auth redirect flows |

## Conventions

- Tests must be runnable against any environment via `E2E_TARGET_URL`
- Do not assert on implementation details — test user-visible behaviour
- Keep specs independent: each test sets up its own state
