# label-manager

A web application for tracking music labels and their releases.

This project is developed with [Claude Code](https://claude.ai/code). Architecture decisions, coding patterns, and development workflow are documented in [CLAUDE.md](CLAUDE.md).

## Tech Stack

- Java 25 / Spring Boot 4.0.0
- PostgreSQL 16
- MinIO (S3-compatible object storage)
- Thymeleaf + Bootstrap 5
- Gradle

## Prerequisites

- JDK 25
- Docker

## Getting Started

1. Start the database and object storage:
   ```bash
   docker compose up -d
   ```

2. Run the application:
   ```bash
   ./gradlew bootRun
   ```

3. Open http://localhost:8080

### Local Services

| Service  | URL                    | Credentials       |
|----------|------------------------|-------------------|
| App      | http://localhost:8080  | -                 |
| MinIO UI | http://localhost:9001  | dev / devdevdev   |

## Running Tests

```bash
./gradlew test
```
