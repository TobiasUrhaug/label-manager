# PDR-002: Use Records for Domain Models, Entities for Persistence

## Status

Accepted

## Context

We need to represent business concepts (Label, Release, Artist) in code. JPA requires mutable entities with default constructors, but mutable objects are harder to reason about and test.

## Decision

Maintain **two representations** for each aggregate:

1. **Domain records** (immutable) - Business logic lives here
2. **JPA entities** (mutable) - Persistence mechanics only

### Domain Layer

```java
// domain/Label.java
public record Label(
    Long id,
    String name,
    Person owner,
    Address address
) {
    public static Label fromEntity(LabelEntity entity) {
        return new Label(
            entity.getId(),
            entity.getName(),
            // ... mapping
        );
    }
}
```

### Infrastructure Layer

```java
// infrastructure/LabelEntity.java
@Entity
@Table(name = "label")
public class LabelEntity {
    @Id @GeneratedValue
    private Long id;
    private String name;
    // ... mutable fields with getters/setters
}
```

## Rationale

- **Immutability**: Domain objects can't be accidentally modified
- **Testability**: Records are easy to construct and compare in tests
- **Clean separation**: JPA annotations don't pollute domain logic
- **Value semantics**: Records provide equals/hashCode automatically

## Consequences

- Mapping code required between domain and entity layers
- Two classes per aggregate (more files)
- Must be disciplined about where business logic lives (domain, not entity)
- Value objects use the same pattern: `Money` record + `MoneyEmbeddable`
