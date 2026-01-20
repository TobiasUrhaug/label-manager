# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Test Commands

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests LabelControllerTest

# Run a single test method
./gradlew test --tests LabelControllerTest.label_redirectsToALabel

# Run checkstyle
./gradlew checkstyleMain checkstyleTest
```

## Domain

This application tracks music labels belonging to a user. A user can have multiple labels, and each label can have multiple releases. Releases can be in physical formats (vinyl, CD) or digital.

## Architecture

This is a Spring Boot 4.0.0 web application (Java 25) using Thymeleaf for server-side templating with Bootstrap 5.

### Package Structure

Code is organized by domain feature rather than technical layer:

```
org.omt.labelmanager/
├── label/
│   ├── Label.java              # Domain POJO (Java Record)
│   ├── LabelCRUDHandler.java   # Service layer
│   ├── api/                    # Controllers
│   └── persistence/            # JPA entities and repositories
├── release/                    # Same structure as label
└── dashboard/                  # Dashboard controller
```

### Layer Separation

- **Domain POJOs**: Java Records (`Label`, `Release`) - immutable domain models
- **Entities**: JPA entities (`LabelEntity`, `ReleaseEntity`) - separate from domain to isolate persistence
- **CRUD Handlers**: Services that manage business logic and transactions
- **Controllers**: Handle HTTP requests, delegate to handlers

### Database

- PostgreSQL in production, H2 for tests
- Flyway manages migrations in `src/main/resources/db/migration/`
- One-to-Many relationship: Label → Releases (cascade delete)

## Testing Patterns

### Unit Tests
- Use `@WebMvcTest` with MockMvc and `@MockitoBean` for controller tests
- Located alongside production code in test packages

### Integration Tests
- Use `@SpringBootTest` with TestContainers (PostgreSQL container)
- Named `*IntegrationTest.java`

### Test Factories
- `LabelFactory` and `ReleaseFactory` provide fluent builders for test data
- Pattern: `LabelFactory.aLabel().withName("test").build()`

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

## Development Setup

Start PostgreSQL with Docker:
```bash
docker-compose up -d
```
