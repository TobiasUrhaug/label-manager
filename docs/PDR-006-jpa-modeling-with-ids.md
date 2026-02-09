# PDR-006: JPA Entity Modeling - Use IDs Instead of Entity References

## Status
Proposed

## Context

Our JPA entities currently use two different patterns for representing relationships:

1. **Entity references** (problematic):
   ```java
   @ManyToOne
   @JoinColumn(name = "label_id")
   private LabelEntity label;
   ```

2. **ID-based** (preferred):
   ```java
   @Column(name = "label_id")
   private Long labelId;
   ```

### Current State

**Using entity references (needs refactoring):**
- `ReleaseEntity` → `LabelEntity label`, `List<ArtistEntity> artists`,
  `List<TrackEntity> tracks`
- `TrackEntity` → `ReleaseEntity release`, `List<ArtistEntity> artists`

**Already using IDs (good examples):**
- `ProductionRunEntity` → `releaseId`
- `ChannelAllocationEntity` → `productionRunId`, `salesChannelId`
- `LabelEntity` → `userId`

## Problem

Entity references create tight coupling that violates modular architecture
principles:

### 1. Breaks Module Boundaries
```java
// catalog module
class ReleaseEntity {
    @ManyToOne
    private LabelEntity label;  // Forces dependency on LabelEntity
}
```
- Forces `ReleaseEntity` to depend on `LabelEntity` at compile time
- Requires cross-package imports between infrastructure layers
- Makes it impossible to separate modules later

### 2. Cascade Complexity
- `@OneToMany(cascade = CascadeType.ALL)` creates implicit behavioral coupling
- Easy to accidentally delete/update related entities
- Harder to reason about transaction boundaries

### 3. Lazy Loading Issues
- N+1 query problems
- `LazyInitializationException` when accessing relationships outside transaction
- Forces use of fetch joins or `@EntityGraph` everywhere

### 4. Testing Complexity
- Must construct full object graphs for tests
- Entity factories become complex
- Integration tests require more setup

### 5. Serialization Risks
- Accidental infinite loops in JSON serialization
- Must use `@JsonIgnore` or DTOs everywhere
- Easy to expose more data than intended

## Decision

**Standard practice: Model relationships using IDs, not entity references.**

### Preferred Pattern

```java
@Entity
@Table(name = "release")
public class ReleaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "label_id", nullable = false)
    private Long labelId;  // ID reference, not entity reference

    // No @ManyToOne, no LabelEntity reference
}
```

### When to Use Each Pattern

| Pattern | Use When | Example |
|---------|----------|---------|
| **ID reference** | Cross-aggregate relationships | `ReleaseEntity.labelId` |
| **Entity reference** | Same aggregate, strong ownership | Not recommended |
| **Embeddables** | Value objects, no identity | `AddressEmbeddable` |

### How to Query

Use repository methods with explicit joins when you need related data:

```java
// Repository method
@Query("""
    SELECT r FROM ReleaseEntity r
    JOIN LabelEntity l ON r.labelId = l.id
    WHERE l.userId = :userId
    """)
List<ReleaseEntity> findByUserId(@Param("userId") Long userId);
```

Or fetch separately in the application layer:

```java
// Application layer
ReleaseEntity release = releaseRepository.findById(releaseId);
LabelEntity label = labelRepository.findById(release.getLabelId());
```

### Benefits

1. **Module independence** - Entities don't depend on entities in other
   packages
2. **Explicit queries** - All joins are visible in repository methods
3. **Simpler testing** - Create entities with just IDs, no complex graphs
4. **No lazy loading issues** - No proxies, no session management
5. **Clear boundaries** - Transaction and aggregate boundaries are explicit
6. **Better serialization** - Entities serialize cleanly without circular
   references

### Exceptions

Only use entity references for:

1. **@Embedded** value objects (e.g., `AddressEmbeddable`, `PersonEmbeddable`)
2. **@ElementCollection** for simple value types
3. **Framework requirements** (extremely rare)

## Migration Plan

