# Code Review — feature/sales-recording-distributor

**Reviewer:** Systems Reviewer
**Date:** 2026-02-17
**Spec reference:** spec.md
**Status:** Changes Requested

---

## Summary

This review covers the three commits on the branch:

| Commit | Scope |
|--------|-------|
| `812ff26` | Add `LocationType` enum |
| `4f3db36` | Phase 1 — Refactor `InventoryMovement` to bidirectional model |
| `842d619` | Phase 2 — Remove `unitsSold` from `ChannelAllocation` |

The Phase 1 and Phase 2 core implementations are **solid**. The bidirectional movement model is
well-designed, migration V25 is careful and correct, and the integration tests are comprehensive.
The one Phase 3 piece that snuck in (`RegisterSaleUseCase` validation update) is mostly
right but has two real issues that need addressing before moving forward.

One blocker, three clear should-fixes.

---

## Findings

### 🔴 Must Fix (Blockers)

#### R-001: `lineItems.getFirst()` crashes with no message on empty input
- **File:** `src/main/java/org/omt/labelmanager/sales/sale/application/RegisterSaleUseCase.java`, line 88
- **Category:** Correctness / robustness
- **Description:** The sale entity constructor call uses `lineItems.getFirst().unitPrice().currency()`
  to derive the currency. If `lineItems` is empty, this throws a bare `NoSuchElementException`
  instead of a helpful validation error. The caller (controller) has no indication of what went
  wrong. A multi-item sale where currency is mixed also silently ignores the mismatch — the
  currency from the first line item wins.
- **Suggestion:** Add an explicit guard at the top of `execute()`:
  ```java
  if (lineItems == null || lineItems.isEmpty()) {
      throw new IllegalArgumentException("Sale must contain at least one line item");
  }
  ```
  Optionally also validate that all line items share the same currency before accepting the sale.

---

### 🟡 Should Fix

#### R-002: Wrong exception type for insufficient inventory — spec requires `InsufficientInventoryException`
- **File:** `src/main/java/org/omt/labelmanager/sales/sale/application/RegisterSaleUseCase.java`, lines 216–220
- **Category:** Correctness / spec compliance
- **Description:** TASK-012 and spec section 3.4 both require throwing `InsufficientInventoryException`
  when `getCurrentInventory(productionRunId, distributorId) < requestedQuantity`. The
  implementation throws a plain `IllegalStateException` instead:
  ```java
  throw new IllegalStateException(
      "Insufficient quantity: available=" + available + ", requested=" + lineItemInput.quantity()
  );
  ```
  Callers (controller, system tests) that want to differentiate an inventory error from a
  generic business rule violation cannot do so. The test in `SaleRegistrationIntegrationTest`
  only checks for `IllegalStateException`, so it will pass today — but this is testing the
  wrong contract.
- **Suggestion:** Either reuse the existing `InsufficientInventoryException` from
  `inventory.allocation.domain` (if it belongs there generically) or, more correctly, move /
  create a shared `InsufficientInventoryException` in `org.omt.labelmanager.inventory` (the
  bounded context root) so both the allocation and sale modules can use it. Update the
  integration test assertion to check `InsufficientInventoryException`.

#### R-003: Redundant production-run lookup in step 6 — bare `.orElseThrow()` with no message
- **File:** `src/main/java/org/omt/labelmanager/sales/sale/application/RegisterSaleUseCase.java`, lines 100–103
- **Category:** Readability / robustness
- **Description:** After `validateAndAddLineItem` has already fetched and validated the
  production run for every line item (step 4), step 6 fetches it a second time:
  ```java
  var productionRun = productionRunQueryApi
          .findMostRecent(lineItemInput.releaseId(), lineItemInput.format())
          .orElseThrow();   // no message
  ```
  This is N extra DB round-trips (one per line item), and the `.orElseThrow()` with no argument
  would produce an undiagnosable `NoSuchElementException` if something went wrong between steps 4
  and 6 (e.g., a concurrent deletion). In practice this never fires, but it's bad defensive
  programming.
- **Suggestion:** Cache the `productionRunId` values during step 4 (e.g., build a
  `Map<SaleLineItemInput, Long> productionRunIds` as you iterate), then use the cached IDs in
  step 6. No second query needed and no ambiguous `orElseThrow`.

