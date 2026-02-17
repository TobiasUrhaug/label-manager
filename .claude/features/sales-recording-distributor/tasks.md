# Distributor Sales Recording — Task Backlog

## Legend
- [ ] To do
- [x] Done
- [~] In progress

---

## Phase 1: Refactor InventoryMovement to Bidirectional Model

> Foundational change. All other phases depend on this. The existing `distributorId +
> quantityDelta` schema is replaced with `fromLocationType + fromLocationId +
> toLocationType + toLocationId + quantity`. Existing records are migrated.

---

- [ ] **TASK-001: Add LocationType enum**
  - **Context**: The new movement model needs a type to distinguish between WAREHOUSE,
    DISTRIBUTOR, and EXTERNAL as inventory locations.
  - **Scope**: Create `org.omt.labelmanager.inventory.domain.LocationType` enum with values
    `WAREHOUSE`, `DISTRIBUTOR`, `EXTERNAL`.
  - **Module/Package**: `inventory/domain`
  - **Acceptance criteria**:
    - Enum compiles with the three values.
    - Javadoc explains the meaning of each value.
  - **Dependencies**: None

---

- [ ] **TASK-002: Write migration V25 — refactor inventory_movement table**
  - **Context**: The DB schema must change before the Java code. The migration adds the new
    bidirectional columns, migrates existing ALLOCATION and SALE records, and drops the old
    columns.
  - **Scope**: `V25__refactor_inventory_movement_bidirectional.sql` (full SQL as specified in
    spec section 9). Verify the migration runs cleanly against a local DB.
  - **Module/Package**: `src/main/resources/db/migration`
  - **Acceptance criteria**:
    - Migration adds `from_location_type`, `from_location_id`, `to_location_type`,
      `to_location_id`, `quantity` columns.
    - Existing ALLOCATION rows have `from=WAREHOUSE, to=DISTRIBUTOR(distributor_id),
      quantity=quantity_delta`.
    - Existing SALE rows have `from=DISTRIBUTOR(distributor_id), to=EXTERNAL,
      quantity=ABS(quantity_delta)`.
    - Old `distributor_id` and `quantity_delta` columns are dropped.
    - New indexes are created.
    - Application starts cleanly after migration.
  - **Dependencies**: TASK-001

---

- [ ] **TASK-003: Refactor InventoryMovementEntity**
  - **Context**: The JPA entity must reflect the new schema.
  - **Scope**: Update `InventoryMovementEntity` — replace `distributorId` and `quantityDelta`
    fields with `fromLocationType` (LocationType), `fromLocationId` (Long nullable),
    `toLocationType` (LocationType), `toLocationId` (Long nullable), `quantity` (int). Update
    constructor and getters accordingly.
  - **Module/Package**: `inventory/inventorymovement/infrastructure`
  - **Acceptance criteria**:
    - Entity maps correctly to the new schema.
    - No references to `distributorId` or `quantityDelta` remain on the entity.
  - **Dependencies**: TASK-002

---

- [ ] **TASK-004: Refactor InventoryMovement domain record**
  - **Context**: The public domain record exposed via the API must match the new entity.
  - **Scope**: Update `InventoryMovement` record — replace `distributorId` and `quantityDelta`
    with the four new location fields and `quantity`. Update `fromEntity()` factory method.
  - **Module/Package**: `inventory/inventorymovement/domain`
  - **Acceptance criteria**:
    - Record has `fromLocationType`, `fromLocationId`, `toLocationType`, `toLocationId`,
      `quantity` fields.
    - `fromEntity()` maps correctly.
  - **Dependencies**: TASK-003

---

