# Code Review — feature/sales-recording-distributor

**Reviewer:** Systems Reviewer
**Date:** 2026-02-18
**Spec reference:** spec.md
**Status:** Changes Requested

---

## Summary

This re-review covers five commits since the last review:

| Commit | Scope |
|--------|-------|
| `ad41da2` | Fix R-019: catch `InsufficientInventoryException` in both controllers |
| `bc25df3` | Fix R-020: mark TASK-017–021 as completed in `tasks.md` |
| `53fbafb` | Fix R-021/R-022: `.toList()` in `SaleController`; `readOnly = true` in `DistributorReturnQueryApiImpl` |
| `2a30d1a` | Chore: mark TASK-023 completed (already implemented in TASK-006) |
| `ae419d3` + `555631f` + `a671df1` | TASK-024: inventory status data in `ReleaseController` and template |

**Previous review items — status:**

| ID | Status | Notes |
|----|--------|-------|
| R-009 flaky ordering | 🔵 Deferred | Carried forward |
| R-010 duplicate API methods | 🔵 Deferred | Carried forward |
| R-014 V27 silent fallback UPDATE | 🔵 Deferred | Carried forward |
| R-019 `InsufficientInventoryException` escapes controllers | ✅ Resolved | Added to all four catch clauses; controller tests updated to use the real exception type |
| R-020 `tasks.md` not updated | ✅ Resolved | TASK-017–021 marked `[x]` |
| R-021 inline fully-qualified `Collectors` in `SaleController` | ✅ Resolved | Replaced with `.toList()` |
| R-022 missing `readOnly = true` in `DistributorReturnQueryApiImpl` | ✅ Resolved | All three methods updated |

R-019 was fixed comprehensively: all four handlers (`registerSale`, `submitEdit` in `SaleController`; `registerReturn`, `submitEdit` in `ReturnController`) now catch `InsufficientInventoryException`. Both controller test classes were updated to throw the real exception type, which accurately exercises the catch coverage.

The TASK-024 implementation is solid. The new view-model classes are well-designed — `DistributorInventoryView.sold()` is a textbook example of business logic in a domain object rather than in the template. `MovementHistoryView` with pre-resolved location strings is the right design for keeping templates free of API lookups. `buildDistributorInventories` correctly unions allocation-known and movement-known distributors. The `formatLocation` switch expression is exhaustive and compile-time checked.

Two items should be addressed before merge: a `Collectors.toList()` holdover in `ReturnController.buildEditForm` that was missed when R-021 was fixed, and a missing controller test case for the populated-distributor path in `ReleaseControllerTest`.

---

## Findings

### 🔴 Must Fix (Blockers)

*None.*

---

### 🟡 Should Fix

#### R-023: `ReturnController.buildEditForm` still uses `.collect(Collectors.toList())`

- **File:** `src/main/java/org/omt/labelmanager/sales/distributor_return/api/ReturnController.java`, line 234
- **Category:** Consistency / Style
- **Description:** R-021 fixed `SaleController.buildEditForm` specifically because it used the
  older `Collectors.toList()` pattern instead of `.toList()` (Java 16+). The same pattern
  exists in `ReturnController.buildEditForm`, which was not caught in that fix:

  ```java
  // ReturnController.java line 234 — still old style
  .collect(Collectors.toList())
  ```

  All other stream terminations in both new modules use `.toList()`. This one holdover makes
  the `ReturnController` inconsistent with `SaleController` and the fix that was just applied.

- **Suggestion:** Replace `.collect(Collectors.toList())` with `.toList()` in
  `ReturnController.buildEditForm`.

  Note: `EditReturnForm.toLineItemInputs()`, `RegisterReturnForm.toLineItemInputs()`,
  `RegisterSaleForm.toLineItemInputs()`, and `EditSaleForm.toLineItemInputs()` also use
  `.collect(Collectors.toList())`, but those are form helper methods with a proper import and
  were not the subject of R-021. Fixing them alongside would be a clean sweep if desired.

---

#### R-024: `ReleaseControllerTest` missing test for non-empty distributor inventory

- **File:** `src/test/java/org/omt/labelmanager/catalog/release/api/ReleaseControllerTest.java`, lines 123–147
- **Category:** Test gap
- **Description:** `release_populatesInventoryDataInProductionRuns` only tests the case where
  `getCurrentInventoryByDistributor` returns an empty map, asserting that
  `distributorInventories` is empty. This does not exercise `buildDistributorInventories`,
  which contains the most complex new logic in TASK-024: grouping allocations by distributor,
  taking the union with movement-known distributors, filtering to positive balances, and sorting
  by name.

  In particular, the path where a distributor appears in `currentByDistributor` but not in
  `allocatedByDistributor` (handled by the `filter + forEach` on lines 252–254 of
  `ReleaseController`) is completely uncovered at the controller test level.

- **Suggestion:** Add a second `release_populatesInventoryDataInProductionRuns`-style test that
  provides a non-empty `getCurrentInventoryByDistributor` result and a matching list of
  `ChannelAllocation`s, then asserts:
  - `productionRuns.get(0).distributorInventories()` has the expected size and entries.
  - `DistributorInventoryView.sold()` values are correct.
  - Entries are sorted by name.

---

### 🟢 Suggestions (Nice to Have)

#### R-009 (carry-forward): Potential flaky ordering in `QueryMovementIntegrationTest`

- **File:** `src/test/java/org/omt/labelmanager/inventory/inventorymovement/QueryMovementIntegrationTest.java`
- **Description:** Two sequential `Instant.now()` calls may produce identical timestamps on a
  fast machine or in CI, making the ordering assertion non-deterministic.
