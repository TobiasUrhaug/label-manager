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

## Development Setup

Start PostgreSQL with Docker:
```bash
docker-compose up -d
```
