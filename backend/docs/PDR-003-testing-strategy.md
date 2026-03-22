# PDR-003: Testing Pyramid with Clear Layer Responsibilities

## Status

Accepted

## Context

Without a clear testing strategy, teams often duplicate assertions across test levels (testing the same validation in unit, integration, and E2E tests) or leave gaps. We need a strategy that maximizes confidence while minimizing redundancy.

## Decision

Follow a strict **testing pyramid** where each layer owns specific responsibilities:

```
      /\        E2E (Playwright)        - Smoke tests only
     /  \       System (SpringBootTest) - Full API flows
    /    \      Integration             - Database, external services
   /      \     Controller (WebMvcTest) - HTTP routing, views
  /________\    Unit (JUnit/Vitest)     - Business logic
```

### Layer Responsibilities

| Layer | What to Test | What NOT to Test |
|-------|--------------|------------------|
| **Unit** | Domain logic, calculations, mappers | Anything requiring Spring context |
| **Controller** | HTTP mappings, view names, form validation, model attributes | Business logic (mock handlers) |
| **Integration** | Repository queries, JPA mappings, transactions, service orchestration | HTTP layer |
| **System** | Critical API flows end-to-end, security, serialization | Every edge case |
| **E2E** | App loads, login works, critical journey | API contracts, validation rules |

### Naming Conventions

- `*Test.java` - Unit tests
- `*ControllerTest.java` - Controller slice tests
- `*IntegrationTest.java` - Integration tests
- `*SystemTest.java` - Full-stack API tests
- `*.spec.js` - Playwright E2E tests

## Rationale

- **No duplication**: Each assertion exists at exactly one level
- **Fast feedback**: Most tests are unit tests (milliseconds)
- **Clear debugging**: When a test fails, you know which layer broke
- **Maintainable**: Changing a validation rule updates one test, not five

## Consequences

- Requires discipline to put tests in the right layer
- New developers need to understand the pyramid
- Some edge cases only tested at lower levels (by design)
- Controller tests require mocking handlers via `@MockitoBean`
