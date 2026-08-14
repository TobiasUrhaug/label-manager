# Architecture

## Package Structure

Spring Boot 4.0.0 REST API (Java 25).

Organized by bounded context, each following clean architecture:

```
org.omt.labelmanager/
├── web/               # Every controller, read model and *View DTO; the ProblemDetail advice
├── catalog/           # Labels, releases, artists, tracks
├── identity/          # Users, authentication
├── finance/           # Costs, invoice extraction
├── distribution/      # Distributors, pricing agreements
├── inventory/         # Production runs, allocations, inventory movements
├── sales/             # Sales, distributor returns
├── shared/            # Domain primitives used by more than one context (Money, Format)
└── infrastructure/    # Cross-cutting: security, storage
```

Within each bounded context, organize by **module** (not by layer).

### `web` is the only module allowed to depend on several contexts at once

Controllers live in `web/<context>/`, not in the module whose data they serve. A response
that needs data from more than one bounded context is composed there and nowhere else.

This rule is what keeps every other arrow pointing one way. A release detail response that
carried its costs, production runs, distributors and sales forced `catalog` to depend on
`finance`, `inventory`, `distribution` and `sales`; putting that composition in `web` deletes
all four edges without changing a line of domain code.

Two consequences worth stating:

- **Nothing depends on `web`.** If a domain module needs something from it, the thing is in
  the wrong place.
- **`web` composes, it does not decide.** Business rules stay in the module that owns them.
  Assembling a response from three query APIs is composition; choosing whether a sale is
  allowed is not.

Read models and `*View` records live in `web/` beside the controller that returns them —
they are presentation shapes, and a module's `api/` package is not for presentation.

## Modular Architecture Pattern

Within each bounded context, organize code into **modules** with encapsulated internals and clean public APIs. This prevents tight coupling and allows modules to evolve independently.

### Choosing a Module Structure

Not every module needs the full layered structure. Choose based on complexity:

- **Full structure**: The module has non-trivial business logic, orchestration across repositories or external APIs, side effects, or complex domain rules.
- **Simplified structure**: The module is primarily CRUD — entities go in, entities come out, with no meaningful business logic beyond validation and persistence.

When planning a new module, **always ask**: does this module have business logic that justifies separate use case classes and the `CommandApi`/`QueryApi` interface split? If the answer is "it's just CRUD", use the simplified structure.

#### Full Module Structure

For modules with real business logic (e.g., `release`, `sale`, `allocation`):

```
catalog/release/
├── api/
│   ├── ReleaseCommandApi.java       # Public interface (mutations)
│   └── ReleaseQueryApi.java         # Public interface (queries)
│                                    # No controller — those live in web/catalog/
│
├── application/                     # package-private
│   ├── CreateReleaseUseCase.java    # Focused business operations
│   ├── BuildTracksUseCase.java
│   ├── ReleaseCommandApiImpl.java   # Implements CommandApi, delegates to use cases
│   └── ReleaseQueryApiImpl.java     # Implements QueryApi
│
├── domain/
│   └── Release.java                 # Public domain record
│
└── persistence/                     # public
    ├── ReleaseEntity.java           # JPA entity
    └── ReleaseRepository.java       # Spring Data repository
```

#### Simplified Module Structure

For CRUD-dominant modules (e.g., `label`, `artist`, `distributor`):

```
distribution/distributor/
├── api/
│   ├── DistributorCommandApi.java  # Public interface (mutations) — only if other modules depend on this module
│   ├── DistributorQueryApi.java    # Public interface (queries) — only if other modules depend on this module
│   ├── Distributor.java            # Public domain record — part of the API's return type
│   └── ChannelType.java            # Public enum used in that record
│                                   # No controller — those live in web/distribution/
│
├── persistence/                    # public
│   ├── DistributorEntity.java      # JPA entity
│   └── DistributorRepository.java  # Spring Data repository
│
├── DistributorCommandService.java  # package-private: handles all mutations directly
└── DistributorQueryService.java    # package-private: handles all queries directly
```

