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

## Developer Responses (Round 1)

- 🔴 **Missing integration tests for `getBandcampInventory`**: Created `InventoryMovementQueryApiImplTest.java` with two integration tests covering the specified scenarios: ALLOCATION 50 + SALE 10 → 40, and additionally RETURN 10 → 30.

- 🔴 **Missing integration tests for `getProductionRunIdsAllocatedToDistributor`**: Added two integration tests in `InventoryMovementQueryApiImplTest.java`: both run IDs returned when each has an ALLOCATION; run excluded when it has only a SALE.

- 🟡 **Misleading `locationIdMatches` comment**: Updated comment to "When no specific location ID is expected (WAREHOUSE, BANDCAMP, EXTERNAL), match by type alone."

- 🟢 **Dead code `canAllocate` / `getAvailableQuantity`**: Removed both methods from `ProductionRun.java` and deleted `ProductionRunTest.java`.

- 🟢 **`LocationType` Javadoc**: Added Bandcamp reservation and cancellation patterns to the standard movement patterns table.
