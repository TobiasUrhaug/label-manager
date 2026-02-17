# Code Review — feature/sales-recording-distributor

**Reviewer:** Systems Reviewer
**Date:** 2026-02-17
**Spec reference:** spec.md
**Status:** Changes Requested

---

## Summary

This is a re-review covering the four Phase 3 commits (TASK-010 through TASK-013):

| Commit | Scope |
|--------|-------|
| `3c0b11c` | V27 migration — add `distributor_id` to sale table |
| `5db9188` | Add `distributorId` to `SaleEntity` and `Sale` domain record |
| `d075ed6` | Add `getSalesForDistributor` and `getSalesForProductionRun` to `SaleQueryApi` |
| `c7a0e69` | Mark TASK-010 through TASK-013 complete in tasks.md |

**Previous review items — status:**

| ID | Status | Notes |
|----|--------|-------|
| R-001 empty-list crash | ✅ Resolved | Guard added at top of `execute()` |
| R-002 wrong exception type | ✅ Resolved | `InsufficientInventoryException` now used and tested |
| R-003 redundant production run lookup | ✅ Resolved | Cached in `LinkedHashMap` and reused in step 6 |
| R-004 per-line-item distributor re-lookup | ✅ Resolved | `determineDistributor` returns `Distributor` object; passed down |
| R-005 double scan in `getCurrentInventoryByDistributor` | ✅ Resolved | Balanced with a single `toMap` pass, then filtered |
| R-006 mixed `@Transactional` imports | ✅ Resolved | `SaleQueryApiImpl` now uses `org.springframework.transaction.annotation.Transactional` |
| R-007 `tasks.md` not updated | ✅ Resolved | TASK-001 through TASK-013 all marked `[x]` |
| R-008 fully-qualified `Stream` usage | ✅ Resolved | `import java.util.stream.Stream` added |
| R-009 flaky ordering test (nice-to-have) | 🔵 Deferred | Not addressed; carried forward as a suggestion |
| R-010 duplicate API methods (nice-to-have) | 🔵 Deferred | Not addressed; carried forward as a suggestion |

The V27 migration is careful and well-commented. The `distributorId` wiring through
entity → domain record → `RegisterSaleUseCase` is correct and the `SaleQueryIntegrationTest`
is thorough on distributor filtering and cross-label isolation. One significant correctness
bug in the `getSalesForProductionRun` query needs to be fixed before Phase 4 builds on top of it.

---

## Findings

### 🔴 Must Fix (Blockers)

#### R-011: `getSalesForProductionRun` query is semantically wrong for labels with multiple pressings

- **File:** `src/main/java/org/omt/labelmanager/sales/sale/infrastructure/SaleRepository.java`, lines 19–30
- **Category:** Correctness
- **Description:** The native SQL joins `sale_line_item` to `production_run` via
  `release_id + format`, then filters by `pr.id = :productionRunId`. This means: find all
  sales that have a line item with the same `release_id + format` as the requested production
  run. But if a label has had **multiple production runs** for the same release and format (a
  first pressing in 2020, id=5, and a repress in 2022, id=7), then
  `getSalesForProductionRun(7)` will also return all sales from the 2020 pressing — because
  their line items share the same `release_id + format` and the JOIN matches production run 7.

  Represses are a routine business event for indie labels. The query is correct only for the
  degenerate case where every release has exactly one production run per format.

  The correct source of truth for "which production run does this sale belong to?" is the
  `inventory_movement` table: every SALE movement records `production_run_id` as the FK.
  The fix is to join through movements:

  ```sql
  SELECT DISTINCT s.*
  FROM sale s
  JOIN inventory_movement im
    ON im.reference_id = s.id
    AND im.movement_type = 'SALE'
  WHERE im.production_run_id = :productionRunId
  ORDER BY s.sale_date DESC
  ```

- **Suggestion:** Replace the current native query with the movement-based join above. Add a
  test that creates two production runs for the same release+format, records a sale against
  each, and verifies that `getSalesForProductionRun(runA)` returns only the sale for run A.