**Key differences from full structure:**
- No separate `application/` or `domain/` sub-packages — services live flat in the module root and the domain record sits in `api/` next to the interfaces that return it
- No separate `*UseCase` classes — the service handles CRUD operations directly
- `CommandApi`/`QueryApi` interfaces are only needed if other modules depend on this module. If only the controller calls the service, skip the interfaces entirely and inject the service directly. (Indicated by the "— only if other modules depend on this module" note in the diagram above.)
- `persistence/` is the only sub-package besides `api/`, separating JPA artifacts from the domain record

**Key principles**:
- The `api/` package is the module's public boundary: interfaces (`CommandApi`, `QueryApi`), the domain records and enums they expose, and domain exceptions and events. It does not contain view-specific DTOs assembled for presentation.
- In a simplified module, everything a caller outside the module needs is reachable from `api/` alone. A type another module imports from the module root is a boundary leak — that is where the package-private services live. (`persistence/` stays public for test helpers and infrastructure adapters, as below.)
- The `persistence/` package contains JPA entities and Spring Data repositories — public so test helpers and shared infrastructure adapters can access them.
- Services (`DistributorCommandService`, `DistributorQueryService`) are package-private and live flat in the module root.
- When a module gains non-trivial business logic, promote it to the full structure.

### Encapsulation Rules

1. **Define public API interfaces in `api/` package**: Create two interfaces per module:
   ```java
   // api/LabelCommandApi.java - mutations
   public interface LabelCommandApi {
       Label createLabel(String name, String email, ...);
       void updateLabel(Long id, String name, ...);
       void delete(Long id);
   }

   // api/LabelQueryApi.java - queries
   public interface LabelQueryApi {
       Optional<Label> findById(Long id);
       boolean exists(Long id);
       List<Label> getLabelsForUser(Long userId);
   }
   ```

2. **Implement business logic in focused use case classes**:
   ```java
   // application/CreateLabelUseCase.java (package-private)
   @Service
   class CreateLabelUseCase {
       private final LabelRepository repository;

       @Transactional
       public Label execute(String name, String email, ...) {
           var entity = new LabelEntity(name, email, ...);
           entity = repository.save(entity);
           return Label.fromEntity(entity);
       }
   }
   ```

3. **Implement API interfaces by delegating to use cases**:
   ```java
   // application/LabelCommandApiImpl.java (package-private)
   @Service
   class LabelCommandApiImpl implements LabelCommandApi {
       private final CreateLabelUseCase createLabel;
       private final UpdateLabelUseCase updateLabel;
       private final DeleteLabelUseCase deleteLabel;

       @Override
       public Label createLabel(String name, String email, ...) {
           return createLabel.execute(name, email, ...);
       }
   }
   ```

4. **Make application classes package-private**: Use case classes and API implementations should have **no access modifier** (package-private), not `public`. Infrastructure classes (`Entity`, `Repository`) are `public` — they may be accessed by test helpers or shared infrastructure adapters.

5. **Use ID references between modules**: Domain records should reference other modules by ID, not by embedding full objects:
   ```java
   // Good - loose coupling
   public record Release(Long id, String name, Long labelId, ...) {}

   // Avoid - tight coupling
   public record Release(Long id, String name, Label label, ...) {}
   ```

6. **Entity-to-domain mapping**: Use `public static fromEntity()` on the domain record for simple mappings. Extract to a separate `*Mapper` class when the mapping requires inputs beyond the entity (e.g., assembled collections):
   ```java
   // Simple mapping — method on the domain record
   public record Label(...) {
       public static Label fromEntity(LabelEntity entity) { ... }
   }

   // Complex mapping — separate mapper class when extra inputs are needed
   public class ReleaseMapper {
       public static Release fromEntity(ReleaseEntity entity, List<Long> artistIds, List<Track> tracks) { ... }
   }
   ```
   Note: domain records depend on their module's persistence entity. This is a pragmatic trade-off accepted in this project — the alternative (a separate mapper for every module) adds boilerplate with little benefit at this scale.

