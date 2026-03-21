# Tasks: label-distributor-allocation

## Status
Draft

## Tasks

### Group 1 — Domain model: add BANDCAMP

- [x] **1.1** Add `BANDCAMP` to `LocationType` enum and `bandcamp()` factory to `InventoryLocation`.
  - Files: `inventory/domain/LocationType.java`, `inventory/domain/InventoryLocation.java`
  - Test: add a unit test asserting `InventoryLocation.bandcamp()` has type `BANDCAMP` and null id.

### Group 2 — InventoryMovement query API extensions

- [x] **2.1** Add `getBandcampInventory(Long productionRunId)` to `InventoryMovementQueryApi` and implement in `InventoryMovementQueryApiImpl`.
  - Implementation: `sumQuantityTo(movements, BANDCAMP, null) - sumQuantityFrom(movements, BANDCAMP, null)` — same pattern as `getWarehouseInventory()`.
  - Test (integration): given ALLOCATION 50 to Bandcamp + SALE 10 from Bandcamp → `getBandcampInventory()` = 40. Given additional RETURN 10 from Bandcamp → result = 30.

- [x] **2.2** Add `getProductionRunIdsAllocatedToDistributor(Long distributorId)` to `InventoryMovementQueryApi`, implement in `InventoryMovementQueryApiImpl`, and add the backing JPQL query to `InventoryMovementRepository`.
  - Repository query: `SELECT DISTINCT m.productionRunId FROM InventoryMovementEntity m WHERE m.toLocationType = 'DISTRIBUTOR' AND m.toLocationId = :distributorId AND m.movementType = 'ALLOCATION'`
  - Test (integration): given two production runs each with an ALLOCATION movement to distributorX → method returns both run IDs. A run with only a SALE (no ALLOCATION) to the distributor is not included.

### Group 3 — Rewrite validateQuantityIsAvailable()

- [x] **3.1** Rewrite `ProductionRunQueryApiImpl.validateQuantityIsAvailable()` to use `InventoryMovementQueryApi.getWarehouseInventory()` instead of `AllocationQueryApi`.
  - Remove `AllocationQueryApi` injection; inject `InventoryMovementQueryApi` instead.
  - Available stock formula: `productionRun.quantity() + inventoryMovementQueryApi.getWarehouseInventory(productionRunId)`.
  - Note: verify the delta semantics of `getWarehouseInventory()` (should return negative value after allocations, positive after returns) before coding.
  - Test: unit test — production run of 500; 200 allocated (warehouse delta = -200); available = 300. Requesting 300 succeeds; requesting 301 throws `InsufficientInventoryException`.

### Group 4 — Remove prior-allocation check from SaleLineItemProcessor

- [x] **4.1** Remove `AllocationQueryApi` injection and the `hasAllocation` guard from `SaleLineItemProcessor`.
  - The stock-only check (`FR-7`) remains unchanged.
  - Test: verify that a sale attempt against a distributor with zero stock (and no allocation) throws `InsufficientInventoryException` (not an `IllegalStateException` about missing allocation).

### Group 5 — Update AgreementController

- [ ] **5.1** Replace `allocationQueryApi.getAllocationsForDistributor()` in `AgreementController.buildAvailableRuns()` with `inventoryMovementQueryApi.getProductionRunIdsAllocatedToDistributor()`.
  - Remove `AllocationQueryApi` injection.
  - Test: update `AgreementControllerTest` — remove `AllocationQueryApi` mock, add `InventoryMovementQueryApi` mock; verify available runs are still resolved correctly.

### Group 6 — AllocateController (replaces AllocationController)

- [ ] **6.1** Create `AllocateForm` in `inventory/productionrun/api/`.
  - Fields: `LocationType locationType`, `Long distributorId` (nullable), `int quantity`.

- [ ] **6.2** Create `CancelBandcampReservationForm` in `inventory/productionrun/api/`.
  - Field: `int quantity`.