- [ ] **TASK-005: Refactor InventoryMovementCommandApi and RecordMovementUseCase**
  - **Context**: The command API signature must change to accept the new bidirectional
    parameters. A new `deleteMovementsByReference` method is needed for edit/delete flows.
  - **Scope**:
    - Update `InventoryMovementCommandApi`:
      - New `recordMovement(Long productionRunId, LocationType fromLocationType,
        Long fromLocationId, LocationType toLocationType, Long toLocationId,
        MovementType movementType, Long referenceId)`.
      - New `deleteMovementsByReference(MovementType movementType, Long referenceId)`.
    - Update `RecordMovementUseCase` to persist the new fields.
    - Create `DeleteMovementsUseCase` — deletes all movements matching movementType +
      referenceId.
    - Update `InventoryMovementCommandApiImpl` to delegate to both use cases.
  - **Module/Package**: `inventory/inventorymovement/api`, `inventory/inventorymovement/application`
  - **Acceptance criteria**:
    - `recordMovement` saves a movement with correct from/to location fields.
    - `deleteMovementsByReference(SALE, 42L)` deletes all SALE movements with referenceId=42.
    - Integration test verifies both methods against real DB.
  - **Dependencies**: TASK-004

---

- [ ] **TASK-006: Extend InventoryMovementQueryApi with inventory calculation methods**
  - **Context**: Current inventory at any location is calculated by summing movements.
    These new query methods are needed by the sale/return validation logic and the inventory
    visibility views.
  - **Scope**:
    - Add to `InventoryMovementQueryApi`:
      - `int getCurrentInventory(Long productionRunId, Long distributorId)`
      - `int getWarehouseInventory(Long productionRunId)`
      - `Map<Long, Integer> getCurrentInventoryByDistributor(Long productionRunId)`
      - `List<InventoryMovement> getMovementsForProductionRun(Long productionRunId)`
    - Implement in `InventoryMovementQueryApiImpl` and `InventoryMovementRepository`.
  - **Module/Package**: `inventory/inventorymovement`
  - **Acceptance criteria**:
    - `getCurrentInventory` returns `SUM(to quantity) - SUM(from quantity)` for a distributor.
    - `getWarehouseInventory` returns correct warehouse balance.
    - `getCurrentInventoryByDistributor` returns a map of all distributors with non-zero
      inventory for a production run.
    - `getMovementsForProductionRun` returns all movements sorted by `occurredAt` descending.
    - Integration tests verify each method with known data.
  - **Dependencies**: TASK-005

---

- [ ] **TASK-007: Update AllocationCommandApiImpl to use new movement API**
  - **Context**: `AllocationCommandApiImpl.createAllocation()` currently calls
    `inventoryMovementCommandApi.recordMovement(...)` with the old signature. It must use
    the new bidirectional signature: `from=WAREHOUSE, to=DISTRIBUTOR(distributorId)`.
  - **Scope**: Update the `recordMovement(...)` call in `AllocationCommandApiImpl` to use
    the new API signature.
  - **Module/Package**: `inventory/allocation/application`
  - **Acceptance criteria**:
    - Creating an allocation produces a movement with `fromLocationType=WAREHOUSE`,
      `toLocationType=DISTRIBUTOR`, `toLocationId=distributorId`.
    - Existing allocation integration tests still pass.
  - **Dependencies**: TASK-006

---

## Phase 2: Remove unitsSold from ChannelAllocation

> With inventory tracking moved entirely to InventoryMovement, the denormalized
> `unitsSold` field and `reduceAllocation()` method are no longer needed.

---

- [ ] **TASK-008: Write migration V26 — drop units_sold from channel_allocation**
  - **Context**: The DB column is no longer used.
  - **Scope**: `V26__remove_units_sold_from_channel_allocation.sql` —
    `ALTER TABLE channel_allocation DROP COLUMN units_sold`.
  - **Module/Package**: `src/main/resources/db/migration`
  - **Acceptance criteria**:
    - Migration runs without error.
    - Application starts cleanly after migration.
  - **Dependencies**: TASK-007

---

- [ ] **TASK-009: Remove unitsSold from ChannelAllocationEntity, ChannelAllocation, and AllocationCommandApi**
  - **Context**: After the migration, the Java layer must be updated to remove all traces of
    `unitsSold`.
  - **Scope**:
    - Remove `unitsSold` field, `getUnitsSold()`, `incrementUnitsSold()` from
      `ChannelAllocationEntity`.
    - Remove `unitsSold` from `ChannelAllocation` domain record.
    - Remove `reduceAllocation()` from `AllocationCommandApi`.
    - Delete `ReduceAllocationUseCase`.
    - Update `AllocationCommandApiImpl` to remove delegation to `ReduceAllocationUseCase`.
  - **Module/Package**: `inventory/allocation`
  - **Acceptance criteria**:
    - No references to `unitsSold` or `reduceAllocation` remain in production code.
    - All existing allocation integration tests pass.
  - **Dependencies**: TASK-008

