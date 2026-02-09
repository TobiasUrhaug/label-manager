# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Test Commands

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run all Java tests
./gradlew test

# Run a single test class
./gradlew test --tests LabelControllerTest

# Run checkstyle
./gradlew checkstyleMain checkstyleTest

# Run JavaScript unit tests
npm run test

# Run E2E tests (auto-starts app)
npm run test:e2e

# Run E2E tests against deployed instance
E2E_TARGET_URL=https://app.example.com npm run test:e2e
```

## Domain

A multi-tenant application for managing independent music labels.

- **Users** own one or more **Labels**
- **Labels** have **Artists** and **Releases**
- **Releases** contain **Tracks** (with artist, duration) and can be physical (vinyl, CD) or digital
- **Costs** track expenses (mastering, distribution) with VAT calculations and document attachments

## Architecture

Spring Boot 4.0.0 web application (Java 25) using Thymeleaf for server-side templating with Bootstrap 5.

### Package Structure

Organized by bounded context, each following clean architecture:

```
org.omt.labelmanager/
├── catalog/           # Labels, releases, artists, tracks
├── identity/          # Users, authentication
├── finance/           # Costs
└── infrastructure/    # Cross-cutting: security, storage, dashboard
```

Within each bounded context, organize by **module** (not by layer):

```
catalog/
├── label/             # Label module (fully encapsulated)
│   ├── Label.java                  # Public domain record
│   ├── LabelCRUDHandler.java       # Public service (mutations)
│   ├── LabelQueryService.java      # Public service (queries)
│   ├── LabelEntity.java            # package-private
│   ├── LabelRepository.java        # package-private
│   └── api/
│       └── LabelController.java
├── release/           # Release module
├── artist/            # Artist module
└── domain/
    └── shared/        # Shared value objects (Address, Person, etc.)
```

### Modular Architecture Pattern

Within each bounded context, organize code into **modules** with encapsulated internals and clean public APIs. This prevents tight coupling and allows modules to evolve independently.

#### Module Structure

Each module (e.g., `label`, `release`, `artist`) should follow this pattern:

```
catalog/label/
├── Label.java                  # Public domain record
├── LabelCRUDHandler.java       # Public - mutations (create, update, delete)
├── LabelQueryService.java      # Public - queries (findById, exists)
├── LabelEntity.java            # package-private (internal persistence)
├── LabelRepository.java        # package-private (internal persistence)
└── api/
    └── LabelController.java    # Public HTTP interface
```

#### Encapsulation Rules

1. **Make internal classes package-private**: `Entity` and `Repository` classes should have **no access modifier** (package-private), not `public`.

2. **Provide a QueryService facade**: Create a public `*QueryService` for read operations that other modules need:
   ```java
   @Service
   public class LabelQueryService {
       private final LabelRepository repository;  // package-private, only accessible within module

       public Optional<Label> findById(Long id) {
           return repository.findById(id).map(Label::fromEntity);
       }

       public boolean exists(Long id) {
           return repository.existsById(id);
       }
   }
   ```

3. **Use ID references between modules**: Domain records should reference other modules by ID, not by embedding full objects:
   ```java
   // Good - loose coupling
   public record Release(Long id, String name, Long labelId, ...) {}

   // Avoid - tight coupling
   public record Release(Long id, String name, Label label, ...) {}
   ```

4. **Keep mapping methods package-private**: Methods like `fromEntity()` should be package-private:
   ```java
   public record Label(...) {
       // package-private - only QueryService can call this
       static Label fromEntity(LabelEntity entity) { ... }
   }
   ```

5. **Provide test helpers for other modules**: Create a public test helper in the test source tree:
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

#### Benefits

- **Encapsulation**: Internal persistence details hidden from other modules
- **Flexibility**: Can refactor module internals without affecting other modules
- **Clear contracts**: Public API (QueryService, CRUDHandler) defines what other modules can do
- **Testability**: Each module can be tested in isolation

#### Inter-Module Communication

When one module needs data from another:

```java
@Service
public class ReleaseCRUDHandler {
    private final LabelQueryService labelQueryService;  // Use QueryService, not Repository

    public void createRelease(Long labelId, ...) {
        // Validate using QueryService
        if (!labelQueryService.exists(labelId)) {
            throw new IllegalArgumentException("Label not found");
        }
        // ... create release with labelId reference
    }

