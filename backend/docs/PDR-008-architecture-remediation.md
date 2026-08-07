# PDR-008: Architecture Remediation — Enforce the Modular Monolith

## Status

Proposed

## Context

PDR-001 established bounded contexts, ARCHITECTURE.md defined the module structure and
encapsulation rules, and PDR-004 chose application-layer multi-tenant isolation. None of these
rules are enforced by a build step. An audit of all 287 backend source files against the
documented architecture found the following drift.

### 1. Multi-tenant isolation is not implemented

PDR-004 selected "enforce in the application layer" and no enforcement point was built.

- 8 of 12 business controllers never take `@AuthenticationPrincipal`: `SaleController`,
  `CostController`, `ProductionRunController`, `AllocateController`, `DistributorController`,
  `AgreementController`, `ReturnController`, `InvoiceExtractionController`.
- Controllers that do take it do not authorize with it. `LabelController#label`
  (`catalog/label/api/LabelController.java:105`) loads any label by id and uses `user` only to
  list artists.
- Only `LabelRepository` and `ArtistRepository` have a `findByUserId`. Every repository below
  the top level queries by parent id (`findByLabelId`, `findByReleaseId`,
  `findByOwnerOwnerTypeAndOwnerOwnerId`) with no tenant predicate.

`SecurityConfig:24` is `.anyRequest().authenticated()` and nothing more, so this covers writes as
well as reads: `SaleController` maps `/api/labels/{labelId}/sales` with GET, POST, PUT and DELETE
and never sees the principal. Any authenticated user can read and mutate another tenant's data by
guessing IDs.

### 2. Bounded contexts form a cycle

ARCHITECTURE.md forbids bidirectional module dependencies. Six context pairs violate it:

```
catalog ↔ sales        catalog ↔ inventory      catalog ↔ distribution
catalog ↔ finance      distribution ↔ sales     finance ↔ infrastructure
```

The last pair is the worst of the six: `infrastructure` is the cross-cutting context, so a business
context and the shared foundation depend on each other (see §3, `DocumentStoragePort`).

Primary driver: `catalog/release/api/ReleaseController.java` (344 lines) imports from
`inventory`, `sales`, `finance` and `distribution` to assemble one detail response.

### 3. Encapsulation leaks

| Location | Violation |
|----------|-----------|
| `finance/cost/application/RegisterCostUseCase.java:16` | Injects `identity…UserRepository` — cross-context repository injection |
| `catalog`, `dashboard` (4 imports) | Import `identity.application.AppUserDetails` — reaches into another context's application layer |
| `infrastructure/storage` | Imports `finance.shared.RetrievedDocument` — cross-cutting context depends on a business context |
| `sales`, `inventory`, `catalog` | `ReleaseFormat` (22 importing files, 16 of them outside `catalog`), `ChannelType`, `MovementType`, `Money` used as a shared kernel that does not exist |

### 4. Structure drift from the documented pattern

- Two names for the JPA layer: `catalog`, `finance`, `sales` use `infrastructure/`;
  `distribution`, `inventory` use `persistence/`. ARCHITECTURE.md specifies `persistence/`.
- Four "shared" locations: `catalog/domain/shared`, `catalog/infrastructure/persistence/shared`,
  `finance/domain/shared`, `finance/shared`, plus loose enums at `inventory/` root.
- ARCHITECTURE.md lists 6 contexts, the code has 8. `sales` is absent from ARCHITECTURE.md and
  PDR-001 entirely and has no PDR. `dashboard` is documented — as living inside
  `infrastructure/` (ARCHITECTURE.md:16) — but the code has it as a top-level context.
  PDR-001 itself lists only 4 contexts; `distribution` and `inventory` arrived via PDR-007.
- `identity` has no modules — it is layered context-wide.
- `sales/distributor_return` breaks Java package naming.
- `src/test/java/…/SaleLineItemProcessorTest.java` sits at the repo root outside `backend/`,
  compiled by no build.

### 5. No domain events

Zero `ApplicationEventPublisher`, `@EventListener`, or `@TransactionalEventListener` usages.
Every cross-context interaction is a synchronous direct call — the mechanism producing §2.

### 6. Contract drift

`contracts/openapi.yaml` (111 lines) covers `/login`, `/logout`, `/api/session`. The other 11
controller trees are absent, while root CLAUDE.md declares the file the FE/BE source of truth.
springdoc is also on the classpath, giving two unreconciled specs.

## Decision

Remediate in the following sequence. P0 blocks new feature work; the rest are ordered by
dependency, not by preference.

### P0 — Close the tenancy hole

Build a single enforcement point rather than per-endpoint checks. Two candidates:

