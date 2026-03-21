# Review Comments: label-distributor-allocation

## Status
In Review

## Scope
Round 1 covers tasks 1.1, 2.1, 2.2, and 3.1.

---

## Review Round 1

### 🔴 Must Fix

- [x] **Missing integration tests for task 2.1 (`getBandcampInventory`)** — Resolved. Two integration tests added in `InventoryMovementQueryApiImplTest`: ALLOCATION 50 + SALE 10 → 40, and ALLOCATION 50 + SALE 10 + RETURN 10 → 30. Both match the spec exactly.

- [x] **Missing integration tests for task 2.2 (`getProductionRunIdsAllocatedToDistributor`)** — Resolved. Two integration tests added: both run IDs returned when each has an ALLOCATION; empty result when run has only a SALE.

### 🟡 Should Fix

- [x] **`InventoryMovementQueryApiImpl.java:122` — misleading comment on `locationIdMatches`** — Resolved. Comment updated to "When no specific location ID is expected (WAREHOUSE, BANDCAMP, EXTERNAL), match by type alone."

### 🟢 Suggestions

- [x] **`ProductionRun.java:30,35` — dead code after 3.1 rewrite** — Resolved. `canAllocate()` and `getAvailableQuantity()` removed from `ProductionRun`; `ProductionRunTest.java` deleted.

- [x] **`LocationType.java` — Javadoc standard movement patterns table doesn't cover BANDCAMP** — Resolved. Bandcamp reservation and cancellation patterns added to the movement patterns table.

### NFR Checks

- **NFR-1 (no negative stock):** `validateQuantityIsAvailable` correctly computes `productionRun.quantity() + getWarehouseInventory()`, which accounts for all outflows including Bandcamp reservations. Available stock cannot go negative if the check is applied before recording each movement. ✅
- **NFR-2 (audit trail):** All stock changes continue to be recorded as `inventory_movement` rows. No change to this path in the tasks reviewed. ✅
- **NFR-3 (safe migration):** Not yet in scope for this review round (task 11.1 not started). ⏳

---

---

## Review Round 2

All Round 1 comments resolved. One new observation:

### 🟢 Suggestions

- [ ] **`LocationType.java:11-17` — `<ul>` in class-level Javadoc omits BANDCAMP**
  The `<ul>` listing enum values (WAREHOUSE, DISTRIBUTOR, EXTERNAL) was not updated. BANDCAMP is documented via its own enum-level Javadoc, so this is not a correctness issue — just an inconsistency. Worth fixing in a later pass or alongside another `LocationType` edit.

All 🔴 items resolved. Feature is clear to continue to task 4.1.

---

## Review Round 3

Scope: task 4.1 — remove `AllocationQueryApi` from `SaleLineItemProcessor`.

### 🔴 Must Fix

None.

### 🟡 Should Fix

- [ ] **`SaleLineItemProcessor.java` — `distributorName` parameter is now unused**
  After removing the `hasAllocation` guard (the only consumer of `distributorName`), the parameter is dead weight. More importantly, `UpdateSaleUseCase.java:77` calls `distributorQueryApi.findById()` via `resolveDistributorName()` solely to populate this argument — that is now a wasted DB query per line item. Remove `distributorName` from `validateAndAdd()`'s signature and update all three call sites (`RegisterSaleUseCase`, `UpdateSaleUseCase`) plus the unit test.

### 🟢 Suggestions

- [ ] **`tasks.md` — Task 3.1 still marked `[ ]` (unchecked)** despite being approved in Round 2. Update to `[x]`.

- [ ] **`SaleLineItemProcessorTest` — only the error path is covered** The single test asserts `InsufficientInventoryException` on zero stock, which is exactly what the spec required. A complementary happy-path test (sufficient stock → line item added to entity, method returns production run ID) would document the success contract at the unit level. Integration tests cover this transitively, so this is discretionary.

---

## Review Round 4 (Round 3 re-review)

### 🟡
- [x] **`distributorName` unused parameter** — Resolved. Parameter removed from `validateAndAdd`, `UpdateSaleUseCase` cleaned up (`resolveDistributorName` and `distributorQueryApi` injection both removed). Wasted DB query eliminated. ✅

### 🟢
- [x] **`tasks.md` task 3.1 unchecked** — Resolved. Now `[x]`. ✅
- [x] **Happy-path unit test** — Resolved. `validateAndAdd_addsLineItemAndReturnsProductionRunId_whenStockIsSufficient` added. ✅

### New observation

- [ ] **`SaleLineItemProcessor.java:46` — stale `@param distributorName` in Javadoc**
  The `@param distributorName` entry remains in the `validateAndAdd` Javadoc but the parameter no longer exists. Remove that line from the doc.

All 🔴 resolved. Feature is clear to continue to task 5.1.

---

## Developer Responses (Round 4)

- 🟢 **Stale `@param distributorName` Javadoc**: Removed the `@param distributorName` line from `validateAndAdd` Javadoc.

---

## Developer Responses (Round 3)

- 🟡 **`distributorName` unused parameter**: Removed `distributorName` from `SaleLineItemProcessor.validateAndAdd()`. Updated `RegisterSaleUseCase` (removed `distributor.name()` from the call). Removed `resolveDistributorName()`, the `distributorQueryApi` field, and the constructor arg from `UpdateSaleUseCase` — the wasted DB query is gone. Updated `SaleLineItemProcessorTest` to match the new 4-arg signature. All 365 tests pass.

- 🟢 **`tasks.md` task 3.1 unchecked**: Updated to `[x]`.

- 🟢 **Happy-path unit test**: Added `validateAndAdd_addsLineItemAndReturnsProductionRunId_whenStockIsSufficient` to `SaleLineItemProcessorTest`. Asserts the production run ID is returned and `saleEntity.addLineItem()` is called.

---

## Developer Responses (Round 1)

- 🔴 **Missing integration tests for `getBandcampInventory`**: Created `InventoryMovementQueryApiImplTest.java` with two integration tests covering the specified scenarios: ALLOCATION 50 + SALE 10 → 40, and additionally RETURN 10 → 30.

- 🔴 **Missing integration tests for `getProductionRunIdsAllocatedToDistributor`**: Added two integration tests in `InventoryMovementQueryApiImplTest.java`: both run IDs returned when each has an ALLOCATION; run excluded when it has only a SALE.

- 🟡 **Misleading `locationIdMatches` comment**: Updated comment to "When no specific location ID is expected (WAREHOUSE, BANDCAMP, EXTERNAL), match by type alone."

- 🟢 **Dead code `canAllocate` / `getAvailableQuantity`**: Removed both methods from `ProductionRun.java` and deleted `ProductionRunTest.java`.

- 🟢 **`LocationType` Javadoc**: Added Bandcamp reservation and cancellation patterns to the standard movement patterns table.
