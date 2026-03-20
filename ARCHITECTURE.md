# Architecture

## Package Structure

Spring Boot 4.0.0 web application (Java 25) using Thymeleaf for server-side templating with Bootstrap 5.

Organized by bounded context, each following clean architecture:

```
org.omt.labelmanager/
├── catalog/           # Labels, releases, artists, tracks
├── identity/          # Users, authentication
├── finance/           # Costs
├── inventory/         # Production runs, allocations, inventory movements
└── infrastructure/    # Cross-cutting: security, storage, dashboard
```

Within each bounded context, organize by **module** (not by layer).

## Modular Architecture Pattern

Within each bounded context, organize code into **modules** with encapsulated internals and clean public APIs. This prevents tight coupling and allows modules to evolve independently.

### Module Structure

Each module (e.g., `label`, `release`, `artist`) should follow this pattern:

```
catalog/label/
├── api/
│   ├── LabelCommandApi.java       # Public interface (mutations)
│   ├── LabelQueryApi.java         # Public interface (queries)
│   └── LabelController.java       # Public HTTP interface
│
├── application/                   # package-private use cases
│   ├── CreateLabelUseCase.java    # Focused business operations
│   ├── UpdateLabelUseCase.java
│   ├── DeleteLabelUseCase.java
│   ├── LabelCommandApiImpl.java   # Implements CommandApi, delegates to use cases
│   └── LabelQueryApiImpl.java     # Implements QueryApi
│
├── domain/
│   └── Label.java                 # Public domain record
│
└── infrastructure/                # package-private
    ├── LabelEntity.java           # JPA entity
    └── LabelRepository.java       # Spring Data repository
```

**Key principles**:
- The `api/` package contains **only module contracts**: public interfaces (`CommandApi`, `QueryApi`), controllers, and form objects. It does not contain view-specific DTOs or read models assembled for Thymeleaf templates.
- Business logic is implemented in small, focused use case classes in `application/`
- API implementations delegate to use cases
- All implementations (use cases, entities, repositories) remain package-private

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

4. **Make all internal classes package-private**: Use case classes, `Entity`, `Repository`, and API implementations should have **no access modifier** (package-private), not `public`.

5. **Use ID references between modules**: Domain records should reference other modules by ID, not by embedding full objects:
   ```java
   // Good - loose coupling
   public record Release(Long id, String name, Long labelId, ...) {}

   // Avoid - tight coupling
   public record Release(Long id, String name, Label label, ...) {}
   ```

6. **Keep mapping methods package-private**: Methods like `fromEntity()` should be package-private. This ensures external callers cannot bypass the application layer and create domain objects directly from entities — all construction goes through the module's own use cases:
   ```java
   public record Label(...) {
       // package-private - only use cases within this module can call this
       static Label fromEntity(LabelEntity entity) { ... }
   }
   ```

7. **Provide test helpers for other modules**: Create a public test helper in the test source tree:
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

Use cases represent discrete business operations.

**When to create a new use case:**
- Each significant business operation gets its own use case class
- Examples: `CreateLabelUseCase`, `PublishReleaseUseCase`, `CalculateRoyaltiesUseCase`
- Use cases should be named with verbs that describe what they do

**Keep use cases focused:**
- Each use case should do one thing well
- Typically 10-30 lines of code

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

**In controllers**, if you need data from multiple modules, fetch them separately:
```java
@GetMapping("/{id}")
public String showRelease(@PathVariable Long id, Model model) {
    Release release = releaseQuery.findById(id).orElseThrow(...);
    Label label = labelQuery.findById(release.labelId()).orElseThrow(...);
    model.addAttribute("release", release);
    model.addAttribute("label", label);
    return "release/detail";
}
```

**Composite read models (views spanning multiple modules)**

When a Thymeleaf view needs data assembled from several modules, compose it in the controller — not in a service or use case. View models are presentation concerns, not domain concerns.

```java
// ✅ Controller assembles the view model from multiple module APIs
@GetMapping("/{id}")
public String showProductionRun(@PathVariable Long id, Model model) {
    ProductionRun run = productionRunQuery.findById(id).orElseThrow(...);
    List<ChannelAllocation> allocations = allocationQuery.getAllocationsForProductionRun(id);
    int warehouseInventory = inventoryMovementQuery.getWarehouseInventory(id);
    model.addAttribute("run", run);
    model.addAttribute("allocations", allocations);
    model.addAttribute("warehouseInventory", warehouseInventory);
    return "inventory/production-run-detail";
}
```

View-specific DTOs that exist solely to carry assembled data to a template belong in the controller's package or a sibling `web/` package — not in any module's `api/` package.

**Avoid bidirectional module dependencies**

If module A depends on module B, module B must not depend on module A. Bidirectional dependencies between modules signal a missing concept — either a shared domain service, or that the two modules belong in the same module.

```
// ❌ BAD - circular dependency
productionrun → allocation (to validate available quantity)
allocation → productionrun (to check manufactured quantity)

// ✅ GOOD - introduce a domain service that both can depend on
inventory/InventoryAvailabilityService → productionrun + allocation
```

**Exception placement**

Exceptions that cross a module boundary belong in the throwing module's `api/` package — they are part of that module's public contract. Callers that catch them depend on the `api/` package they already depend on.

```java
// api/InsufficientInventoryException.java  ← part of the module's contract
public class InsufficientInventoryException extends RuntimeException { ... }

// application/SomeCommandApiImpl.java (throws it)
// another module's Controller (catches it) — both reference the same api/ package
```

## Layer Separation

| Subdirectory | Contains | Visibility | Example |
|--------------|----------|------------|---------|
| `api/` | Module contracts: interfaces, controllers, forms, domain exceptions | Public | `LabelCommandApi.java`, `LabelController.java`, `LabelNotFoundException.java` |
| `application/` | Use cases, API implementations | Package-private | `CreateLabelUseCase.java`, `LabelCommandApiImpl.java` |
| `domain/` | Domain records | Public | `Label.java` |
| `infrastructure/` | JPA entities, repositories | Package-private | `LabelEntity.java`, `LabelRepository.java` |

Note: Shared infrastructure (cross-cutting concerns like security, storage) lives in the `infrastructure/` **bounded context**, not within individual modules.

## Database

- PostgreSQL (production and tests via TestContainers)
- Flyway migrations in `src/main/resources/db/migration/`

## JavaScript and Frontend

**CRITICAL: Never inline JavaScript in templates**

JavaScript must be in separate `.js` files with corresponding tests.

**File structure:**
```
src/main/resources/static/js/
├── sale-form.js           # JavaScript modules
└── ...

src/test/js/
├── sale-form.test.js      # Vitest unit tests
└── ...
```

**Guidelines:**
1. All JavaScript logic goes in `.js` files in `src/main/resources/static/js/`
2. Every `.js` file needs a corresponding `.test.js` file with Vitest tests
3. Templates should only contain HTML/Thymeleaf and minimal inline event handlers
4. Use ES6 modules: export/import functions for testability

```html
<!-- ✅ Template -->
<script src="/js/sale-form.js"></script>
<button onclick="SaleForm.addLineItem()">Add Item</button>
```

```javascript
// ✅ src/main/resources/static/js/sale-form.js
export const SaleForm = {
    addLineItem() { /* ... */ }
};
```

**CSRF Tokens in Forms**

All POST/PUT/DELETE forms MUST include a CSRF token:

```html
<form method="post" ...>
    <input type="hidden" th:if="${_csrf}" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
</form>
```

Forms using `th:action` get CSRF tokens automatically. Forms with dynamic `action` attributes set via JavaScript (like modals) must include the hidden input explicitly.