- [ ] **6.3** Create `AllocateController` in `inventory/productionrun/api/`.
  - POST `…/production-runs/{runId}/allocations`:
    1. Call `productionRunQueryApi.validateQuantityIsAvailable(runId, form.quantity)`
    2. Build `toLocation` from form: `BANDCAMP` if `locationType == BANDCAMP`, else `InventoryLocation.distributor(form.distributorId)`
    3. Call `inventoryMovementCommandApi.recordMovement(runId, InventoryLocation.warehouse(), toLocation, quantity, ALLOCATION, null)`
    4. Catch `InsufficientInventoryException` → flash error + redirect
  - POST `…/production-runs/{runId}/bandcamp-cancellations`:
    1. `int held = inventoryMovementQueryApi.getBandcampInventory(runId)`
    2. If `form.quantity > held` → flash error + redirect
    3. Call `inventoryMovementCommandApi.recordMovement(runId, InventoryLocation.bandcamp(), InventoryLocation.warehouse(), quantity, RETURN, null)`
  - Test (`AllocateControllerTest`): successful allocation redirects; allocation over limit flashes error; successful cancellation redirects; cancellation over held quantity flashes error.

### Group 7 — Update view models

- [ ] **7.1** Update `ProductionRunWithAllocation`: remove `allocated`, `unallocated`, `allocationViews` fields; add `int bandcampInventory`.
  - Update all construction sites (primarily `ReleaseController`).

- [ ] **7.2** Update `DistributorInventoryView`: remove `allocated` field; verify whether `sold()` derived method is referenced in the template and remove if not.
  - Grep `release.html` for `allocated` and `sold` usages before removing.

- [ ] **7.3** Delete `inventory/api/AllocationView.java` after confirming no remaining references.

### Group 8 — Update ReleaseController

- [ ] **8.1** Remove `AllocationQueryApi` injection from `ReleaseController`.

- [ ] **8.2** Rewrite `buildProductionRunWithAllocation()`:
  - Remove allocation queries; derive warehouse available stock as `productionRun.quantity() + inventoryMovementQueryApi.getWarehouseInventory(runId)`.
  - Add `bandcampInventory` from `inventoryMovementQueryApi.getBandcampInventory(runId)`.
  - Derive distributor inventories from `inventoryMovementQueryApi.getCurrentInventoryByDistributor(runId)` and resolve names via `DistributorQueryApi`.
  - Test: update `ReleaseControllerTest` — remove allocation mocks, add movement mocks; verify model contains correct warehouse and Bandcamp values.

### Group 9 — Update release.html and extract JS

- [ ] **9.1** Update `release.html`:
  - Remove the allocation details dropdown (expandable row with per-distributor allocation quantities).
  - Update production run table: remove "Allocated" and "Unallocated" columns.
  - Update inventory status section: remove "Allocated" column from distributor table; add Bandcamp row showing held units and a "Cancel Reservation" button (triggers cancellation modal).
  - Update allocate modal: add a location type selector (Distributor / Bandcamp); show distributor dropdown only when Distributor is selected; hide it for Bandcamp. Update form action URL to point to `AllocateController` path.
  - Add Bandcamp cancellation modal: simple form with quantity input; posts to `…/bandcamp-cancellations`.
  - CSRF tokens required on all new forms.

- [ ] **9.2** Extract allocate modal JavaScript from `release.html` to `src/main/resources/static/js/allocate-form.js`.
  - Export functions for testability (ES6 module pattern matching existing JS files).
  - Write `src/test/js/allocate-form.test.js` with Vitest unit tests covering: form action URL construction, location type toggle (show/hide distributor dropdown), max quantity binding.

### Group 10 — Delete allocation module

- [ ] **10.1** Delete the entire `inventory/allocation/` package (all 9 files listed in spec).
  - Run `./gradlew build` and confirm zero compilation errors.

- [ ] **10.2** Delete `AllocationControllerTest.java`.

### Group 11 — Database migration

- [ ] **11.1** Write `V31__drop_channel_allocation_table.sql`:
  ```sql
  DROP INDEX IF EXISTS idx_channel_allocation_production_run_id;
  DROP INDEX IF EXISTS idx_channel_allocation_sales_channel_id;
  DROP TABLE channel_allocation;
  ```
  - Run `./gradlew test` to confirm the migration applies cleanly and all tests pass.

## Blockers

None.