    public Release findById(Long id) {
        // Return Release with labelId
        // If UI needs label details, controller fetches separately:
        //   Label label = labelQueryService.findById(release.labelId())
    }
}
```

### Layer Separation

| Layer | Contains | Example |
|-------|----------|---------|
| `domain/` | Immutable records, value objects | `Label.java`, `Money.java` |
| `application/` | Use cases, orchestration | `LabelCRUDHandler.java` |
| `api/` | Controllers, forms | `LabelController.java` |
| `infrastructure/` | JPA entities, repositories, adapters | `LabelEntity.java`, `S3DocumentStorageAdapter.java` |

### Database

- PostgreSQL (production and tests via TestContainers)
- Flyway migrations in `src/main/resources/db/migration/`

## Testing Strategy

Follow the testing pyramid. Each layer owns specific responsibilities—avoid duplication.

```
      /\        E2E (Playwright)        - Smoke tests, deployment verification
     /  \       System (SpringBootTest) - Full stack API flows
    /    \      Integration             - Database, external services
   /      \     Controller (WebMvcTest) - HTTP routing, views
  /________\    Unit (JUnit/Vitest)     - Business logic
```

### Unit Tests

**Purpose**: Test business logic in isolation. No Spring context, no I/O.

| Type | What to test | Location |
|------|--------------|----------|
| Java | Domain objects, calculations, mappers | `*Test.java` |
| JavaScript | Utilities, formatting, validation | `*.test.js` (Vitest) |

**Count**: Thousands are fine. Must be fast.

```bash
./gradlew test --tests "*Test" -x "*IntegrationTest" -x "*SystemTest"
npm run test
```

### Controller Tests

**Purpose**: Test HTTP routing and view rendering. Uses `@WebMvcTest` (slice).

**Test**: Request mappings, redirects, view names, model attributes, form binding, validation errors.
**Mock**: All handlers/services via `@MockitoBean`.

```bash
./gradlew test --tests "*ControllerTest"
```

### Integration Tests

**Purpose**: Test component wiring with real dependencies.

**Uses**: `@SpringBootTest` + TestContainers (PostgreSQL).
**Named**: `*IntegrationTest.java`
**Test**: Repository queries, transactions, cascades, service orchestration.

**Count**: Dozens, not hundreds.

```bash
./gradlew test --tests "*IntegrationTest"
```

### System Tests

**Purpose**: Test full stack via real HTTP. High confidence, low diagnostics when failing.

**Uses**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` + TestContainers.
**Named**: `*SystemTest.java`
**Test**: Critical API flows (happy path + high-risk edge cases), security, serialization.
**Don't test**: Every validation rule, every error scenario (use controller/unit tests).

**Count**: Single digits (5-15 total).

```bash
./gradlew test --tests "*SystemTest"
```

### E2E Tests (Playwright)

**Purpose**: Verify the app works through a real browser. Deployment smoke tests.

**Test**: App loads, login works, critical user journey.
**Don't test**: API contracts, edge cases, validation (use system/controller tests).

**Count**: Minimal (3-5 tests).

```bash
npm run test:e2e                                  # Local
E2E_TARGET_URL=https://app.com npm run test:e2e   # Deployed instance
```

### Avoiding Duplication

Each level owns a responsibility. If the same assertion appears at multiple levels, one is wrong.

| Concern | Test Level |
|---------|------------|
| Business rules, calculations | Unit |
| HTTP mapping, validation errors | Controller |
| JSON serialization | Controller |
| Repository queries, JPA mappings | Integration |
| Transactions | Integration |
| Full API flow, security | System |
| Browser rendering, JS interactions | E2E |

### Test Factories

Fluent builders for test data: `LabelFactory.aLabel().name("Test").build()`

#### Module Test Helpers

For modules with package-private internals, provide a public test helper in `src/test/java` that other modules can use to create test fixtures:

```java
@Component
public class LabelTestHelper {
    private final LabelRepository labelRepository;

    public Label createLabel(String name) {
        LabelEntity entity = new LabelEntity(name, null, null);
        return Label.fromEntity(labelRepository.save(entity));
    }
}
```

This allows integration tests in other modules (e.g., `ReleaseCRUDIntegrationTest`) to create label fixtures without accessing package-private classes directly.

## Logging Strategy

Use SLF4J with Logback. Configuration is in `logback-spring.xml` (simple pattern locally, JSON in production).

### Log Levels

