# Suggested Commands

## Build & Run
- `./gradlew build` - Build the project
- `./gradlew bootRun` - Run the application
- `./gradlew test` - Run all Java tests

## Testing
- `./gradlew test --tests "ClassName"` - Run a single test class
- `./gradlew test --tests "*IntegrationTest"` - Run all integration tests
- `./gradlew test --tests "*ControllerTest"` - Run all controller tests
- `npm run test` - Run JavaScript unit tests
- `npm run test:e2e` - Run E2E tests (auto-starts app)

## Code Quality
- `./gradlew checkstyleMain checkstyleTest --quiet` - Run checkstyle

## Task Completion Checklist
1. Run `./gradlew test` - all tests pass
2. Run `./gradlew checkstyleMain checkstyleTest --quiet` - no style violations
3. Review own diff for visibility, naming, edge cases
4. Update tasks.md if applicable
5. Commit with descriptive message
6. Stop and summarise to user
