# Architecture

The rules backend code must follow. Where code contradicts a rule here, the deviation is recorded
in [PDR-008](docs/PDR-008-architecture-remediation.md): fix the code or write a PDR — do not amend
this file to match what the code does.

Spring Boot 4.0.0, Java 25. Commands and workflow are in [CLAUDE.md](CLAUDE.md) and
[DEVELOPMENT.md](DEVELOPMENT.md); test strategy is in [TESTING.md](TESTING.md).

## Bounded contexts

| Context | Owns |
|---------|------|
| `catalog` | Labels, releases, artists, tracks |
| `identity` | Users, authentication |
| `finance` | Costs, VAT, invoice extraction |
| `distribution` | Distributors, pricing agreements |
| `inventory` | Production runs, allocations, inventory movements |
| `sales` | Sales, distributor returns |
| `dashboard` | Cross-context read model |
| `infrastructure` | Cross-cutting: security, storage, web |

Organize within a context by **module**, not by layer. Package names are lowercase with no
separators — `distributorreturn`, not `distributor_return`.

## Dependencies

These apply between modules and between contexts alike; where a rule is context-only it says so.

- **Import only `api/`.** Reach another module or context through its `api/` package and nothing
  else — never `application/`, `domain/`, or `persistence/`.
- **Never inject a repository you do not own.** Use cases inject repositories from their own module
  and `QueryApi`/`CommandApi` interfaces from others.
- **No cycles.** If A depends on B, B must not depend on A, directly or through a third party. A
  cycle is a missing construct, not a stubborn import: a shared type, a shared domain service, a
  read model, an event, or two modules that should be one.
- **`infrastructure` depends on no business context.** Business contexts depend on it; it does not
  depend back. A port declared there defines its own types rather than importing a business
  context's records to describe its return values.
- **`dashboard` is the one exception to the cycle rule.** It is the read model: downstream of every
  context, depended on by none. All cross-context response assembly belongs there.
- **Types used by more than one context go in a `shared/` context** that depends on nothing —
  `Money`, `ReleaseFormat`, `ChannelType`, `MovementType`. Importing whichever context happens to
  declare a type today creates a dependency unrelated to that context's behaviour.
- **Cross-context writes publish an event; they do not call.** The writing context publishes a
  domain event, the reacting context subscribes with `@TransactionalEventListener`. Calling another
  context's `CommandApi` couples the two write paths and is what produces cycles. The event record
  is contract, so it lives in the publishing context's `api/`. Use `phase = BEFORE_COMMIT` when the
  listener's write must share the publisher's transaction. Cross-context *reads* through `QueryApi`
  are fine.
- **Within a context, calling another module's `CommandApi` is correct** — that is how a module
  keeps its side effects encapsulated (see *Writing code*).

## Multi-tenant isolation

Every business entity belongs to exactly one user. Authentication is not authorization:
`SecurityConfig` proves who the caller is and nothing about what they may touch.

- **Every endpoint that accepts an entity ID verifies that entity belongs to the authenticated user
  before reading or mutating it.** Writes included — a cross-tenant `DELETE` is worse than a
  cross-tenant `GET` and is the easier one to leave untested.
- Taking `@AuthenticationPrincipal` is not the same as authorizing with it. A controller that holds
  the principal and never checks it looks correct at a glance and is not.
- A finder taking only a parent ID (`findByLabelId`) carries no tenant predicate. Either scope the
  finder by user, or prove ownership of the parent first.
- PDR-004 chose application-layer enforcement and PDR-008 P0 selects the single enforcement point.
  Until that lands, make the check explicit at every entry point.
- Each context needs an integration test proving a second user receives 404 from its **mutating**
  endpoints, not only its reads.

## Module structure

Use the **full** structure when a module has business logic beyond validation — orchestration
across repositories or external APIs, side effects, or domain rules. Use the **simplified**
structure when it is CRUD. Promote a simplified module to full when logic arrives.

Declare `*CommandApi` / `*QueryApi` in `api/` only when another module depends on this one.
Otherwise the controller injects the service directly; an unused public contract is one nobody is
holding you to.

| Where | What | Visibility |
|-------|------|------------|
| `api/` | `*CommandApi`, `*QueryApi`, controller, request/response records, views published to other modules, exceptions that cross the module boundary | public |
| `application/` — full only | `*UseCase`, `*CommandApiImpl`, `*QueryApiImpl` | package-private |
| `domain/` — full only | domain records | public |
| `persistence/` | JPA entities, Spring Data repositories | public — test helpers and infrastructure adapters need them |
| module root — full | `*Mapper` | public |
| module root — simplified | domain record, `*CommandService` / `*QueryService` | record public, services package-private |

`persistence/` is the JPA package name in both structures. `infrastructure` names the cross-cutting
context and is never a package inside a module.

A context may own a context-level `api/` package for views it publishes to other contexts that no
single module owns. Published contract only — it is not a home for types that failed to find a
module.

## Writing code

**Domain records** hold business rules, invariants, calculations, validation, and derived values.
Logging, exception handling, orchestration, and side effects stay in the application layer.

**Reference other modules by ID**, never by embedding their domain objects ([PDR-006](docs/PDR-006-jpa-modeling-with-ids.md)).

**Map entity to domain** with a public static `fromEntity()` on the domain record. When the mapping
needs inputs beyond the entity, put a `*Mapper` in the module root instead. A domain record
depending on its own module's persistence entity is a deliberate trade-off here — do not "fix" it.

**`*CommandApiImpl` only delegates** to use cases. `@Transactional` goes on the use case method, or
the `*CommandService` method in a simplified module — never on the delegating impl.

**Simple queries** — by ID, exists, by foreign key — live directly in `*QueryApiImpl`. Extract a
use case when a query carries business logic, spans repositories, or calls another module.

**An operation that always requires a side effect performs it** inside the module's `CommandApi`
implementation, so callers cannot forget it.

**Controllers assemble responses.** Fetch from each module's API separately and compose in the
controller — response shaping never belongs in a service or use case. A record another module would
construct or return is contract and belongs in `api/`; a record only this controller builds is
assembly, so nest it in the controller.

**Test helpers other modules need** are public, live in the test source tree, and may call
`fromEntity()`.

## Database

PostgreSQL in production and in tests (TestContainers). Flyway migrations live in
`src/main/resources/db/migration/`, named `V{n}__{description}.sql`.
