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

## Review Round 7

Scope: tasks 7.1, 7.2, 7.3 — `ProductionRunWithAllocation`, `DistributorInventoryView`, `AllocationView` deletion, plus cascading changes to `ReleaseController`, `ReleaseControllerTest`, and `release.html`.

### 🔴 Must Fix

- [x] **`ReleaseController.java:209` — `warehouseInventory` stores the raw movement delta, not absolute available stock** — Resolved. Line 209 now reads `run.quantity() + inventoryMovementQueryApi.getWarehouseInventory(run.id())`. Test assertion updated to 700. ✅
  `buildProductionRunWithAllocation` assigns `warehouseInventory = inventoryMovementQueryApi.getWarehouseInventory(run.id())`. `getWarehouseInventory` returns a net delta (negative after allocations). For a run of 500 units with 200 allocated, the stored value is `−200`, but the template renders it directly at `runWithAlloc.warehouseInventory()` — the user sees `−200` instead of `300`. The spec is explicit: "`warehouseInventory` stored in this record should be absolute available stock: `productionRun.quantity() + getWarehouseInventory()`." Fix: change line 209 to `run.quantity() + inventoryMovementQueryApi.getWarehouseInventory(run.id())`. The test in `ReleaseControllerTest` (`release_populatesInventoryDataInProductionRuns`, asserts `warehouseInventory() == 200`) must be updated to assert `700` (run.quantity=500, delta=200).

### 🟡 Should Fix

- [x] **`tasks.md:76,78` — tasks 8.1 and 8.2 implemented but not checked off** — Resolved. Both marked `[x]`. ✅

- [x] **`ReleaseControllerTest.java` — no assertion on `bandcampInventory()`** — Resolved. Stub `getBandcampInventory(10L) → 25` added; `bandcampInventory() == 25` asserted. ✅

### 🟢 Suggestions

- [ ] **`ReleaseController.java:230` — zero-stock distributors no longer shown**
  The old `buildDistributorInventories` unioned allocation records with movement records, so distributors with zero current stock (sold all units) still appeared in the table. The new version only includes entries in `currentByDistributor`. Verify whether `getCurrentInventoryByDistributor` returns zero-valued entries or filters them. If it filters them, a distributor who sold all their stock disappears from the table — which is arguably correct per AC-5.1 ("current stock"), but worth a conscious decision. No change required if intentional.

### NFR Checks

- **NFR-1 (no negative stock):** 🔴 fix applied — `warehouseInventory` now shows absolute available stock. UI display is correct. ✅
- **NFR-2 (audit trail):** Unchanged — all stock changes still recorded as movements. ✅
- **NFR-3 (safe migration):** Task 11.1 not yet started. ⏳

---

## Developer Responses (Round 7)

- 🔴 **`warehouseInventory` raw delta**: Fixed `buildProductionRunWithAllocation` — line 209 now computes `run.quantity() + inventoryMovementQueryApi.getWarehouseInventory(run.id())`. For a run of 500 with delta 200, the stored value is now 700, not 200.
- 🟡 **Tasks 8.1 and 8.2 unchecked**: Both marked `[x]` in `tasks.md`.
- 🟡 **`bandcampInventory` assertion missing**: Added `when(inventoryMovementQueryApi.getBandcampInventory(10L)).thenReturn(25)` stub and `assertThat(productionRuns.get(0).bandcampInventory()).isEqualTo(25)` assertion to `release_populatesInventoryDataInProductionRuns`. Also updated `warehouseInventory` assertion from 200 to 700. All tests pass.

---

## Developer Responses (Round 6)

- 🟡 **null `locationType` guard**: Added `if (form.getLocationType() == null)` check in `AllocateController.allocate()` before the distributor-specific guard — flashes `"A location type must be selected"` and redirects. Added test `allocate_withNullLocationType_flashesAllocationError`. All 9 `AllocateControllerTest` tests pass.

---

## Developer Responses (Round 5)

- 🟡 **No `quantity > 0` guard**: Added guard at the top of both `allocate()` and `cancelBandcampReservation()` — flashes an error and redirects immediately when `quantity ≤ 0`. Added corresponding tests: `allocate_withZeroQuantity_flashesAllocationError` and `cancelBandcampReservation_withZeroQuantity_flashesCancellationError`.

- 🟡 **`resolveToLocation` with null `distributorId`**: Added guard in `allocate()` — if `locationType == DISTRIBUTOR && distributorId == null`, flashes `"A distributor must be selected"` and redirects. Added test: `allocate_distributorWithoutDistributorId_flashesAllocationError`. All 8 `AllocateControllerTest` tests pass.

---

## Review Round 6

Scope: Group 6 in full — `AllocateForm` (6.1), `CancelBandcampReservationForm` (6.2), `AllocateController` + use cases (6.3), `AllocateControllerTest`, `AllocateUseCaseTest`, `CancelBandcampReservationUseCaseTest`.

Note: the spec described the controller flow directly; this was superseded by an explicit architectural decision to extract `AllocateUseCase` and `CancelBandcampReservationUseCase`. Reviewed against the intent of the requirements, not the original spec flow.

### 🔴 Must Fix

None.

### 🟡 Should Fix

