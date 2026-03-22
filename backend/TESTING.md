# Testing Strategy

Follow the testing pyramid. Each layer owns specific responsibilities—avoid duplication.

```
      /\        E2E (Playwright)        - Smoke tests, deployment verification
     /  \       System (SpringBootTest) - Full stack API flows
    /    \      Integration             - Database, external services
   /      \     Controller (WebMvcTest) - HTTP routing, views
  /________\    Unit (JUnit/Vitest)     - Business logic
```

## Unit Tests

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

### Writing Behavior-Focused Unit Tests

**Prefer testing behavior over implementation.** Focus on outcomes, not method calls.

```java
// ✅ Good - tests observable behavior
@Test
void throwsException_whenProductionRunCannotAllocate() {
    doThrow(new InsufficientInventoryException(200, 50))
        .when(productionRunQueryApi)
        .validateQuantityIsAvailable(PRODUCTION_RUN_ID, QUANTITY);

    assertThatThrownBy(() -> useCase.invoke(...))
        .isInstanceOf(InsufficientInventoryException.class);
}

// ❌ Bad - tests implementation details
@Test
void invoke_callsValidationAndCreatesAllocation() {
    useCase.invoke(...);
    verify(productionRunQueryApi).validateQuantityIsAvailable(...);
    verifyNoMoreInteractions(allocationCommandApi);
}
```

**Guidelines:**
- ✅ Test observable outcomes (return values, exceptions thrown)
- ✅ Setup mocks to return test data or throw exceptions
- ❌ Avoid excessive `verify()` calls on mocks
- ❌ Don't use `verifyNoMoreInteractions()` unless there's a specific reason

## Controller Tests

**Purpose**: Test HTTP routing and view rendering. Uses `@WebMvcTest` (slice).

**Test**: Request mappings, redirects, view names, model attributes, form binding, validation errors.
**Mock**: All API interfaces (CommandApi/QueryApi) via `@MockitoBean`.

```bash
./gradlew test --tests "*ControllerTest"
```

Note: `@WebMvcTest` does NOT render Thymeleaf templates. Template errors are only caught by E2E tests.

## Integration Tests

**Purpose**: Test component wiring with real dependencies.

**Uses**: `@SpringBootTest` + TestContainers (PostgreSQL).
**Named**: `*IntegrationTest.java`
**Test**: Repository queries, transactions, cascades, service orchestration.
**Count**: Dozens, not hundreds.

```bash
./gradlew test --tests "*IntegrationTest"
```

### AbstractIntegrationTest Pattern

All integration tests extend `AbstractIntegrationTest`:

```java
@SpringBootTest
public abstract class AbstractIntegrationTest {
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void dbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

**Important**: Do NOT use `@Testcontainers` or `@Container` annotations — they prevent container sharing.

### Organizing Integration Tests by Operation

```
catalog/label/
├── CreateLabelIntegrationTest.java
├── UpdateLabelIntegrationTest.java
├── DeleteLabelIntegrationTest.java
└── QueryLabelIntegrationTest.java
```

Each test class extends `AbstractIntegrationTest`, focuses on one operation, and autowires API interfaces (not internal use cases).

### Prefer Integration Over Unit for CRUD

**When to use integration tests:**
- ✅ Simple CRUD use cases
- ✅ Service orchestration without complex logic
- ✅ Verifying database queries and transactions

**When to use unit tests with mocks:**
- ✅ Complex business logic with multiple branches
- ✅ Calculations and algorithms
- ✅ External service interactions

```java
// ❌ Bad - mechanical unit test
@Test
void execute_deletesLabelById() {
    useCase.execute(1L);
    verify(repository).deleteById(1L);
}

// ✅ Good - integration test that verifies real behavior
@Test
void deleteLabel_removesLabelFromDatabase() {
    var label = createTestLabel();
    labelCommandApi.delete(label.id());
    assertThat(labelQueryApi.findById(label.id())).isEmpty();
}
```

## System Tests

**Purpose**: Test full stack via real HTTP.

**Uses**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` + TestContainers.
**Named**: `*SystemTest.java`
**Test**: Critical API flows (happy path + high-risk edge cases), security, serialization.
**Count**: Single digits (5-15 total).

```bash
./gradlew test --tests "*SystemTest"
```

## E2E Tests (Playwright)

**Purpose**: Verify the app works through a real browser. Deployment smoke tests.

**Test**: App loads, login works, critical user journey.
**Count**: Minimal (3-5 tests).

```bash
npm run test:e2e                                  # Local
E2E_TARGET_URL=https://app.com npm run test:e2e   # Deployed instance
```

## Avoiding Duplication

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

## Test Helpers and Factories

**Test Factories**: Fluent builders for test data — `LabelFactory.aLabel().name("Test").build()`

**Module Test Helpers**: For modules with package-private internals, provide a public test helper in `src/test/java`:

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

## Test Package Structure

Test packages must mirror the production package structure:

```
src/main/java/                          src/test/java/
catalog/label/                          catalog/label/
├── api/                                ├── api/
│   └── LabelController                 │   └── LabelControllerTest
├── domain/                             ├── domain/
│   └── Label                           │   └── LabelTest (if complex logic)
└── infrastructure/                     └── infrastructure/
    └── LabelRepository                     └── LabelRepositoryIntegrationTest
```

## Refactoring Workflow

When refactoring a module:

1. Restructure production code: create use cases and API implementations
2. Update integration test: ensure it covers all CRUD operations via the new API interfaces
3. Move test files: match production package structure
4. Verify: run all tests
5. Skip: don't create mechanical unit tests for simple delegation

**Never** complete a refactoring without ensuring integration tests cover the main flows.