7. **Provide test helpers for other modules**: Create a public test helper in the test source tree. It may call `fromEntity()` directly since that method is public:
   ```java
   @Component  // in src/test/java
   public class LabelTestHelper {
       private final LabelRepository labelRepository;

       public Label createLabel(String name) {
           LabelEntity entity = new LabelEntity(name, null, null);
           return Label.fromEntity(labelRepository.save(entity));
       }
   }
   ```

### Use Case Guidelines

Use cases represent discrete business operations. They belong in modules that use the **full structure** (see above).

**When to create a new use case:**
- The operation involves orchestration (multiple repositories, external API calls, side effects)
- The operation contains business rules beyond simple validation
- Examples: `PublishReleaseUseCase`, `RegisterSaleUseCase`, `CalculateRoyaltiesUseCase`

**When NOT to create separate use case classes:**
- The module is CRUD-only — put the logic directly in a `CommandService`/`QueryService` (simplified structure)
- The operation is a trivial save/delete with no coordination or business rules

**Keep use cases focused:**
- Each use case should do one thing well
- Typically 10-30 lines of code

**`@Transactional` placement:**
- Place `@Transactional` on the use case method (or `CommandService` method in the simplified structure), not on the `CommandApiImpl`
- `CommandApiImpl` delegates to use cases — the transaction boundary belongs with the business operation, not the delegation

**Query operations:**
- Simple queries (find by ID, exists check, find by foreign key) can live directly in `QueryApiImpl`
- Extract to a dedicated use case when the query involves business logic, calculations across multiple repositories, or coordination with another module's API

**Encapsulating side effects:**
- When an operation always requires side effects, encapsulate them in the module
- The module's CommandApi implementation should handle all required side effects

```java
// ✅ GOOD - Side effects encapsulated in module
@Service
class AllocationCommandApiImpl implements AllocationCommandApi {
    private final AllocationRepository repository;
    private final InventoryMovementCommandApi inventoryMovementApi;

    @Override
    @Transactional
    public ChannelAllocation createAllocation(
            Long productionRunId,
            Long distributorId,
            int quantity
    ) {
        var entity = repository.save(new AllocationEntity(...));
        var allocation = ChannelAllocation.fromEntity(entity);

        inventoryMovementApi.recordMovement(
                productionRunId,
                distributorId,
                quantity,
                MovementType.ALLOCATION,
                allocation.id()
        );

        return allocation;
    }
}

// ❌ BAD - Caller must remember side effects
@Service
public class AllocateUseCase {
    public ChannelAllocation invoke(...) {
        var allocation = allocationApi.createAllocation(...);
        inventoryMovementApi.recordMovement(...);  // Easy to forget!
        return allocation;
    }
}
```

### Domain Objects with Business Logic

Domain objects should contain business rules, not just be data carriers.

**What belongs in domain objects:**
- ✅ Business rules and invariants
- ✅ Calculations using the object's data
- ✅ Validation logic
- ✅ Derived values and computed properties

**What stays in application layer:**
- ❌ Logging (infrastructure concern)
- ❌ Exception handling (application decision)
- ❌ Orchestration across modules
- ❌ Side effects (persistence, external calls)

```java
public record ProductionRun(..., int quantity) {
    public boolean canAllocate(int requestedQuantity, int currentlyAllocated) {
        return requestedQuantity <= (quantity - currentlyAllocated);
    }
}
```

### Inter-Module Communication

When one module needs to interact with another, depend on the public APIs:

```java
@Service
class CreateReleaseUseCase {
    private final LabelQueryApi labelQuery;  // Depend on API, not internal classes
    private final ReleaseRepository repository;

    @Transactional
    public Release execute(Long labelId, String name, ...) {
        if (!labelQuery.exists(labelId)) {
            throw new IllegalArgumentException("Label not found");
        }
        var entity = new ReleaseEntity(name, labelId, ...);
        entity = repository.save(entity);
        return Release.fromEntity(entity);
    }
}
```

