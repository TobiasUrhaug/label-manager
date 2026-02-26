# Code Review — feature/sales-recording-distributor

**Reviewer:** Systems Reviewer
**Date:** 2026-02-18
**Spec reference:** spec.md
**Status:** Changes Requested

---

## Summary

This re-review covers four commits since the last review:

| Commit | Scope |
|--------|-------|
| `a688e2d` | Fix R-023 (full `.toList()` sweep) and R-024 (non-empty distributor inventory test) |
| `0ca3e10` | TASK-025: sales and returns history on distributor detail page |
| `b3d61cd` | TASK-026: sales history table on release detail page |
| `d2f3273` | Chore: update tasks.md and comments.md |

**Previous review items — status:**

| ID | Status | Notes |
|----|--------|-------|
| R-009 flaky ordering | 🔵 Deferred | Carried forward |
| R-010 duplicate API methods | 🔵 Deferred | Carried forward |
| R-014 V27 silent fallback UPDATE | 🔵 Deferred | Carried forward |
| R-023 `ReturnController.buildEditForm` `.collect(Collectors.toList())` | ✅ Resolved | Fixed in a full sweep: all four form helper classes updated |
| R-024 Missing non-empty distributor inventory test | ✅ Resolved | `release_populatesNonEmptyDistributorInventories` covers both the allocation+inventory path and the union edge case |
| R-025 3N queries per production run | 🔵 Deferred | Carried forward as 🟢 |
| R-026 Missing `@Transactional(readOnly = true)` in `InventoryMovementQueryApiImpl` | 🔵 Deferred | Carried forward as 🟢 |
| A-003 Primitive-heavy `recordMovement` signature | ✅ Resolved | Introduced `InventoryLocation` value object with `warehouse()`, `distributor(id)`, `external()` factory methods; updated all 5 call sites and 2 integration tests |

R-023 was fixed more broadly than required — the commit message notes the sweep covered
`EditReturnForm`, `RegisterReturnForm`, `EditSaleForm`, and `RegisterSaleForm` as well as the
originally flagged `ReturnController.buildEditForm`. The stale `Collectors` import was also
removed from each class. Clean.

R-024 is solid. The test covers a distributor with both an allocation and current inventory
(exercising `sold() = allocated - current`), plus a second distributor that appears only in
`currentByDistributor` — exactly the union edge case on lines 264–266 of `ReleaseController`
that was previously uncovered. Both assertions include concrete values, not just size checks.

TASK-026 is clean. `ReleaseSaleView` is a well-documented, purpose-built view record.
`buildReleaseSales` is an elegant flat-map pipeline. The template handles null revenue and the
empty state correctly. The controller test exercises distributor name resolution, unit
aggregation from line items, and `totalUnitsSold` computation.

One issue in TASK-025 needs to be addressed before merge: `showDistributor` does not verify
that the distributor belongs to the label in the URL, which creates a cross-tenant data
exposure on the new page.

---

## Findings

### 🔴 Must Fix (Blockers)

*None.*

---

### 🟡 Should Fix

#### R-027: `showDistributor` does not verify the distributor belongs to the label

- **File:** `src/main/java/org/omt/labelmanager/distribution/distributor/api/DistributorController.java`, lines 41–59
- **Category:** Security / Data isolation
- **Description:** The new `showDistributor` endpoint checks that the label exists and that
  the distributor exists, but it does not verify that the distributor actually belongs to the
  label in the URL path:

  ```java
  var label = labelQueryApi.findById(labelId)
          .orElseThrow(() -> new EntityNotFoundException("Label not found"));
  var distributor = distributorQueryApi.findById(distributorId)
          .orElseThrow(() -> new EntityNotFoundException("Distributor not found"));
  // ← no check that distributor.labelId().equals(labelId)
  var sales = saleQueryApi.getSalesForDistributor(distributorId);
  var returns = returnQueryApi.getReturnsForDistributor(distributorId);
  ```

  An authenticated user who knows (or guesses) the ID of a distributor belonging to another
  label can access it at `/labels/1/distributors/99`, and the controller will happily return
  that distributor's full sales and returns history. This page is new — it did not exist before
  this feature — and it's the first page in the application to expose sales revenue and returns
  data for an arbitrary distributor by ID.

  The `Distributor` domain record already carries `labelId()`, so the fix is a one-liner after
  the lookup:

  ```java
  var distributor = distributorQueryApi.findById(distributorId)
          .filter(d -> d.labelId().equals(labelId))
          .orElseThrow(() -> new EntityNotFoundException("Distributor not found"));
  ```

  A corresponding test case — verifying that accessing `/labels/1/distributors/99` where
  distributor 99 belongs to label 2 returns 404 (or equivalent) — would complete the fix.