---

### 🟡 Should Fix

#### R-006 (carry-forward): `SaleQueryApiImpl` still uses `jakarta.transaction.Transactional`

- **File:** `src/main/java/org/omt/labelmanager/sales/sale/application/SaleQueryApiImpl.java`, line 3
- **Category:** Consistency
- **Description:** The previous review flagged mixed `@Transactional` import styles in the
  codebase. `RegisterSaleUseCase` was corrected to use
  `org.springframework.transaction.annotation.Transactional`. But in the same commit
  (`d075ed6`) that added `@Transactional` annotations to all list methods in
  `SaleQueryApiImpl`, the class still imports `jakarta.transaction.Transactional`. The
  inconsistency is still present.
- **Suggestion:** Change line 3 to `import org.springframework.transaction.annotation.Transactional;`
  and verify the rest of the project follows suit.

---

#### R-012: `SaleQueryIntegrationTest` bypasses module APIs to set up test data

- **File:** `src/test/java/org/omt/labelmanager/sales/sale/SaleQueryIntegrationTest.java`, lines 16–31 (imports), 54–100 (`@BeforeEach`)
- **Category:** Architecture / DDD compliance
- **Description:** The test directly injects and uses infrastructure classes from four other
  modules:
  - `DistributorRepository`, `DistributorEntity` (distribution module)
  - `ProductionRunRepository`, `ProductionRunEntity` (inventory/productionrun module)
  - `ChannelAllocationRepository` (inventory/allocation module)
  - `InventoryMovementRepository` (inventory/inventorymovement module)

  A `ProductionRunTestHelper` already exists in the test tree and is not used. CLAUDE.md
  states that modules should expose test helpers in `src/test/java` for exactly this reason,
  and that "repository injection from other modules" violates encapsulation even in tests.

  Directly calling `distributorRepository.save(new DistributorEntity(...))` bypasses any
  business rules the distributor module might enforce and couples the test to infrastructure
  internals that can change.

  The same issue affects `SalePersistenceIntegrationTest` (commit `5db9188`), which also
  wires `DistributorRepository` directly to supply the new mandatory `distributorId` field.

- **Suggestion:**
  1. Use the existing `ProductionRunTestHelper` to create production run fixtures.
  2. Create a `DistributorTestHelper` (mirroring `LabelTestHelper`) in
     `src/test/java/org/omt/labelmanager/distribution/distributor/` that wraps
     `DistributorRepository` and exposes `createDistributor(Long labelId, String name,
     ChannelType channelType)`.
  3. Use the test helper (or `DistributorCommandApi`, if it exposes create) everywhere a
     distributor fixture is needed in tests outside the distributor module.
  4. Drop the direct `InventoryMovementRepository.deleteAll()` / `ChannelAllocationRepository.deleteAll()`
     calls in `@BeforeEach` — prefer using `@Transactional` + rollback on each test, or scope
     the cleanup to the specific data created in that test.

---

#### R-013: `getSalesForProductionRun` has no ordering test

- **File:** `src/test/java/org/omt/labelmanager/sales/sale/SaleQueryIntegrationTest.java`
- **Category:** Test gap
- **Description:** The TASK-013 acceptance criteria doesn't explicitly require an ordering
  test for `getSalesForProductionRun`, but the `SaleQueryApi` Javadoc states "ordered by
  date (newest first)" and the spec lists all list methods as returning results in that order.
  `getSalesForDistributor` has a dedicated ordering test
  (`getSalesForDistributor_returnsSalesOrderedByDateDescending`); `getSalesForProductionRun`
  does not. The query has `ORDER BY s.sale_date DESC` so it will work, but the ordering is
  untested.
- **Suggestion:** Once R-011 is fixed and the query is rewritten, add an ordering test similar
  to the one for `getSalesForDistributor`. Create two sales against the same production run on
  different dates and assert the result order.

---

### 🟢 Suggestions (Nice to Have)