- [ ] **`AllocateController.java:36` — null `locationType` bypasses both guards**
  If a POST arrives without a `locationType` parameter (or with an unrecognised value that Spring fails to bind), `form.getLocationType()` is `null`. The guard on line 36 checks `== LocationType.DISTRIBUTOR`, which is `false` for `null`, so it does not fire. `resolveToLocation` then falls through to `InventoryLocation.distributor(form.getDistributorId())` — and if `distributorId` is also absent, a movement with `toLocationType=DISTRIBUTOR, toLocationId=null` is persisted, corrupting warehouse calculations (same root cause as the Round 5 fix, different trigger). Add an explicit guard before line 36: `if (form.getLocationType() == null) { flash error; return; }`.

### 🟢 Suggestions

- [ ] **`AllocateUseCase.java` — no success-path log**
  `AllocateUseCase` logs only on the rejection path. `CreateProductionRunUseCase` logs at `info` on success. Consider adding `log.info("Allocated {} units from run {} to {}", quantity, productionRunId, toLocation)` after `recordMovement` for operational visibility.

- [ ] **`AllocateUseCaseTest` — no test for production run not found**
  `AllocateUseCase.execute()` throws `IllegalArgumentException` when the production run does not exist. This path is untested. Add a test: `execute_throwsIllegalArgumentException_whenProductionRunNotFound`.

- [ ] **`ProductionRunQueryApi.validateQuantityIsAvailable` — schedule removal at task 10.1**
  After `AllocateUseCase` was introduced, the stock-check logic that lived in `validateQuantityIsAvailable` now lives in the use case. `validateQuantityIsAvailable` is still on the public API because `AllocationCommandApiImpl` calls it — but `AllocationCommandApiImpl` is deleted in task 10.1. At that point, `validateQuantityIsAvailable` should be removed from `ProductionRunQueryApi`, `ProductionRunQueryApiImpl`, and `ProductionRunQueryApiImplTest` to eliminate the duplication.

### NFR Checks

- **NFR-1 (no negative stock):** Both use cases are `@Transactional` — validate and record are now atomic, eliminating the TOCTOU race that existed when the controller called them separately. ✅
- **NFR-2 (audit trail):** All movements still go through `inventoryMovementCommandApi.recordMovement`. ✅
- **NFR-3 (safe migration):** Task 11.1 not yet started. ⏳

---

## Developer Responses (Round 4)

- 🟢 **Stale `@param distributorName` Javadoc**: Removed the `@param distributorName` line from `validateAndAdd` Javadoc.

---

## Review Round 5

Scope: task 5.1 (AgreementController), 6.1 (AllocateForm), 6.2 (CancelBandcampReservationForm), 6.3 (AllocateController), 10.2 (AllocationControllerTest deleted).

### Re-confirm Round 4 observation

- [x] **`SaleLineItemProcessor.java:46` — stale `@param distributorName`** — Confirmed resolved. Current Javadoc at line 39–48 has no `@param distributorName`. ✅

### 🔴 Must Fix

None.

### 🟡 Should Fix

- [ ] **`AllocateController.java:43` — no guard against `quantity ≤ 0` (allocate endpoint)**
  `validateQuantityIsAvailable(runId, quantity)` checks `quantity > available`. With `quantity = 0` the check always passes (0 ≤ any non-negative available). With a negative quantity (e.g. `-10`), it also passes and creates an ALLOCATION movement with −10 units — warehouse outbound becomes negative, inflating available stock by 10. Add a guard at the top of the handler: `if (form.getQuantity() <= 0) { redirectAttributes.addFlashAttribute("allocationError", "Quantity must be greater than zero"); return redirect; }`. Same guard needed on `cancelBandcampReservation` — a zero or negative `CancelBandcampReservationForm.quantity` passes the `quantity > held` check and records a spurious RETURN movement.

- [ ] **`AllocateController.java:85` — `resolveToLocation` creates `InventoryLocation.distributor(null)` when `locationType == DISTRIBUTOR` and `distributorId` is missing**
  If a request arrives with `locationType=DISTRIBUTOR` but no `distributorId` (e.g. JS disabled, or direct POST), `resolveToLocation` returns `InventoryLocation.distributor(null)`. The movement is then persisted with `toLocationType = DISTRIBUTOR, toLocationId = null`. `getCurrentInventoryByDistributor` silently drops this null-ID entry, but the warehouse-outbound calculation includes it, permanently inflating warehouse available stock. Add a server-side guard before calling `resolveToLocation`: if `locationType == DISTRIBUTOR && distributorId == null`, flash an error and redirect.

### 🟢 Suggestions

- [ ] **Round 2 carry-over — `LocationType.java:11-17` — `<ul>` still omits BANDCAMP**
  The class-level Javadoc lists WAREHOUSE, DISTRIBUTOR, and EXTERNAL but not BANDCAMP. Minor inconsistency; no correctness impact.

### NFR Checks

- **NFR-1 (no negative stock):** `validateQuantityIsAvailable` correctly guards distributor and Bandcamp allocations. However, the missing `quantity > 0` guard (see 🟡 above) means a negative quantity bypasses the check and can silently inflate available stock. ⚠️ Pending 🟡 fix.
- **NFR-2 (audit trail):** All movements go through `inventoryMovementCommandApi.recordMovement`. No direct DB writes in the new controller. ✅
- **NFR-3 (safe migration):** Task 11.1 not yet started. ⏳

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
