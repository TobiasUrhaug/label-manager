# Code Review — feature/sales-recording-distributor

**Reviewer:** Systems Reviewer
**Date:** 2026-02-17
**Spec reference:** spec.md
**Status:** Changes Requested

---

## Summary

This is a re-review covering the two Phase 4 commits (TASK-014 and TASK-015):

| Commit | Scope |
|--------|-------|
| `6c892ca` | Implement `UpdateSaleUseCase` and `DeleteSaleUseCase`; extract `SaleLineItemProcessor` and `SaleConverter` |
| `a1eaccc` | `SaleDeleteIntegrationTest` |

**Previous review items — status:**

| ID | Status | Notes |
|----|--------|-------|
| R-001 empty-list crash | ✅ Resolved | |
| R-002 wrong exception type | ✅ Resolved | |
| R-003 redundant production run lookup | ✅ Resolved | |
| R-004 per-line-item distributor re-lookup | ✅ Resolved | |
| R-005 double scan in `getCurrentInventoryByDistributor` | ✅ Resolved | |
| R-006 mixed `@Transactional` imports | ✅ Resolved | Spring annotation throughout |
| R-007 `tasks.md` not updated | ✅ Resolved | |
| R-008 fully-qualified `Stream` usage | ✅ Resolved | |
| R-009 flaky ordering in `QueryMovementIntegrationTest` | 🔵 Deferred | Carried forward |
| R-010 duplicate API methods | 🔵 Deferred | Carried forward |
| R-011 `getSalesForProductionRun` wrong join | ✅ Resolved | Now joins through `inventory_movement`; repressing test added |
| R-012 test module boundary violations | ✅ Resolved | `DistributorTestHelper` introduced and used throughout |
| R-013 missing ordering test for `getSalesForProductionRun` | ✅ Resolved | Test added |
| R-014 V27 silent fallback UPDATE | 🔵 Deferred | Not addressed; carried forward |
| R-015 updateSale omits distributorId/channel | ✅ Resolved | Documented as intentionally immutable (Javadoc on interface + use case) |
| R-016 DeleteSaleUseCase missing existence check | ✅ Resolved | Added existsById check + EntityNotFoundException + new test |

The implementation shows strong design thinking. Extracting `SaleLineItemProcessor` and
`SaleConverter` as shared application-layer components cleanly eliminates the duplication
that would otherwise have existed between `RegisterSaleUseCase`, `UpdateSaleUseCase`, and
`SaleQueryApiImpl`. The inventory restoration logic — deleting old movements before
re-validating new quantities — is implemented correctly and specifically tested. Both use
cases follow the established patterns (package-private `@Service`, Spring `@Transactional`).

Two items need attention before TASK-016 builds on this: a spec deviation in the `updateSale`
API signature that will constrain the edit form design, and a missing existence check in
`DeleteSaleUseCase` that could produce confusing behaviour at the HTTP layer.

---

## Findings

### 🔴 Must Fix (Blockers)

_None._

---

### 🟡 Should Fix

#### R-015: `updateSale` omits `distributorId` and `channel` from the spec contract

- **File:** `src/main/java/org/omt/labelmanager/sales/sale/api/SaleCommandApi.java`, lines 46–51
- **Category:** Spec deviation
- **Description:** The spec (section 4, API Contracts) defines `updateSale` as:

  ```java
  void updateSale(
      Long saleId, LocalDate saleDate, ChannelType channel,
      String notes, Long distributorId, List<SaleLineItemInput> lineItems
  );
  ```

  The current implementation accepts only `saleId`, `saleDate`, `notes`, and `lineItems`.
  This means the distributor and channel type on a sale are permanently fixed at registration
  time — the edit form (TASK-016) will have no way to change them.

  Whether this is intentional or not matters: if a user attributed a sale to the wrong
  distributor, they cannot correct it without deleting and re-creating the sale. If the
  deliberate limitation is acceptable, it needs to be documented; if not, the parameters
  need to be added.

  Note: returning `Sale` instead of `void` (as the spec says) is a _good_ deviation —
  it makes the caller's life easier and the method more consistent with `registerSale`.
  That part is fine.

