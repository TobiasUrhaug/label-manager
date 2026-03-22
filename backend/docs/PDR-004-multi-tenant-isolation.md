# PDR-004: Multi-Tenant Isolation via user_id Denormalization

## Status

Accepted

## Context

This is a multi-tenant application where users manage their own labels, artists, releases, and costs. We need to ensure users can only access their own data without complex database-level security.

## Decision

Denormalize `user_id` into all tenant-scoped tables and enforce isolation in the application layer.

### Database Schema

```sql
CREATE TABLE label (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    name VARCHAR(255) NOT NULL,
    -- ...
);

CREATE TABLE cost (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    -- ...
);
```

### Application Layer Enforcement

```java
// Controllers extract user from security context
@GetMapping("/labels")
public String listLabels(@AuthenticationPrincipal AppUserDetails user, Model model) {
    List<Label> labels = labelHandler.getLabelsForUser(user.getId());
    // ...
}

// Handlers filter by user_id
public List<Label> getLabelsForUser(Long userId) {
    return labelRepository.findAllByUserId(userId)
        .stream()
        .map(Label::fromEntity)
        .toList();
}
```

## Alternatives Considered

1. **Row-Level Security (RLS) in PostgreSQL** - More secure but complex to maintain and debug
2. **Separate schemas per tenant** - Overkill for this application size
3. **Filter in queries only** - Chose this, simpler and sufficient

## Rationale

- **Simple queries**: Just add `WHERE user_id = ?`
- **Explicit**: Easy to audit that all queries filter correctly
- **Performant**: Index on `user_id` enables efficient filtering
- **Testable**: Easy to verify isolation in integration tests

## Consequences

- Every tenant-scoped query must include `user_id` filter (easy to forget)
- No database-level enforcement if application code has a bug
- Must be careful when adding new tables to include `user_id`
- Code review should verify all new queries filter by user

## Security Checklist for New Features

- [ ] Does the new table need `user_id`?
- [ ] Do all repository methods filter by `userId`?
- [ ] Do controller methods extract user from `@AuthenticationPrincipal`?
- [ ] Are there integration tests verifying tenant isolation?