See separate implementation plan below.

## Consequences

### Positive
- Cleaner module boundaries
- Simpler entity code
- Easier testing
- Predictable query behavior
- No lazy loading surprises

### Negative
- Must write explicit join queries (good trade-off - makes queries visible)
- Slightly more code in application layer to fetch related entities
- No automatic cascade deletes (must handle explicitly)

### Neutral
- Different from typical JPA examples (which often show entity references)
- Requires discipline to maintain consistency

## Implementation Strategy

### For New Code
- Always use ID references for relationships
- Code review checklist item: "Does this entity use ID references?"

### For Existing Code
- Refactor in atomic commits per entity
- Update tests in same commit
- See detailed migration plan below

---

## Appendix: Migration Plan

### Phase 1: ReleaseEntity

**Current problems:**
- `labelId` column exists in database but isn't used
- `label_id` foreign key constraint causes confusion
- Entity uses `LabelEntity label` instead

**Changes needed:**

1. **Update ReleaseEntity**
   - Keep `labelId` field (already exists in DB)
   - Remove `LabelEntity label` field
   - Remove `List<ArtistEntity> artists` → track via join table queries
   - Remove `List<TrackEntity> tracks` → query tracks by `releaseId`

2. **Update all references**
   - Mappers: `ReleaseEntityMapper.toDomain()` → use `labelId` directly
   - Repositories: Add methods to query with joins when needed
   - Services: Fetch label separately if needed

3. **Update tests**
   - Repository integration tests
   - All controller and system tests using releases

**Database:** No migration needed - `label_id` column already exists

### Phase 2: TrackEntity

**Changes needed:**

1. **Update TrackEntity**
   - Add `releaseId` field (column already exists)
   - Remove `ReleaseEntity release`
   - Remove `List<ArtistEntity> artists` → query via join table

2. **Update references**
   - `TrackEntityMapper`
   - Repository methods
   - Service layer

3. **Update tests**

**Database:** No migration needed - `release_id` column already exists

### Phase 3: Artist Associations

Artist relationships are many-to-many via join tables:
- `release_artist` (release_id, artist_id)
- `track_artist` (track_id, artist_id)

**Approach:**
- Don't model these in entities at all
- Create repository methods that query the join tables directly
- Return `List<ArtistEntity>` or `List<Long>` as needed

**Example:**
```java
@Repository
public interface ReleaseArtistRepository {
    @Query("SELECT a FROM ArtistEntity a " +
           "JOIN release_artist ra ON ra.artist_id = a.id " +
           "WHERE ra.release_id = :releaseId")
    List<ArtistEntity> findArtistsByReleaseId(
            @Param("releaseId") Long releaseId
    );
}
```

### Phase 4: Verification

After each phase:
1. Run full test suite: `./gradlew test`
2. Run checkstyle: `./gradlew checkstyleMain checkstyleTest`
3. Run E2E tests: `npm run test:e2e`
4. Verify no N+1 queries (add query logging in tests)

### Rollout Strategy

1. One entity at a time
2. Each change is one atomic commit
3. Tests updated in same commit
4. All tests pass before moving to next entity

### Estimated Effort

| Phase | Estimated LOC Changed | Risk Level |
|-------|----------------------|------------|
| Phase 1: ReleaseEntity | ~200 lines | Medium |
| Phase 2: TrackEntity | ~150 lines | Low |
| Phase 3: Artist associations | ~100 lines | Low |
| **Total** | **~450 lines** | **Medium** |

### Non-Goals

- No changes to `LabelEntity` (already uses `userId`)
- No changes to `CostEntity` (uses embeddables, acceptable)
- No changes to inventory entities (already using IDs)
- No database schema changes (all foreign key columns exist)

---

## References

- Martin Fowler, "Patterns of Enterprise Application Architecture"
- Domain-Driven Design: Aggregate boundaries
- Spring Data JPA documentation: Projections and explicit joins
- Existing code: `ProductionRunEntity`, `ChannelAllocationEntity` (good)