- **Suggestion:** Decide on the correct behaviour, then do one of:
  1. **Accept the simplification:** Add a Javadoc note to `updateSale` (and to the use case)
     explaining that the distributor and channel are immutable after creation and why.
  2. **Implement the full spec:** Add `channel` and `distributorId` to the signature.
     `UpdateSaleUseCase` would need to re-run the `determineDistributor` logic from
     `RegisterSaleUseCase` (which could be extracted into a shared helper), update the
     entity's `distributorId`, and use the new distributor when recording movements.

  Either path is fine, but it must be resolved before TASK-016 designs the edit form.

---

#### R-016: `DeleteSaleUseCase` does not verify the sale exists

- **File:** `src/main/java/org/omt/labelmanager/sales/sale/application/DeleteSaleUseCase.java`, lines 28–36
- **Category:** Robustness / Consistency
- **Description:** `DeleteSaleUseCase.execute(Long saleId)` calls
  `deleteMovementsByReference(SALE, saleId)` then `saleRepository.deleteById(saleId)`
  without first checking whether the sale exists. Compare this to `UpdateSaleUseCase`, which
  begins with:

  ```java
  var saleEntity = saleRepository.findById(saleId)
          .orElseThrow(() -> new EntityNotFoundException("Sale not found: " + saleId));
  ```

  The inconsistency creates two problems:

  1. **Silent success on double-delete.** In Spring Data JPA 3.x+, `deleteById` is a no-op
     when the entity is not found. A caller that tries to delete an already-deleted (or
     never-existing) sale gets no feedback. The TASK-016 controller will need to handle
     the "sale not found" case, and a silent no-op makes that harder.
  2. **Confusing movement deletion.** If `saleId` is garbage, `deleteMovementsByReference`
     runs against non-existent reference data. It deletes nothing, so there is no data
     corruption — but the sequence of a successful `deleteMovementsByReference` followed by
     a silent `deleteById` no-op is not what the spec describes ("1. Load existing sale.").

- **Suggestion:** Load the entity first, consistent with the update use case:

  ```java
  @Transactional
  public void execute(Long saleId) {
      log.info("Deleting sale {}", saleId);

      if (!saleRepository.existsById(saleId)) {
          throw new EntityNotFoundException("Sale not found: " + saleId);
      }

      inventoryMovementCommandApi.deleteMovementsByReference(MovementType.SALE, saleId);
      saleRepository.deleteById(saleId);

      log.info("Sale {} deleted successfully", saleId);
  }
  ```

  Add a test case `deleteSale_withNonExistentSale_throwsException` to
  `SaleDeleteIntegrationTest`.

---

### 🟢 Suggestions (Nice to Have)

#### R-009 (carry-forward): Potential flaky ordering in `QueryMovementIntegrationTest`

- **File:** `src/test/java/org/omt/labelmanager/inventory/inventorymovement/QueryMovementIntegrationTest.java`
- **Description:** Same as previous review. Two sequential `Instant.now()` calls may produce
  identical timestamps on a fast machine or in CI.
- **Suggestion:** Inject a `Clock` into `RecordMovementUseCase` and set distinct instants in
  the test, or add a `Thread.sleep(10)` between record calls.

---

#### R-010 (carry-forward): `findByProductionRunId` and `getMovementsForProductionRun` are duplicates

- **File:** `src/main/java/org/omt/labelmanager/inventory/inventorymovement/api/InventoryMovementQueryApi.java`
- **Description:** Two names for one operation on the public API.
- **Suggestion:** Deprecate `findByProductionRunId` and migrate callers over time.

---

#### R-014 (carry-forward): V27 fallback UPDATE could silently corrupt data in production

- **File:** `src/main/resources/db/migration/V27__add_distributor_id_to_sale.sql`
- **Description:** Same as previous review. The third UPDATE silently assigns the DIRECT
  distributor to any sale still lacking a `distributor_id` after the first two passes,
  with no warning that this should affect zero rows in production.
- **Suggestion:** Add an inline comment warning that the fallback should affect zero rows in
  production.

---

#### R-017: `SaleEditIntegrationTest` only covers DIRECT-channel sales