---

### 🟢 Suggestions (Nice to Have)

#### R-009 (carry-forward): Potential flaky ordering in `QueryMovementIntegrationTest`

- **File:** `src/test/java/org/omt/labelmanager/inventory/inventorymovement/QueryMovementIntegrationTest.java`
- **Description:** Two sequential `Instant.now()` calls may produce identical timestamps on a
  fast machine or in CI, making the ordering assertion non-deterministic.
- **Suggestion:** Inject a `Clock` into `RecordMovementUseCase` and advance it between calls
  in the test, or add a small `Thread.sleep` between record operations.

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

#### R-025 (carry-forward): Three identical DB queries per production run in the inventory section

- **File:** `src/main/java/org/omt/labelmanager/catalog/release/api/ReleaseController.java`, lines 229–233
- **Description:** `getWarehouseInventory`, `getCurrentInventoryByDistributor`, and
  `getMovementsForProductionRun` each independently issue a `SELECT … FROM inventory_movement
  WHERE production_run_id = ?` for the same row. For N production runs this is 3N identical
  queries. Acceptable at current scale per the spec.
- **Suggestion:** No action required now. Note for the future.

---

#### R-026 (carry-forward): `InventoryMovementQueryApiImpl` missing `@Transactional(readOnly = true)`

- **File:** `src/main/java/org/omt/labelmanager/inventory/inventorymovement/application/InventoryMovementQueryApiImpl.java`
- **Description:** All five public methods are read-only queries with no `@Transactional`
  annotation. R-022 fixed the same gap in `DistributorReturnQueryApiImpl`; consistency
  suggests applying `@Transactional(readOnly = true)` here too.
- **Suggestion:** Add `@Transactional(readOnly = true)` to `findByProductionRunId`,
  `getMovementsForProductionRun`, `getCurrentInventory`, `getWarehouseInventory`, and
  `getCurrentInventoryByDistributor`.

---

### ✅ What's Done Well

- **R-023 swept comprehensively.** Rather than fixing only the one line flagged in the
  review, the commit updated all four `toLineItemInputs()` helper methods across
  `EditReturnForm`, `RegisterReturnForm`, `EditSaleForm`, and `RegisterSaleForm`, and removed
  the now-unused `Collectors` import from each. The fix is applied uniformly rather than
  leaving one straggler.

- **R-024 test covers the right edge case.** `release_populatesNonEmptyDistributorInventories`
  sets up exactly two distributors — one with an allocation and current inventory, one that
  appears only in `currentByDistributor` — and asserts the concrete values of both entries.
  The union path that was previously uncovered (lines 264–266 of `ReleaseController`) is now
  exercised, and the `sold() = allocated - current = -30` assertion on Beta proves the method
  handles the no-allocation case rather than silently returning 0.

- **`ReleaseSaleView` is a clean, self-documenting record.** The Javadoc on `totalRevenue`
  explicitly notes it may be null when no unit price is recorded, which is the right place to
  document that invariant. The record has no business logic and does exactly one job: carry
  pre-resolved display data to the template.

- **`buildReleaseSales` is a readable pipeline.** `flatMap → map → sorted → toList` reads as
  a clear data-enrichment flow. The comparator uses `.reversed()` rather than negating the
  comparator manually, which is the idiomatically correct form.

- **Distributor detail template aggregates units without pre-computing in the controller.**
  `${#aggregates.sum(sale.lineItems.![quantity])}` avoids adding a `totalUnits` method to
  `Sale` or computing it in the controller just for the template. The controller exposes the
  domain objects directly, which is appropriate since all the necessary data is already
  available on the object graph.

- **TASK-026 test verifies resolution end-to-end.** The test constructs a concrete `SaleLineItem`
  with a known quantity and price, asserts the resolved `distributorName`, the computed
  `totalUnits`, and the `totalRevenue.amount()`, and confirms `totalUnitsSold` on the model.
  That exercises the full enrichment path in `toReleaseSaleView` without being over-specified.

