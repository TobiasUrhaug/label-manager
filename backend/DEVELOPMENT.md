# Development Guide

## Setup

```bash
# Start PostgreSQL
docker compose up -d

# Install JavaScript dependencies
npm install

# Install Playwright browsers (for E2E tests)
npx playwright install
```

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

## TDD with Atomic Commits

Use Test-Driven Development with small, atomic commits. For any feature, break it into slices where each slice is:

1. **A complete, working increment** — tests pass, code compiles
2. **Committed separately** — one commit per slice
3. **TDD within each slice** (Red-Green-Refactor):
   - **Red**: Write a failing test first
   - **Green**: Implement the minimum to make it pass
   - **Refactor**: Clean up the code while tests stay green

### Example: Adding a new field/feature

| Slice | What | Commit message |
|-------|------|---------------|
| 1 | Update domain record, factory, mapping tests | `Add X to domain model` |
| 2 | Add to entity, create migration, integration test | `Add X to persistence layer` |
| 3 | Add use case, update API impl, integration test | `Add X to application layer` |
| 4 | Add endpoint, controller test | `Add X endpoint` |
| 5 | Update UI | `Add X to templates` |

### Feature Verification

After completing a feature that touches UI/templates, always verify with E2E tests:

```bash
./gradlew test    # Java tests
npm run test:e2e  # Catches template rendering errors
```

## Code Formatting

**IMPORTANT**: Run checkstyle before committing:
```bash
./gradlew checkstyleMain checkstyleTest --quiet
```

### Line Length

Maximum line length is 100 characters. When lines are too long:
- Break method chains across multiple lines
- Use multi-line text blocks for JSON test data
- Extract long strings into variables

### Long Method Signatures

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

- Each parameter on its own line, indented with 8 spaces (double indent)
- Closing parenthesis on its own line, aligned with the method modifier

## Logging Strategy

Use SLF4J with Logback. Configuration is in `logback-spring.xml`.

| Level | Use For |
|-------|---------|
| **ERROR** | Failed operations needing attention |
| **WARN** | Unexpected but recoverable situations |
| **INFO** | Business-significant events (entity created, job completed) |
| **DEBUG** | Technical details for troubleshooting |

**Guidelines:**
- INFO should tell the business story
- Log at boundaries: incoming requests (INFO), outgoing calls (DEBUG), business decisions (INFO)
- Include context: IDs, user, entity being processed
- Avoid: sensitive data, logging in tight loops

```java
private static final Logger log = LoggerFactory.getLogger(MyClass.class);

public Label createLabel(String name, Long userId) {
    log.info("Creating label '{}' for user {}", name, userId);
    // ...
    log.debug("Label created with id {}", label.getId());
    return label;
}
```