#### R-009 (carry-forward): Potential flaky ordering in `QueryMovementIntegrationTest`

- **File:** `src/test/java/org/omt/labelmanager/inventory/inventorymovement/QueryMovementIntegrationTest.java`
- **Description:** Same as previous review. The `getMovementsForProductionRun_returnsMovementsNewestFirst`
  test relies on two sequential `Instant.now()` calls producing distinct timestamps. On a fast
  machine or in CI with a frozen clock, both calls may land in the same millisecond, making the
  sort order arbitrary.
- **Suggestion:** Inject a `Clock` into `RecordMovementUseCase` and set distinct instants in
  tests, or add explicit `Thread.sleep(10)` between the two record calls (simple but fragile).

#### R-010 (carry-forward): `findByProductionRunId` and `getMovementsForProductionRun` are duplicates

- **File:** `src/main/java/org/omt/labelmanager/inventory/inventorymovement/api/InventoryMovementQueryApi.java`
- **Description:** Same as previous review. Two names for one operation on the public API.
  The Javadoc already acknowledges the alias relationship.
- **Suggestion:** Deprecate `findByProductionRunId` and migrate callers over time.

#### R-014: V27 fallback UPDATE could silently corrupt data in production

- **File:** `src/main/resources/db/migration/V27__add_distributor_id_to_sale.sql`, lines 27–33
- **Description:** The third UPDATE assigns the label's DIRECT distributor to any sale row
  still lacking a `distributor_id` after the first two passes. The commit message explains
  this handles "dev data predating the referenceId feature" — a reasonable pragmatic decision.
  However, if this migration were ever run against a production database with DISTRIBUTOR-channel
  sales that have no movement records for any reason (manual deletion, ETL import, etc.), those
  sales would silently receive an incorrect `distributor_id`. There is no warning, log, or
  assertion that the rows being updated by the fallback should actually be zero in a clean
  production environment.
- **Suggestion:** Add a comment to the fallback UPDATE warning that this should affect zero
  rows in production, and optionally add a `DO $$ ... ASSERT ... $$` block that logs a WARNING
  if the fallback touches any rows, so an operator is alerted during deployment.

---

### ✅ What's Done Well

- **V27 migration structure is solid.** The three-stage backfill (DIRECT channel → movement
  lookup → fallback) is well-sequenced and commented. Adding the column nullable, filling it,
  then applying NOT NULL is the correct pattern for adding required columns to a live table.

- **`determineDistributor` refactoring is clean.** The previous commit fetched the `Distributor`
  object once in `determineDistributor` and passed it as a full object to `validateAndAddLineItem`,
  eliminating the per-line-item lookup from R-004. The code is easier to follow as a result.

- **`RegisterSaleUseCase` now uses `InsufficientInventoryException` and Spring's
  `@Transactional`.** Both R-001 and R-002 from the previous review are cleanly resolved.
  The integration test assertion now checks for the correct exception type.

- **`SaleQueryIntegrationTest` distributor filtering and isolation tests.** The tests cover
  the important isolation case (`getSalesForDistributor` returns only the right distributor's
  sales), the empty list case, the `distributorId` persistence on registration, and cross-label
  isolation in `getSalesForProductionRun`. These are exactly the edge cases that matter.

- **`SaleQueryApiImpl` list methods are now `@Transactional`.** Adding transactions to all
  list-returning methods that stream line items (a `@OneToMany` lazy-loaded collection) is the
  right call to avoid `LazyInitializationException` in callers outside a transaction boundary.

---

## Verdict

**Changes Requested.**

- R-011 (`getSalesForProductionRun` query) is a correctness bug that will silently return
  wrong data for any label with multiple production runs for the same release and format.
  Fix it and add the matching repress scenario test before Phase 4 builds on this query.

- R-012 (test module boundary violations) and R-013 (missing ordering test) should be
  addressed at the same time since the test file needs rework anyway.

- R-006 (jakarta import) is a one-liner — fold it into the same commit.