- **All controller tests pass.** `DistributorControllerTest` and `ReleaseControllerTest` green.

---

## Verdict

**Changes Requested.**

R-027 is the only remaining item. The fix is a one-line `.filter(d -> d.labelId().equals(labelId))`
guard in `showDistributor` after the distributor lookup, plus a controller test case that verifies
a distributor from a different label is treated as not found. Once that is in, the branch is
complete and ready for a PR.

---
---

# Architecture Review — Inventory, Sales & Distribution Modules

**Reviewer:** Senior System Architect
**Date:** 2026-02-26
**Scope:** DDD design, bounded context boundaries, readability, domain richness
**Status:** Advisory (non-blocking improvements for post-merge)

---

## Overall Assessment

The modular architecture is well-structured. Bounded contexts are clearly delineated, modules
communicate exclusively through public API interfaces, and the event-sourcing-lite pattern for
inventory movements is a strong design choice. The issues below are structural improvements to
pursue incrementally — none are merge blockers for the current feature.

---

## Findings

### A-001: Circular dependency between `distribution` and `sales` bounded contexts

- **Category:** Bounded context integrity
- **Severity:** High (architectural)
- **Files:**
  - `distribution/distributor/api/DistributorController.java` — imports `SaleQueryApi` and
    `DistributorReturnQueryApi` from the `sales` bounded context
  - `sales/sale/application/RegisterSaleUseCase.java` — imports `DistributorQueryApi` and
    `ChannelType` from the `distribution` bounded context
- **Description:** The dependency graph forms a cycle:
  ```
  sales → distribution  (ChannelType, DistributorQueryApi)
  distribution → sales  (SaleQueryApi, DistributorReturnQueryApi)
  ```
  Bounded contexts should have unidirectional dependencies. The cycle prevents either context
  from evolving independently and makes it impossible to extract either into a separate
  deployable unit in the future.
- **Suggestion:** The distributor detail page's sales/returns data is a **read-side concern**.
  Move the composition logic into a higher-level orchestrator — either a dedicated view service
  in `infrastructure/dashboard` or a new `DistributorDetailViewController` that sits above both
  bounded contexts. The `distribution` bounded context should have zero imports from `sales`.

  For `ChannelType`, either:
  - Move it to a shared kernel package (`org.omt.labelmanager.domain.shared`)
  - Or duplicate the enum in each bounded context that needs it (pure DDD approach)

---

### A-002: `findMostRecent` is a fragile implicit production run binding

- **Category:** Domain correctness
- **Severity:** High (data integrity risk)
- **Files:**
  - `sales/sale/application/SaleLineItemProcessor.java`, line 73
  - `sales/distributor_return/application/ReturnLineItemProcessor.java`, line 67
- **Description:** Both processors resolve which production run a sale or return applies to via
  `productionRunQueryApi.findMostRecent(releaseId, format)` — always returning the **most
  recently manufactured** pressing. This is a hidden business assumption with a concrete risk:

  1. Distributor A is allocated 100 units from pressing #1
  2. Label creates pressing #2 (a repressing)
  3. A sale is registered for Distributor A
  4. `findMostRecent` resolves to pressing #2, which has no allocation for Distributor A
  5. Sale is rejected with "No inventory allocated" even though Distributor A has 100 units

  The allocation check (`SaleLineItemProcessor` lines 82–95) partially guards against this, but
  it's confusing: the system has inventory but refuses the sale because it looked at the wrong
  pressing.
- **Suggestion:** Either:
  - Make the production run explicit on line items (the user selects which pressing they're
    selling from, or the system auto-resolves by finding the pressing that actually has an
    allocation for the distributor)
  - Or change the resolution logic to find the production run that has an active allocation for
    the given distributor, rather than the most recent one globally

---

### A-003: Primitive-heavy `recordMovement` API signature