---

## Phase 3: Add distributorId to Sale + Validate via Movements

---

- [ ] **TASK-010: Write migration V27 — add distributor_id to sale**
  - **Context**: The sale entity must explicitly store which distributor made the sale.
  - **Scope**: `V27__add_distributor_id_to_sale.sql`:
    - Add `distributor_id` column (nullable initially).
    - Backfill DIRECT sales from the DIRECT distributor of each label.
    - Backfill DISTRIBUTOR-channel sales from the `from_location_id` of their SALE movements.
    - Add NOT NULL constraint.
    - Add index.
  - **Module/Package**: `src/main/resources/db/migration`
  - **Acceptance criteria**:
    - All existing sale rows have a non-null `distributor_id` after migration.
    - Application starts cleanly.
  - **Dependencies**: TASK-009

---

- [ ] **TASK-011: Add distributorId to SaleEntity and Sale domain record**
  - **Context**: Java model must reflect the new column.
  - **Scope**:
    - Add `distributorId` field to `SaleEntity` (getter, constructor).
    - Add `distributorId` to `Sale` record.
    - Update `fromEntity()` mapping and anywhere `Sale` is constructed.
  - **Module/Package**: `sales/sale`
  - **Acceptance criteria**:
    - `Sale.distributorId()` returns the correct distributor.
    - No compilation errors.
  - **Dependencies**: TASK-010

---

- [ ] **TASK-012: Update RegisterSaleUseCase to validate via movements and use new movement API**
  - **Context**: Inventory validation previously used `reduceAllocation()` (now removed).
    It must now use `InventoryMovementQueryApi.getCurrentInventory()`. The movement call must
    use the new bidirectional API. The sale's `distributorId` must be persisted.
  - **Scope**:
    - Replace `allocationCommandApi.reduceAllocation(...)` with:
      1. `int available = movementQueryApi.getCurrentInventory(productionRunId, distributorId)`
      2. If `available < quantity`, throw `InsufficientInventoryException`.
    - Replace old `inventoryMovementCommandApi.recordMovement(...)` with the new signature:
      `from=DISTRIBUTOR(distributorId), to=EXTERNAL, SALE, saleId`.
    - Store `distributorId` on `SaleEntity`.
    - Record movements AFTER saving the sale (so `saleId` is available as `referenceId`).
  - **Module/Package**: `sales/sale/application`
  - **Acceptance criteria**:
    - Registering a sale with sufficient inventory succeeds and creates a SALE movement.
    - Registering a sale with insufficient inventory throws `InsufficientInventoryException`.
    - `sale.distributor_id` is persisted correctly.
    - Integration test covers both cases.
  - **Dependencies**: TASK-011

---

- [ ] **TASK-013: Add getSalesForDistributor and getSalesForProductionRun to SaleQueryApi**
  - **Context**: Sales history views on the distributor page and release page need these queries.
  - **Scope**:
    - Add `findByDistributorId` and `findByProductionRunId` queries to `SaleRepository`.
    - Add `getSalesForDistributor(Long distributorId)` and
      `getSalesForProductionRun(Long productionRunId)` to `SaleQueryApi`.
    - Implement in `SaleQueryApiImpl`.
  - **Module/Package**: `sales/sale`
  - **Acceptance criteria**:
    - `getSalesForDistributor` returns sales sorted by date descending.
    - `getSalesForProductionRun` returns sales for the given production run.
    - Integration tests verify both queries.
  - **Dependencies**: TASK-012

---

## Phase 4: Sale Edit and Delete

---

