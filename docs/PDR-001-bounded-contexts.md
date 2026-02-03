# PDR-001: Organize Code by Bounded Contexts

## Status

Accepted

## Context

As the application grows, we need a code organization strategy that scales and keeps related concepts together. The traditional approach of organizing by technical layer (controllers/, services/, repositories/) leads to shotgun surgery when adding features.

## Decision

Organize code by **bounded contexts** from Domain-Driven Design, with each context containing its own technical layers internally.

```
org.omt.labelmanager/
├── catalog/           # Labels, releases, artists, tracks
├── identity/          # Users, authentication
├── finance/           # Costs, VAT calculations
└── infrastructure/    # Cross-cutting concerns
```

Each bounded context follows the same internal structure:

```
catalog/
├── api/               # Controllers, forms
├── application/       # Use cases (CRUD handlers)
├── domain/            # Domain models, value objects, factories
└── infrastructure/    # JPA entities, repositories
```

## Rationale

- **Feature cohesion**: Adding a new catalog feature touches files in one package tree
- **Clear ownership**: Each context can evolve independently
- **Aligned with domain**: Package names reflect business concepts, not technical roles
- **Easier navigation**: Find all release-related code in one place

## Consequences

- Developers must understand which bounded context owns a concept
- Some cross-cutting concerns (security, storage) live in a shared `infrastructure` context
- New features require deciding which context they belong to (or creating a new one)