#### R-004: Per-line-item distributor lookup just to get the name for an error message
- **File:** `src/main/java/org/omt/labelmanager/sales/sale/application/RegisterSaleUseCase.java`, lines 189–196
- **Category:** Performance / readability
- **Description:** Inside `validateAndAddLineItem`, for every line item the code calls
  `distributorQueryApi.findByLabelId(labelId)` and streams through all distributors to find
  `targetDistributorId`. This is the same distributor already fully validated and resolved in
  `determineDistributor`. The only reason it's being fetched again is to get the distributor
  name for the error message on line 207. This results in one extra `findByLabelId` round-trip
  per line item, and each call loads the entire distributor list.
- **Suggestion:** Pass the distributor name (or the `Distributor` object itself) down from
  `execute()` to `validateAndAddLineItem`. `determineDistributor` can be widened to return the
  full `Distributor` rather than just the `Long` ID, since all the information is already
  fetched there.

#### R-005: `getCurrentInventoryByDistributor` computes inbound/outbound twice per distributor
- **File:** `src/main/java/org/omt/labelmanager/inventory/inventorymovement/application/InventoryMovementQueryApiImpl.java`, lines 67–81
- **Category:** Readability / minor performance
- **Description:** The `filter` step and the `toMap` value step both call `sumQuantityTo` and
  `sumQuantityFrom` for the same distributor ID. For N distributors this means 4N scans over
  the same `movements` list. Each `sumQuantityTo/From` call is an O(M) scan, so it's O(4NM)
  instead of O(2NM).
  ```java
  .filter(id -> {
      int inbound  = sumQuantityTo(...);   // 1st scan
      int outbound = sumQuantityFrom(...); // 2nd scan
      return (inbound - outbound) > 0;
  })
  .collect(Collectors.toMap(id -> id, id -> {
      int inbound  = sumQuantityTo(...);   // 3rd scan (repeated!)
      int outbound = sumQuantityFrom(...); // 4th scan (repeated!)
      return inbound - outbound;
  }));
  ```
- **Suggestion:** Compute the balance in a single pass first (e.g. collect to a
  `Map<Long, Integer>` of `distributorId → balance`), then filter out zeroes:
  ```java
  return distributorIds.stream()
          .collect(Collectors.toMap(
                  id -> id,
                  id -> sumQuantityTo(movements, LocationType.DISTRIBUTOR, id)
                          - sumQuantityFrom(movements, LocationType.DISTRIBUTOR, id)
          ))
          .entrySet().stream()
          .filter(e -> e.getValue() > 0)
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  ```

#### R-006: Mixed `@Transactional` annotation imports
- **Files:** `AllocationCommandApiImpl.java` line 4, `RegisterSaleUseCase.java` line 3 (jakarta) vs `RecordMovementUseCase.java` line 12, `DeleteMovementsUseCase.java` line 8 (Spring)
- **Category:** Correctness (subtle) / consistency
- **Description:** The branch mixes two `@Transactional` annotations:
  - `jakarta.transaction.Transactional` — in `AllocationCommandApiImpl` and `RegisterSaleUseCase`
  - `org.springframework.transaction.annotation.Transactional` — in `RecordMovementUseCase` and `DeleteMovementsUseCase`

  With Spring Boot's JPA autoconfiguration both annotations work, but they have different
  propagation semantics around `rollbackOn`. Jakarta `@Transactional` does not roll back on
  unchecked exceptions by default in all containers; Spring's annotation always rolls back on
  `RuntimeException`. In practice with Spring Boot this difference is negligible, but mixing
  them creates confusion and makes the intent harder to read.
- **Suggestion:** Standardise on `org.springframework.transaction.annotation.Transactional`
  throughout. It's the conventional choice in a Spring Boot project and has clearer rollback
  semantics.

#### R-007: `tasks.md` not updated — all tasks still marked `[ ] To do`
- **File:** `.claude/features/sales-recording-distributor/tasks.md`
- **Category:** Workflow
- **Description:** Three commits completing Phase 1 and Phase 2 have landed, but every task in
  `tasks.md` is still `[ ] To do`. This breaks the ability to use the task list as a progress
  tracker and makes it unclear exactly which tasks the next developer (or reviewer) can depend on.
- **Suggestion:** Mark TASK-001 through TASK-009 as `[x] Done`. TASK-012 (RegisterSaleUseCase
  movement validation) is partially done — mark it `[~] In progress` with a note that the
  `InsufficientInventoryException` type and the `distributorId` persistence are outstanding.

---

### 🟢 Suggestions (Nice to Have)