- [ ] **TASK-014: Implement UpdateSaleUseCase**
  - **Context**: Editing a sale requires reversing old inventory movements and applying
    new ones, then updating the sale entity.
  - **Scope**:
    - Create `UpdateSaleUseCase`:
      1. Load existing sale.
      2. `deleteMovementsByReference(SALE, saleId)`.
      3. Replace line items on entity.
      4. For each new line item: validate inventory, add line item to entity.
      5. Save entity.
      6. For each new line item: record SALE movement (`from=DISTRIBUTOR, to=EXTERNAL,
         referenceId=saleId`).
    - Add `updateSale(...)` to `SaleCommandApi`.
    - Wire up in `SaleCommandApiImpl`.
  - **Module/Package**: `sales/sale/application`
  - **Acceptance criteria**:
    - Editing a sale updates the entity and replaces movements correctly.
    - Editing to a quantity that exceeds available inventory is rejected.
    - Integration test covers: successful edit, insufficient inventory rejection.
  - **Dependencies**: TASK-013

---

- [ ] **TASK-015: Implement DeleteSaleUseCase**
  - **Context**: Deleting a sale must remove movements and the sale entity.
  - **Scope**:
    - Create `DeleteSaleUseCase`:
      1. `deleteMovementsByReference(SALE, saleId)`.
      2. `saleRepository.deleteById(saleId)`.
    - Add `deleteSale(Long saleId)` to `SaleCommandApi`.
    - Wire up in `SaleCommandApiImpl`.
  - **Module/Package**: `sales/sale/application`
  - **Acceptance criteria**:
    - Deleting a sale removes the entity and all related SALE movements.
    - Integration test verifies the sale and its movements are gone.
  - **Dependencies**: TASK-014

---

- [ ] **TASK-016: Add edit and delete endpoints to SaleController**
  - **Context**: The controller needs GET and POST endpoints for editing, and a POST
    endpoint for deletion.
  - **Scope**:
    - `GET /labels/{labelId}/sales/{saleId}/edit` — show pre-populated edit form.
    - `POST /labels/{labelId}/sales/{saleId}` — submit edit.
    - `POST /labels/{labelId}/sales/{saleId}/delete` — delete with confirmation.
    - Create `templates/sale/edit.html`.
    - Update `templates/sale/detail.html` with Edit and Delete buttons.
  - **Module/Package**: `sales/sale/api`
  - **Acceptance criteria**:
    - Edit form is pre-populated with existing sale data.
    - Submitting a valid edit redirects to sale detail.
    - Submitting an invalid edit re-renders form with error message.
    - Delete redirects to sales list.
    - Controller tests cover all new endpoints (mock API).
  - **Dependencies**: TASK-015

---

## Phase 5: Return Module

---

- [ ] **TASK-017: Write migration V28 — create distributor_return tables**
  - **Context**: Persistence layer for the new return module.
  - **Scope**: `V28__create_distributor_return_tables.sql` — creates `distributor_return`
    and `distributor_return_line_item` tables with all indexes (see spec section 9).
  - **Module/Package**: `src/main/resources/db/migration`
  - **Acceptance criteria**:
    - Tables and indexes created without error.
    - Application starts cleanly.
  - **Dependencies**: TASK-013

---

- [ ] **TASK-018: Create Return domain model and infrastructure**
  - **Context**: Domain records and JPA entities for the return module.
  - **Scope**:
    - `DistributorReturn.java` — public record with `id, labelId, distributorId, returnDate,
      notes, lineItems, createdAt`.
    - `ReturnLineItem.java` — public record with `id, returnId, releaseId, format, quantity`.
    - `ReturnLineItemInput.java` — value object for form input.
    - `DistributorReturnEntity.java` — package-private JPA entity.
    - `ReturnLineItemEntity.java` — package-private JPA entity with OneToMany on return.
    - `DistributorReturnRepository.java` — package-private Spring Data repository with
      `findByLabelIdOrderByReturnDateDesc`, `findByDistributorIdOrderByReturnDateDesc`.
  - **Module/Package**: `sales/distributor_return`
  - **Acceptance criteria**:
    - Entities map correctly to the new tables.
    - Repositories compile and can be injected.
  - **Dependencies**: TASK-017