| Level | Use For |
|-------|---------|
| **ERROR** | Failed operations needing attention (unhandled exceptions, failed external calls) |
| **WARN** | Unexpected but recoverable situations (retry succeeded, fallback used) |
| **INFO** | Business-significant events (entity created, job started/completed) |
| **DEBUG** | Technical details for troubleshooting (method entry/exit, variable values) |

### Guidelines

- **INFO should tell the business story** - readable without drowning in detail
- **Log at boundaries**: incoming requests (INFO), outgoing calls (DEBUG), business decisions (INFO)
- **Include context**: IDs, user, entity being processed
- **Avoid**: sensitive data, logging in tight loops, duplicate information

### Example Pattern

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger log = LoggerFactory.getLogger(MyClass.class);

public Label createLabel(String name, Long userId) {
    log.info("Creating label '{}' for user {}", name, userId);

    if (labelRepository.existsByName(name)) {
        log.warn("Label '{}' already exists for user {}", name, userId);
        throw new DuplicateLabelException(name);
    }

    Label label = labelRepository.save(new Label(name, userId));
    log.debug("Label created with id {}", label.getId());

    return label;
}
```

## Code Formatting

**IMPORTANT**: Run checkstyle before committing to catch issues early:
```bash
./gradlew checkstyleMain checkstyleTest --quiet
```

### Line Length
Maximum line length is 100 characters. This applies to all code including:
- Method chains and fluent APIs
- String literals and text blocks
- Test data (JSON strings, mock responses)

When lines are too long:
- Break method chains across multiple lines
- Use multi-line text blocks for JSON test data
- Extract long strings into variables

### Long Method Signatures
When a method signature exceeds the line limit, use this format:
- Method name with opening parenthesis on the first line
- Each parameter on its own line, indented with 8 spaces (double indent)
- Closing parenthesis on its own line, aligned with the method modifier
- Opening brace follows directly after the closing parenthesis

```java
@Transactional
public void updateArtist(
        Long id,
        String artistName,
        Person realName,
        String email,
        Address address
) {
    // method body
}
```

## Development Workflow

### TDD with Atomic Commits

Use Test-Driven Development with small, atomic commits. For any feature, break it into slices where each slice is:

1. **A complete, working increment** - tests pass, code compiles
2. **Committed separately** - one commit per slice
3. **TDD within each slice** (Red-Green-Refactor):
   - **Red**: Write a failing test first
   - **Green**: Implement the minimum to make it pass
   - **Refactor**: Clean up the code while tests stay green. Extract well-named helper
     methods that each capture a single concept. Long methods are a sign that the
     refactor step was skipped.
   - Commit

### Example: Adding a new field/feature

**Slice 1: Domain Model**
- Update domain record, factory, mapping tests
- Commit: "Add X to domain model"

**Slice 2: Persistence**
- Add to entity, create migration
- Integration test for persistence
- Commit: "Add X to persistence layer"

**Slice 3: Service Layer**
- Add method to handler
- Integration test
- Commit: "Add X to service layer"

**Slice 4: Controller**
- Add endpoint
- Controller test with MockMvc
- Commit: "Add X endpoint"

**Slice 5: Templates**
- Update UI
- Commit: "Add X to templates"

### Benefits

- Each commit is reviewable and revertible
- Forces clean separation of concerns
- Progress is visible and incremental
- Easier to catch issues early

### Feature Verification

After completing a feature that touches UI/templates, always verify with E2E tests before
considering it done:

```bash
./gradlew test              # Java tests (unit, controller, integration)
npm run test:e2e            # E2E tests - catches template rendering errors
```

Controller tests (`@WebMvcTest`) do NOT render Thymeleaf templates—they only verify view names
and model attributes. Template syntax errors and runtime rendering issues are only caught by
E2E tests or manual testing.

### CSRF Tokens in Forms

All POST/PUT/DELETE forms MUST include a CSRF token. A 403 Forbidden error on form submission
almost always means the CSRF token is missing.

```html
<form method="post" ...>
    <input type="hidden" th:if="${_csrf}" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
    <!-- form fields -->
</form>
```

Forms using `th:action` get CSRF tokens automatically. Forms with dynamic `action` attributes
set via JavaScript (like modals) must include the hidden input explicitly.

## Development Setup

```bash
# Start PostgreSQL
docker compose up -d

# Install JavaScript dependencies
npm install

# Install Playwright browsers (for E2E tests)
npx playwright install
```