#### R-008: Fully-qualified `java.util.stream.Stream.builder()` inside a lambda — add import
- **File:** `src/main/java/org/omt/labelmanager/inventory/inventorymovement/application/InventoryMovementQueryApiImpl.java`, lines 54–63
- **Description:** The implementation uses the fully-qualified class name inline:
  ```java
  java.util.stream.Stream.Builder<Long> ids = java.util.stream.Stream.builder();
  ```
  This is the only place in the class where this is done. The imports section already pulls in
  `java.util.stream.Collectors` — `Stream` is just missing. It's a minor readability issue.
- **Suggestion:** Add `import java.util.stream.Stream;` and use `Stream.builder()`.

#### R-009: Potential flaky ordering test in `QueryMovementIntegrationTest`
- **File:** `src/test/java/org/omt/labelmanager/inventory/inventorymovement/QueryMovementIntegrationTest.java`, lines 136–145
- **Description:** `getMovementsForProductionRun_returnsMovementsNewestFirst` relies on
  `recordAllocation` and `recordSale` being given distinct `Instant.now()` values so that the
  sort order is deterministic. On a fast machine (or in CI with frozen clocks), two sequential
  `Instant.now()` calls within the same millisecond will produce equal timestamps, and the
  sort order becomes arbitrary. This could produce a spurious failure.
- **Suggestion:** Either add a `Thread.sleep(1)` between the two record calls (simple but
  fragile), or — better — inject a controllable `Clock` into `RecordMovementUseCase` and set
  distinct instants in tests.

#### R-010: `findByProductionRunId` and `getMovementsForProductionRun` are duplicates on the public API
- **File:** `src/main/java/org/omt/labelmanager/inventory/inventorymovement/api/InventoryMovementQueryApi.java`, lines 19 and 29
- **Description:** The API exposes two methods that return identical results. The Javadoc on
  `getMovementsForProductionRun` says it is an "alias" for `findByProductionRunId`. Two names
  for one operation makes callers unsure which to use and forces implementors to maintain both
  forever. The spec keeps `findByProductionRunId` as "existing" and adds
  `getMovementsForProductionRun` as new, but there's no reason both need to live on the
  public contract.
- **Suggestion:** Deprecate `findByProductionRunId` in the interface Javadoc
  (`@deprecated use getMovementsForProductionRun`) and migrate internal callers. Remove it
  in a later cleanup commit.

---

### ✅ What's Done Well

- **Migration V25 is excellent.** The sequence — add columns nullable, backfill existing rows
  with correct data, then add NOT NULL constraints, drop the old index *before* dropping the
  column, then create new indexes — is textbook. No data is at risk and the migration is
  re-runnable-safe with `DROP INDEX IF EXISTS`.

- **Bidirectional movement model design.** The `(fromLocationType, fromLocationId,
  toLocationType, toLocationId, quantity)` model is significantly cleaner than the signed
  `quantityDelta` approach. All quantities are positive, direction is explicit, and the
  standard patterns table in the Javadoc makes the API instantly understandable.

- **Side-effect encapsulation in `AllocationCommandApiImpl`.** Recording the ALLOCATION
  movement inside `createAllocation` (not left to callers) is exactly the pattern the CLAUDE.md
  prescribes. The allocation ID is correctly used as `referenceId`, making movements traceable
  to their source.

- **`DeleteMovementsUseCase` and `RecordMovementUseCase` are focused and correct.** Each does
  exactly one thing. `DeleteMovementsUseCase` is properly `@Transactional` and delegates to the
  repository derived query — no logic to get wrong.

- **Movement recording AFTER save in `RegisterSaleUseCase` (step 6).** Recording SALE movements
  after `saleRepository.save()` returns a real `saleId` as `referenceId` is the correct design.
  The whole method is `@Transactional`, so a failure in step 6 rolls back the entire sale.

- **Integration test coverage.** `SaleRegistrationIntegrationTest` covers all the important
  cases: happy path, insufficient inventory, wrong label, no production run, no allocation,
  channel-type mismatch, missing distributor ID. `QueryMovementIntegrationTest` covers all four
  query methods with meaningful assertions, including the zero-inventory exclusion.
  `RecordMovementIntegrationTest` directly verifies the delete-by-reference semantics.

- **`InventoryMovementPersistenceIntegrationTest.deletesMovementWhenProductionRunDeleted`.**
  Testing the cascade-delete behaviour via the production run is a smart edge case to cover —
  it validates the DB schema constraint, not just the Java layer.

---

## Verdict

**Changes Requested.** Address R-001 (empty-list crash) and R-002 (wrong exception type) before
proceeding to Phase 3. R-003 and R-004 should be fixed at the same time since they're both in
`RegisterSaleUseCase` and Phase 3 will touch that file anyway. R-006 and R-007 are quick wins.