**CRITICAL: Repository Injection Rules**

Use cases should ONLY inject:
- ✅ Repositories from their own module (same package namespace)
- ✅ QueryApi/CommandApi interfaces from other modules
- ❌ NEVER inject repositories from other modules/bounded contexts

```java
// ❌ WRONG - Violates encapsulation
@Service
class RegisterSaleUseCase {
    private final SaleRepository saleRepo;
    private final DistributorRepository distributorRepo;  // ❌ Other module
}

// ✅ CORRECT - Uses public APIs
@Service
class RegisterSaleUseCase {
    private final SaleRepository saleRepo;
    private final DistributorQueryApi distributorQuery;  // ✅ Via API
}
```

**In controllers** (which live in `web/`), if you need data from multiple modules, fetch them separately:
```java
@GetMapping("/{id}")
public ReleaseDetailResponse getRelease(@PathVariable Long id) {
    Release release = releaseQuery.findById(id).orElseThrow(...);
    Label label = labelQuery.findById(release.labelId()).orElseThrow(...);
    return new ReleaseDetailResponse(release, label);
}
```

**Composite response records (aggregating data from multiple modules)**

When a REST response genuinely needs data assembled from several modules, compose it in the controller in `web/` — not in a service or use case, and never in a domain module. Response shaping is a presentation concern.

```java
// ✅ web/inventory/ProductionRunController assembles from several module APIs
@GetMapping
public List<ProductionRunWithAllocation> productionRuns(@PathVariable Long labelId, ...) {
    List<Distributor> distributors = distributorQueryApi.findByLabelId(labelId);   // distribution
    return queryApi.findByReleaseId(releaseId).stream()                            // inventory
            .map(run -> withAllocation(run, distributors))
            .toList();
}
```

Response records that exist solely to carry assembled data live in `web/`, next to the controller — not in any module's `api/` package.

**But prefer a resource to a bundle.** "This screen shows four things" is not a reason for one endpoint to return four things. Composites that mirror a page are how a module ends up depending on four others, and how a response ends up issuing a query per row. Give each thing its own collection, and compose only when a client genuinely cannot make two calls:

```java
// ❌ A page model: twelve fields from five bounded contexts, one query per movement row
record ReleaseDetailResponse(..., List<Cost> costs, List<ProductionRunWithAllocation> runs,
                             List<Distributor> distributors, List<ReleaseSaleView> sales) {}

// ✅ A release, plus sibling collections that stand on their own
GET /api/labels/{labelId}/releases/{releaseId}
GET /api/labels/{labelId}/releases/{releaseId}/production-runs
GET /api/labels/{labelId}/costs?releaseId=
GET /api/labels/{labelId}/sales?releaseId=
```

**Avoid bidirectional module dependencies**

If module A depends on module B, module B must not depend on module A. Bidirectional dependencies between modules signal a missing concept — either a shared domain service, or that the two modules belong in the same module.

```
// ❌ BAD - circular dependency
productionrun → allocation (to validate available quantity)
allocation → productionrun (to check manufactured quantity)

// ✅ GOOD - introduce a domain service that both can depend on
inventory/InventoryAvailabilityService → productionrun + allocation
```

**Domain events — the narrow exception to synchronous calls**

Synchronous `api/` calls are the default. Publish a domain event instead only when the publisher does
not care whether anyone listens; make a direct call when the outcome depends on the answer.

A caller that rejects its own work based on the result — `RegisterSaleUseCase` throwing
`InsufficientInventoryException` when stock is short — is a synchronous invariant, not a
notification. Turning that into an event would trade a transactional check for eventual consistency
and buy nothing in a single-process, single-database application.

The one event today is `LabelCreated`. `catalog` announces that a label exists; `distribution`
decides on its own that a new label needs a DIRECT distributor. Events live in the publishing
module's `api/` package and are published through `ApplicationEventPublisher`.