- **Structural** — Hibernate `@TenantId` (or `@FilterDef` + a `TenantContext` populated in the
  security filter chain) on every tenant-scoped entity. Isolation cannot be forgotten on a new
  query.
- **Explicit** — `LabelAccessGuard.requireOwnership(labelId, userId)` in
  `infrastructure/security`, called by every controller carrying a `{labelId}` path variable,
  with an ArchUnit rule asserting that call.

Add per-context integration tests proving a second user receives 404 — covering the mutating
endpoints (`PUT`/`DELETE /api/labels/{labelId}/sales/{saleId}` and their equivalents), not only the
reads. Cross-tenant writes are the higher-severity half and the easier one to leave untested.
Rewrite PDR-004: it
rejected PostgreSQL RLS in favour of application-layer filtering, and the filtering was never
built.

### P1 — Make the rules executable

Add ArchUnit tests, or adopt **Spring Modulith** — `ApplicationModules.verify()`,
`@ApplicationModule(allowedDependencies = …)`, generated module documentation, and a
transactional event publication log, all of which this codebase already needs. Initial rules:

- [ ] No cycles between bounded contexts
- [ ] Cross-context imports resolve only to `api/` packages
- [ ] No repository injection across module boundaries
- [ ] `@RestController` classes live only in `api/` packages

The first run fails on every item in §3. That is the acceptance signal.

### P2 — Break the cycles

1. **Extract a read-model context.** `ReleaseController`'s detail endpoint is a report, not
   catalog behaviour. Move cross-context assembly into `reporting/` (folding in `dashboard`), a
   context explicitly downstream of all others and depended on by none. This removes most of §2.
2. **Domain events on the write side.** `sales` publishes `SaleRegistered`; `inventory` reacts
   via `@TransactionalEventListener`, replacing the direct `InventoryMovementCommandApi` call.
3. **Declare a shared kernel.** A `shared/` context owning `Money`, `ReleaseFormat`,
   `ChannelType`, `MovementType`, `InventoryLocation` — depended on by all, depending on none.

### P3 — Reconcile structure with documentation

- [ ] One JPA package name (`persistence/`); reserve `infrastructure` for the cross-cutting context
- [ ] Update ARCHITECTURE.md and PDR-001 to the real context list; write the missing PDR for `sales`
- [ ] Move `AppUserDetails` to `identity/api`, or have `infrastructure/security` own a
      `CurrentUser` type that controllers depend on instead
- [ ] Invert `DocumentStoragePort` → `finance`: the port defines its own return type
      (this is what breaks the `finance ↔ infrastructure` cycle in §2)
- [ ] Rename `distributor_return`
- [ ] Delete the stray root `src/`
- [ ] Split `identity` into modules, or record that it is intentionally single-module

### P4 — One API contract

Generate `openapi.yaml` from springdoc in CI and commit it, or backfill it by hand plus a CI
check that the generated spec matches. The frontend currently has only `api/auth.js`, so the
cost of fixing this is near zero today and grows with every page added.

## Rationale

- The documented architecture is sound; the failure mode is drift, which only a build-time check
  prevents. P1 before P2/P3 means the cleanup is verified rather than asserted.
- §1 is a security defect, not a design preference — it precedes everything.
- The cycles in §2 are a symptom of missing constructs (a read model, a shared kernel, events),
  not of careless imports. Adding the constructs removes the cycles; forbidding the imports alone
  would not.

## Consequences

- P0 changes the response of existing endpoints for non-owning users. Any e2e or frontend code
  relying on cross-tenant reads breaks — none is expected to exist.
- P2's read-model context deliberately depends on many contexts. That is its purpose; the
  ArchUnit rules must exempt it as a designated downstream module.
- Spring Modulith constrains package layout more tightly than the current structure. Adopting it
  makes P3 a prerequisite rather than an optional cleanup.
- Domain events introduce eventual consistency within a transaction boundary. For
  `SaleRegistered` → inventory movement, use `@TransactionalEventListener(BEFORE_COMMIT)` or
  Modulith's publication log to keep the invariant.

## Open Questions

- Aggregate boundaries: ID-only references (PDR-006) plus no aggregate roots means invariants
  spanning entities (allocation quantity ≤ production run quantity) live in use cases rather than
  the domain. Making `ProductionRun` an aggregate root owning its allocations would relocate that
  invariant but conflicts with PDR-006. Needs an explicit decision, not drift.
- Whether `sales` is a bounded context or a module of `distribution` — PDR-007 split distribution
  from inventory without addressing sales.

## References

- PDR-001: Organize Code by Bounded Contexts
- PDR-004: Multi-Tenant Isolation via user_id Denormalization
- PDR-006: JPA Modeling with IDs
- PDR-007: Create Distribution Bounded Context
- ARCHITECTURE.md