---

- [ ] **TASK-019: Implement RegisterReturnUseCase and DistributorReturnCommandApi**
  - **Context**: Core business logic for registering a return. For each line item, validates
    that the distributor has sufficient current inventory, then records RETURN movements.
  - **Scope**:
    - `RegisterReturnUseCase` (package-private):
      1. Validate label and distributor exist.
      2. For each line item:
         - Find most-recent production run via `ProductionRunQueryApi.findMostRecent(...)`.
         - `getCurrentInventory(productionRunId, distributorId) >= quantity`, else throw
           `InsufficientInventoryException`.
         - Add line item to entity.
      3. Save return entity → `returnId`.
      4. For each line item: `recordMovement(from=DISTRIBUTOR(distributorId), to=WAREHOUSE,
         RETURN, returnId)`.
    - `DistributorReturnCommandApi` — public interface with `registerReturn(...)`,
      `updateReturn(...)`, `deleteReturn(...)`.
    - `DistributorReturnCommandApiImpl` — delegates to use cases.
  - **Module/Package**: `sales/distributor_return/application`
  - **Acceptance criteria**:
    - Registering a return creates the entity and RETURN movements.
    - Attempting to return more than the distributor has is rejected.
    - Integration tests cover both cases.
  - **Dependencies**: TASK-018

---

- [ ] **TASK-020: Implement UpdateReturnUseCase and DeleteReturnUseCase**
  - **Context**: Edit/delete for returns mirrors the sale edit/delete pattern.
  - **Scope**:
    - `UpdateReturnUseCase`:
      1. `deleteMovementsByReference(RETURN, returnId)`.
      2. Replace line items on entity.
      3. Validate and save.
      4. Record new RETURN movements.
    - `DeleteReturnUseCase`:
      1. `deleteMovementsByReference(RETURN, returnId)`.
      2. Delete return entity.
    - Wire up in `DistributorReturnCommandApiImpl`.
  - **Module/Package**: `sales/distributor_return/application`
  - **Acceptance criteria**:
    - Editing a return adjusts movements correctly.
    - Deleting a return removes entity and movements.
    - Integration tests for both operations.
  - **Dependencies**: TASK-019

---

- [ ] **TASK-021: Implement DistributorReturnQueryApi**
  - **Context**: Query methods needed by the controller and history views.
  - **Scope**:
    - `DistributorReturnQueryApi` — `getReturnsForLabel(Long labelId)`,
      `getReturnsForDistributor(Long distributorId)`, `findById(Long returnId)`.
    - `DistributorReturnQueryApiImpl` — delegates to repository.
  - **Module/Package**: `sales/distributor_return`
  - **Acceptance criteria**:
    - `getReturnsForLabel` returns returns sorted by date descending.
    - `getReturnsForDistributor` returns returns for a specific distributor.
    - Integration tests verify both queries.
  - **Dependencies**: TASK-019

---

- [ ] **TASK-022: Create ReturnController and templates**
  - **Context**: HTTP layer for the return module with full CRUD flows.
  - **Scope**:
    - `ReturnController` with endpoints:
      - `GET /labels/{labelId}/returns` — list
      - `GET /labels/{labelId}/returns/new` — create form
      - `POST /labels/{labelId}/returns` — submit create
      - `GET /labels/{labelId}/returns/{returnId}` — detail
      - `GET /labels/{labelId}/returns/{returnId}/edit` — edit form
      - `POST /labels/{labelId}/returns/{returnId}` — submit edit
      - `POST /labels/{labelId}/returns/{returnId}/delete` — delete
    - Templates: `return/list.html`, `return/register.html`, `return/detail.html`,
      `return/edit.html`.
    - `RegisterReturnForm` and `ReturnLineItemForm` for form binding.
    - Dynamic line item add/remove using the existing sale-form JavaScript pattern
      (extract to `return-form.js` with Vitest tests).
  - **Module/Package**: `sales/distributor_return/api`
  - **Acceptance criteria**:
    - Full CRUD flow works end-to-end.
    - Validation errors are shown on form re-render.
    - Controller tests cover all endpoints.
    - JavaScript tests cover `return-form.js`.
  - **Dependencies**: TASK-021

