# Label Manager - Project Overview

## Purpose
Multi-tenant web application for managing independent music labels. Users own Labels, which have Artists, Releases (with Tracks), Costs, Distributors, Sales, and Returns.

## Tech Stack
- Java 25, Spring Boot 4.0.0, Thymeleaf, Bootstrap 5
- PostgreSQL with Flyway migrations
- Gradle (Kotlin DSL)
- JavaScript: Vitest for unit tests, Playwright for E2E
- TestContainers for integration tests

## Architecture
Bounded contexts: catalog, identity, finance, distribution, sales, inventory, infrastructure
Each context uses modular architecture: api/ (public), application/ (package-private), domain/ (public), infrastructure/ (package-private)

## Key Patterns
- Clean Architecture with Command/Query API separation (CQRS-lite)
- TDD: Red-Green-Refactor cycle
- Package-private by default
- Domain records (not JPA entities) cross module boundaries
- ID references between modules (no embedded objects)