- **Category:** Readability / API design
- **Severity:** Medium
- **File:** `inventory/inventorymovement/api/InventoryMovementCommandApi.java`, lines 32–41
- **Description:** The `recordMovement` method takes 8 parameters, including two conceptual
  pairs (`fromLocationType` + `fromLocationId`, `toLocationType` + `toLocationId`). At call
  sites it is easy to transpose `from` and `to` arguments, and the intent is not self-evident:

  ```java
  inventoryMovementCommandApi.recordMovement(
      entry.getValue(),           // productionRunId
      LocationType.DISTRIBUTOR,   // fromLocationType
      distributor.id(),           // fromLocationId
      LocationType.EXTERNAL,      // toLocationType
      null,                       // toLocationId
      entry.getKey().quantity(),   // quantity
      MovementType.SALE,          // movementType
      savedSale.getId()           // referenceId
  );
  ```

- **Suggestion:** Introduce an `InventoryLocation` value object:

  ```java
  public record InventoryLocation(LocationType type, Long id) {
      public static InventoryLocation warehouse() {
          return new InventoryLocation(WAREHOUSE, null);
      }
      public static InventoryLocation distributor(Long id) {
          return new InventoryLocation(DISTRIBUTOR, id);
      }
      public static InventoryLocation external() {
          return new InventoryLocation(EXTERNAL, null);
      }
  }
  ```

  The API then becomes:
  ```java
  void recordMovement(Long productionRunId, InventoryLocation from,
      InventoryLocation to, int quantity, MovementType type, Long referenceId);
  ```

  Call sites become self-documenting:
  ```java
  inventoryMovementCommandApi.recordMovement(
      productionRunId,
      InventoryLocation.distributor(distributor.id()),
      InventoryLocation.external(),
      quantity,
      MovementType.SALE,
      savedSale.getId()
  );
  ```

---

### A-004: Duplicate validation logic in Sale and Return line item processors

- **Category:** DRY / Maintainability
- **Severity:** Medium
- **Files:**
  - `sales/sale/application/SaleLineItemProcessor.java`
  - `sales/distributor_return/application/ReturnLineItemProcessor.java`
- **Description:** These two classes share ~70% of their logic:
  1. Validate release exists and belongs to label (identical in both)
  2. Find most recent production run for release + format (identical in both)
  3. Check current inventory at distributor (identical in both)

  The only difference: sales also check that an allocation exists (lines 82–95 in
  `SaleLineItemProcessor`). If the validation rules change (e.g., the production run resolution
  from A-002), both classes must be updated in lockstep.
- **Suggestion:** Extract an `InventoryLineItemValidator` (either in the `inventory` bounded
  context as a public API, or as a shared helper within `sales`) that handles the common
  three-step validation: release ownership → production run resolution → inventory sufficiency.
  Each processor then composes it with its specific rules (allocation check for sales, nothing
  extra for returns).

---

### A-005: Indirect distributor lookup loads all distributors to find one

- **Category:** Readability / Performance
- **Severity:** Medium
- **Files:**
  - `sales/sale/application/RegisterSaleUseCase.java`, lines 137–144
  - `sales/distributor_return/application/RegisterReturnUseCase.java`, lines 71–77
  - `sales/sale/application/UpdateSaleUseCase.java`, lines 112–119
- **Description:** To find a distributor by ID and verify label ownership, the code fetches
  **all distributors for the label** and filters in memory:

  ```java
  distributorQueryApi.findByLabelId(labelId)
      .stream()
      .filter(d -> d.id().equals(distributorId))
      .findFirst()
  ```

  This appears in three separate places. `DistributorQueryApi` already exposes
  `findById(Long)`, which returns `Optional<Distributor>` including `labelId()`.
- **Suggestion:** Use `findById` directly and verify label ownership:

  ```java
  var distributor = distributorQueryApi.findById(distributorId)
      .filter(d -> d.labelId().equals(labelId))
      .orElseThrow(() -> new EntityNotFoundException(
          "Distributor " + distributorId + " not found for label " + labelId));
  ```

  This is clearer, avoids loading unnecessary data, and matches the pattern already used in
  `DistributorController.showDistributor` after the R-027 fix.

---

### A-006: `SaleRepository` native query crosses bounded context schema boundary