- **File:** `src/test/java/org/omt/labelmanager/sales/sale/SaleEditIntegrationTest.java`
- **Description:** All four tests in `SaleEditIntegrationTest` use `ChannelType.DIRECT` and
  `directDistributorId`. `UpdateSaleUseCase` reads the distributor ID from the persisted
  entity — the same logic applies regardless of channel, so the tests pass. But a
  DISTRIBUTOR-channel sale goes through a different code path in `RegisterSaleUseCase`
  (`determineDistributor` returns a non-DIRECT distributor), and that state is what the
  update use case inherits.

  Having at least one test covering a DISTRIBUTOR-channel sale being edited would confirm the
  distributor is correctly preserved on update and the DISTRIBUTOR-sourced movements are
  correctly replaced.

- **Suggestion:** Add a fifth test (or expand `SaleQueryIntegrationTest`'s setup which
  already has `externalDistributorId`) that:
  1. Registers a DISTRIBUTOR-channel sale.
  2. Edits the line item quantity.
  3. Asserts the updated inventory uses the external distributor's balance.

---

#### R-018: "No allocation" error path untested in the edit flow

- **File:** `src/main/java/org/omt/labelmanager/sales/sale/application/SaleLineItemProcessor.java`, lines 82–95
- **Description:** `SaleLineItemProcessor.validateAndAdd` throws `IllegalStateException` when
  no allocation exists for the distributor. This path is shared by both register and update
  flows. Integration tests exist for the register flow (from earlier phases), but neither
  `SaleEditIntegrationTest` nor `SaleDeleteIntegrationTest` tests the scenario where a sale
  is edited to include a line item with no backing allocation.

  In practice, existing sales will have an allocation (the original registration validated it),
  so this can only happen if the allocation is somehow removed between registration and edit —
  an edge case, but the error path is there and untested.

- **Suggestion:** A single test in `SaleEditIntegrationTest` would be sufficient:
  create a sale for release A, then edit it to include a new release B that has no
  allocation, and assert that `IllegalStateException` is thrown. This also serves as
  documentation of the intended error behaviour.

---

### ✅ What's Done Well

- **`SaleLineItemProcessor` and `SaleConverter` extraction is excellent.** Pulling shared
  validation-and-mutation logic into `SaleLineItemProcessor` and entity→domain mapping into
  `SaleConverter` eliminates the code duplication that would otherwise have existed across
  `RegisterSaleUseCase`, `UpdateSaleUseCase`, and `SaleQueryApiImpl`. Both are correctly
  package-private `@Service`s — visible only within the `application` sub-package, not to
  callers outside the module.

- **Inventory restoration logic is correctly ordered and tested.** `UpdateSaleUseCase` deletes
  old movements _before_ calling `lineItemProcessor.validateAndAdd`, which means
  `getCurrentInventory` sees the full restored balance during re-validation. This is the
  subtle-but-correct order, and `updateSale_restoresInventoryBeforeValidating_allowingLargerQuantity`
  specifically tests it with a quantity that would fail if the order were wrong (selling 75
  when only 70 remain — succeeds because the old 10-unit sale is reversed first).

- **`SaleDeleteIntegrationTest` tests three orthogonal properties.** Rather than one
  monolithic test, the three tests check: (1) the entity is gone, (2) current inventory is
  restored, (3) the SALE-type movements with the matching referenceId are specifically
  removed. Each assertion stands alone and would catch a different class of bug.

- **`UpdateSaleUseCase.execute()` is clearly commented.** The numbered step comments match
  the spec's delete-then-re-record flow and make the intent obvious to a future reader.

- **`SaleCommandApiImpl` wiring is clean.** Three use cases, three delegating methods,
  no logic — exactly the intended pattern.

- **Test helpers are used correctly throughout both new test classes.** No repository
  injection from other modules; `LabelTestHelper`, `ReleaseTestHelper`,
  `ProductionRunTestHelper`, and `DistributorQueryApi` (via the label helper's
  `createLabelWithDirectDistributor`) provide all necessary fixtures.

---

## Verdict

**Changes Requested.**

- R-015 (`updateSale` spec deviation) is the higher-priority item: TASK-016 will design the
  edit form against this API. The decision — immutable distributor/channel, or editable —
  must be made and reflected in the interface before that task begins.

- R-016 (`DeleteSaleUseCase` missing existence check) is a one-method fix with one new test.
  Do it in the same pass.

Both are contained within the `sales/sale` module. Once resolved, the implementation is ready
for TASK-016.