- **Suggestion:** Inject a `Clock` into `RecordMovementUseCase` and advance it between calls in
  the test, or add a small `Thread.sleep` between record operations.

---

#### R-010 (carry-forward): `findByProductionRunId` and `getMovementsForProductionRun` are duplicates

- **File:** `src/main/java/org/omt/labelmanager/inventory/inventorymovement/api/InventoryMovementQueryApi.java`
- **Description:** Two names for the same operation on the public API.
- **Suggestion:** Deprecate `findByProductionRunId` and migrate callers over time.

---

#### R-014 (carry-forward): V27 fallback UPDATE could silently corrupt data in production

- **File:** `src/main/resources/db/migration/V27__add_distributor_id_to_sale.sql`
- **Description:** The third UPDATE assigns the DIRECT distributor as a fallback for any sale
  still lacking a `distributor_id`, with no warning that this should affect zero rows in
  production.
- **Suggestion:** Add a comment: `-- Fallback: should affect 0 rows if prior UPDATEs are complete`.

---

#### R-025: Three identical DB queries per production run in the inventory section

- **File:** `src/main/java/org/omt/labelmanager/catalog/release/api/ReleaseController.java`, lines 217–221
- **Category:** Performance
- **Description:** `buildProductionRunWithAllocation` calls `getWarehouseInventory`,
  `getCurrentInventoryByDistributor`, and `getMovementsForProductionRun` in sequence. Each
  method independently calls `movementsFor(productionRunId)` in `InventoryMovementQueryApiImpl`,
  which issues a separate `SELECT … FROM inventory_movement WHERE production_run_id = ?` query.
  For a release with N production runs, this generates 3N queries fetching identical data.

  The spec notes this is "acceptable for the expected data volumes," and for a handful of
  production runs it genuinely is. Worth flagging for awareness: if a label ever has a release
  with many production runs (multiple pressings + digital), this will visibly repeat.

- **Suggestion:** No action required now. If it becomes observable, a `getBulkInventoryStatus`
  method that returns all three pieces of data in one query could consolidate this.

---

#### R-026: `InventoryMovementQueryApiImpl` missing `@Transactional(readOnly = true)`

- **File:** `src/main/java/org/omt/labelmanager/inventory/inventorymovement/application/InventoryMovementQueryApiImpl.java`
- **Category:** Convention / Performance
- **Description:** R-022 specifically filed and fixed the missing `readOnly = true` in
  `DistributorReturnQueryApiImpl`. `InventoryMovementQueryApiImpl` is the same pattern — a
  query-only API impl — but none of its methods have any `@Transactional` annotation at all.
  For consistency with the rest of the codebase convention and to allow Hibernate and the
  JDBC driver to skip unnecessary dirty-checking on reads, adding `@Transactional(readOnly = true)`
  to all five public methods would be appropriate.

- **Suggestion:** Add `@Transactional(readOnly = true)` to `findByProductionRunId`,
  `getMovementsForProductionRun`, `getCurrentInventory`, `getWarehouseInventory`, and
  `getCurrentInventoryByDistributor`.

---

### ✅ What's Done Well

- **R-019 fixed completely and correctly.** All four handlers that could surface an
  `InsufficientInventoryException` now catch it — including `registerSale`, which was not
  in the original bug report but was correctly identified and fixed in the same commit. The
  corresponding controller tests now throw the real exception type (`new
  InsufficientInventoryException(999, 0)`) rather than a generic `IllegalStateException`,
  which accurately verifies the catch coverage rather than just testing that _something_ is
  caught.

- **`DistributorInventoryView.sold()` is the right abstraction.** Computing `allocated -
  current` in the domain record rather than in the template or the controller is exactly the
  "rich domain object" pattern described in CLAUDE.md. The Javadoc explains the formula, and
  the template simply calls `${inv.sold()}` — the template has no arithmetic.

- **`MovementHistoryView` pre-resolves location names.** By converting `LocationType` +
  `locationId` to a human-readable string in the controller (`formatLocation`) before passing
  to the template, the template stays free of API lookups and switch logic. The `formatLocation`
  switch expression is exhaustive (WAREHOUSE, DISTRIBUTOR, EXTERNAL) and compile-checked.

- **`buildDistributorInventories` handles the edge case.** The union of
  `allocatedByDistributor.keySet()` and `currentByDistributor.keySet()` (lines 251–254) means
  a distributor that has received returns after all their sales are cancelled — and therefore
  appears in movements but no longer in allocations — still shows up in the table rather than
  silently disappearing.

- **`<details>/<summary>` for movement history.** A clean progressive disclosure pattern. The
  page shows the inventory summary immediately and lets the user expand movement history on
  demand without a JavaScript toggle or a separate request.

- **`ProductionRunWithAllocation` extended cleanly.** Adding `warehouseInventory`,
  `distributorInventories`, and `movements` to the record is a clean, non-breaking extension.
  The Javadoc on the record and its new components explains the assembly intent.

- **TASK-023 correctly identified as already done.** Rather than re-implementing
  `getCurrentInventoryByDistributor`, the developer verified it was already in place from
  TASK-006 and updated the task log accordingly. Good discipline.

---

## Verdict

**Changes Requested.**

- **R-023** is a one-line fix — the same `.collect(Collectors.toList())` → `.toList()`
  replacement that was applied to `SaleController` in the last round.
- **R-024** asks for one additional test case in `ReleaseControllerTest` covering the
  non-empty distributor inventory path, which exercises the most complex logic added in TASK-024.

Both are small. Once they are addressed, Phase 6 is complete and the branch is ready for TASK-025/026 (Phase 7) or for a PR.