- **Category:** Bounded context integrity
- **Severity:** Medium
- **File:** `sales/sale/infrastructure/SaleRepository.java`, lines 24–34
- **Description:** The `findByProductionRunIdOrderBySaleDateDesc` query uses a native SQL JOIN
  against the `inventory_movement` table:

  ```sql
  SELECT DISTINCT s.*
  FROM sale s
  JOIN inventory_movement im
    ON im.reference_id = s.id
    AND im.movement_type = 'SALE'
  WHERE im.production_run_id = :productionRunId
  ```

  This means the `sales` module's repository has a hard runtime coupling to inventory's
  database schema. If inventory's table structure or column names change, this query breaks
  silently (no compile-time detection).
- **Suggestion:** Mediate through the `InventoryMovementQueryApi`. Add a method like
  `getSaleIdsForProductionRun(Long productionRunId)` to the inventory API, then use those IDs
  to fetch sales within the sales module:

  ```java
  var saleIds = inventoryMovementQueryApi
      .getSaleIdsForProductionRun(productionRunId);
  return saleRepository.findAllById(saleIds);
  ```

  Alternatively, if performance is a concern, accept this as a pragmatic query optimization
  and document the cross-schema dependency with a prominent comment.

---

### A-007: Anemic `Sale` and `SaleLineItem` domain records

- **Category:** Domain richness
- **Severity:** Low
- **Files:**
  - `sales/sale/domain/Sale.java`
  - `sales/sale/domain/SaleLineItem.java`
- **Description:** Both records are pure data carriers with zero business logic. Contrast with
  `ProductionRun`, which has `canAllocate()` and `getAvailableQuantity()`. Missed
  opportunities:

  - The invariant "sale must have at least one line item" is validated in three separate use
    cases (`RegisterSaleUseCase`, `UpdateSaleUseCase`, and implicitly by the form). This
    belongs on the domain object.
  - `Sale` could expose `totalQuantity()` or `itemCount()` for the templates that currently
    compute it via SpEL aggregation.
  - `SaleLineItem` could compute `lineTotal` from `unitPrice * quantity` rather than having
    it pre-computed at entity-mapping time, making the derivation explicit.

- **Suggestion:** Add focused business methods where they eliminate duplication:

  ```java
  public record Sale(...) {
      public int totalQuantity() {
          return lineItems.stream().mapToInt(SaleLineItem::quantity).sum();
      }
  }
  ```

  This keeps the domain records intention-revealing without adding external dependencies.

---

### A-008: `SaleRepository` visibility should be package-private

- **Category:** Convention consistency
- **Severity:** Low
- **File:** `sales/sale/infrastructure/SaleRepository.java`, line 10
- **Description:** Per the project's modular architecture conventions, repository interfaces
  in `infrastructure/` should be package-private (no access modifier). `SaleRepository` is
  declared `public`, leaking an implementation detail outside the module. Check other
  repositories in the `sales` and `inventory` bounded contexts for the same issue.
- **Suggestion:** Remove the `public` modifier. Since all consumers are within the same
  package (use cases in `application/`), this should be a no-op change.
- **Developer note (2026-02-26):** Not applicable with the current package structure.
  `infrastructure/` and `application/` are separate Java packages
  (e.g. `…sales.sale.infrastructure` vs `…sales.sale.application`), so repositories
  *must* be `public` for the application layer to access them. This applies to all
  repositories across the codebase (`catalog`, `inventory`, `sales`, `distribution`).
  Fixing this would require flattening each module into a single package, which is a
  larger structural change outside the scope of this feature.

---

## Priority Summary

| ID | Issue | Severity | Effort |
|----|-------|----------|--------|
| A-001 | Circular `distribution` ↔ `sales` dependency | High | Medium |
| A-002 | `findMostRecent` implicit production run binding | High | Medium |
| A-003 | ~~Primitive-heavy `recordMovement` signature~~ ✅ | Medium | Small |
| A-004 | Duplicate Sale/Return line item validation | Medium | Medium |
| A-005 | Indirect distributor lookup via `findByLabelId` | Medium | Small |
| A-006 | Cross-schema native query in `SaleRepository` | Medium | Small |
| A-007 | Anemic `Sale`/`SaleLineItem` domain records | Low | Small |
| A-008 | `SaleRepository` public visibility | Low | Trivial |

**Recommended order:** A-005 and A-008 are quick wins. A-003 improves readability across many
call sites. A-001 is the most important structural issue but requires the most coordination.
A-002 should be addressed before the system has multiple production runs per release/format in
real data.