```java
// catalog/label/application/CreateLabelUseCase.java — publisher
events.publishEvent(new LabelCreated(entity.getId()));

// distribution/distributor/DirectSalesProvisioner.java — subscriber
@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
void onLabelCreated(LabelCreated event) { ... }
```

Use `BEFORE_COMMIT`, so the subscriber's work joins the publisher's transaction and the two commit or
roll back together. `AFTER_COMMIT` would open a second transaction and allow the publisher's write to
survive a failed subscriber — no queue and no outbox exist to reconcile that.

**Exception placement**

Exceptions that cross a module boundary belong in the throwing module's `api/` package — they are part of that module's public contract. Callers that catch them depend on the `api/` package they already depend on.

```java
// api/InsufficientInventoryException.java  ← part of the module's contract
public class InsufficientInventoryException extends RuntimeException { ... }

// application/SomeCommandApiImpl.java (throws it)
// web/'s controller or ApiExceptionHandler (catches it) — both reference the same api/ package
```

## Layer Separation

**Full structure** (modules with real business logic):

| Subdirectory | Contains | Visibility | Example |
|--------------|----------|------------|---------|
| `api/` | Module contracts: interfaces, domain exceptions, domain events | Public | `LabelCommandApi.java`, `LabelNotFoundException.java`, `LabelCreated.java` |
| `application/` | Use cases, API implementations | Package-private | `CreateLabelUseCase.java`, `LabelCommandApiImpl.java` |
| `domain/` | Domain records | Public | `Label.java` |
| `persistence/` | JPA entities, repositories | Public | `LabelEntity.java`, `LabelRepository.java` |

**Simplified structure** (CRUD-dominant modules):

| Location | Contains | Visibility | Example |
|----------|----------|------------|---------|
| `api/` | Module contracts: interfaces, domain records and enums, domain exceptions, domain events | Public | `DistributorQueryApi.java`, `Distributor.java`, `ChannelType.java` |
| `persistence/` | JPA entities, repositories | Public | `DistributorEntity.java`, `DistributorRepository.java` |
| module root (flat) | Command/query services | Package-private | `DistributorCommandService.java` |

Note: Shared infrastructure (cross-cutting concerns like security, storage) lives in the `infrastructure/` **bounded context**, not within individual modules.

Ports are the exception: a port belongs to the bounded context that needs it, and `infrastructure/` holds only the adapter. `DocumentStoragePort` and `DocumentStorageException` live in `finance/shared/`; `S3DocumentStorageAdapter` implements them from `infrastructure/storage/`. The dependency runs `infrastructure → finance`, never the other way.

## Errors

Every error this application raises deliberately is an RFC 9457 `ProblemDetail`, served as
`application/problem+json`. There are three places it comes from:

| Source | Covers |
|--------|--------|
| `web/ApiExceptionHandler` | `EntityNotFoundException` → 404, `InsufficientInventoryException` / `IllegalArgumentException` / `IllegalStateException` → 400 |
| A controller-local `@ExceptionHandler` | Exceptions only that controller knows about (`AgreementNotFoundException`, `InvoiceParserUnavailableException`) |
| `infrastructure/security` | The filter chain's 401 and 403, written through the application's `JsonMapper` so the mixin applies |

Spring's own exceptions — validation failures, unreadable bodies, `ResponseStatusException` —
are rendered by Boot, via `spring.mvc.problemdetails.enabled` in `application.yaml`.

Two rules:

- **A handler that returns no body is a bug.** `ResponseEntity.badRequest().build()` throws away
  an exception message the caller needed. If a local handler exists only to set a status the
  advice already sets, delete it.
- **Validate at the edge.** A request that cannot succeed should be rejected by `@Valid` with a
  400, not discovered by a database constraint and surfaced as a 500.

## Database

- PostgreSQL (production and tests via TestContainers)
- Flyway migrations in `src/main/resources/db/migration/`, named `V{n}__{description}.sql` (e.g., `V30__add_commission_type.sql`)