---

## Phase 6: Inventory Visibility on Release Page

---

- [ ] **TASK-023: Add getCurrentInventoryByDistributor and getMovementsForProductionRun to query API**
  - **Context**: The release detail page needs per-distributor inventory breakdown and movement
    history. `getCurrentInventoryByDistributor` was deferred from TASK-006 as it wasn't yet
    needed.
  - **Scope**:
    - Add to `InventoryMovementQueryApi`:
      - `Map<Long, Integer> getCurrentInventoryByDistributor(Long productionRunId)`
    - Implement in `InventoryMovementQueryApiImpl` and repository.
    - `getMovementsForProductionRun` should already exist from TASK-006 — verify it's done.
  - **Module/Package**: `inventory/inventorymovement`
  - **Acceptance criteria**:
    - `getCurrentInventoryByDistributor` returns a map where each entry is
      `distributorId → currentQuantity`. Distributors with zero current inventory may be
      omitted.
    - Integration test with known data verifies correctness.
  - **Dependencies**: TASK-022

---

- [ ] **TASK-024: Add inventory status section to release detail page**
  - **Context**: Label managers need to see where inventory is located directly from the
    release page.
  - **Scope**: Update `ReleaseController.showRelease(...)` to fetch and pass to the model:
    - For each production run of the release:
      - `ChannelAllocation` records (for original allocated quantities) via `AllocationQueryApi`.
      - `getCurrentInventoryByDistributor(productionRunId)` for current quantities.
      - `getWarehouseInventory(productionRunId)` for warehouse stock.
      - `getMovementsForProductionRun(productionRunId)` for movement history.
    - Distributor names resolved via `DistributorQueryApi`.
    - Update `templates/release/detail.html` to render the inventory section:
      - Total manufactured
      - Warehouse: N units
      - Per distributor: name, allocated, current, sold (allocated − current)
      - Movement history table (date, type, from, to, quantity)
  - **Module/Package**: `catalog/release/api`, templates
  - **Acceptance criteria**:
    - Release detail page shows correct inventory figures.
    - Warehouse and distributor totals update correctly after recording sales and returns.
    - Movement history shows correct entries in reverse chronological order.
    - Controller test verifies model attributes are populated.
  - **Dependencies**: TASK-023

---

## Phase 7: Sales & Returns History on Distributor and Release Pages

---

- [ ] **TASK-025: Add sales and returns to distributor detail page**
  - **Context**: Label managers want to see a distributor's full sales and return history
    from the distributor detail page.
  - **Scope**:
    - Update `DistributorController.showDistributor(...)` to fetch:
      - `saleQueryApi.getSalesForDistributor(distributorId)`
      - `returnQueryApi.getReturnsForDistributor(distributorId)`
    - Update `templates/distributor/detail.html` to show:
      - Sales table: date, total units, total revenue (link to sale detail).
      - Returns table: date, total units (link to return detail).
  - **Module/Package**: `distribution/distributor/api`
  - **Acceptance criteria**:
    - Distributor detail page shows all sales sorted newest first.
    - Distributor detail page shows all returns sorted newest first.
    - Controller test verifies model attributes.
  - **Dependencies**: TASK-024

---

- [ ] **TASK-026: Add sales history to release detail page**
  - **Context**: Label managers want to see how well a specific release is selling across
    all distributors.
  - **Scope**:
    - In `ReleaseController.showRelease(...)`, for each production run of the release, call
      `saleQueryApi.getSalesForProductionRun(productionRunId)`.
    - Combine results and enrich line items with distributor names.
    - Update `templates/release/detail.html` to show a sales-by-release table:
      date, distributor, quantity sold, revenue. Show totals: total units sold, total revenue.
  - **Module/Package**: `catalog/release/api`, templates
  - **Acceptance criteria**:
    - Release detail page shows all sales of that release across all distributors.
    - Totals (units and revenue) are correct.
    - Controller test verifies model attributes.
  - **Dependencies**: TASK-025
