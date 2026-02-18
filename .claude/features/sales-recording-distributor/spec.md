# Distributor Sales Recording — Implementation Specification

## 1. Overview

This feature adds distributor sales recording, returns processing, and inventory visibility to the
Label Manager application. Label managers receive monthly sales reports from distributors and need
to record what was sold, process returns of unsold inventory, and maintain an accurate picture of
where physical inventory is located at all times.

The feature extends the existing `sales/sale` module (which only supports creating sales) and
introduces a new `sales/return` module. It also refactors the `inventory/inventorymovement`
module to model inventory flows as proper bidirectional transfers, and removes the now-redundant
`unitsSold` denormalization from `ChannelAllocation`.

### Existing State

The following already exists and must be extended rather than replaced:

- **`sales/sale`** — `RegisterSaleUseCase`, `SaleCommandApi`, `SaleQueryApi`, `SaleController`
  (list, create, detail views). Missing: edit, delete, `distributorId` on the entity.
- **`inventory/allocation`** — `createAllocation()` and `reduceAllocation()`. The
  `ChannelAllocationEntity` has a mutable `unitsSold` field that will be removed.
- **`inventory/inventorymovement`** — `recordMovement(productionRunId, distributorId,
  quantityDelta, movementType, referenceId)`. This API will be replaced with a bidirectional
  model.
- **`distribution/distributor`** — `DistributorQueryApi`, `DistributorCommandApi`. Unchanged.
- **`inventory/productionrun`** — `ProductionRunQueryApi`. Unchanged.

---

## 2. Goals & Non-Goals

### Goals

- Record distributor sales with automatic inventory deduction and movement audit trail.
- Process distributor returns with automatic inventory restoration and movement audit trail.
- Edit and delete sales and returns with correct inventory adjustments.
- Display inventory location breakdown (warehouse vs each distributor) on the release detail page.
- Display movement history on the release detail page.
- Display sales history on label, distributor, and release pages.
- Display return history on distributor pages.
- Refactor `InventoryMovement` to a clean bidirectional transfer model.
- Remove `ChannelAllocation.unitsSold` — current inventory is derived entirely from movements.

### Non-Goals

- Direct (label-to-consumer) sales.
- Record-store or event-channel returns (returns are distributor-specific only).
- Multi-currency support.
- Role-based access control.
- Financial tracking for returns (refunds, restocking fees).
- Automated imports from distributor systems.
- Multi-warehouse support.

---

## 3. Architecture

### 3.1 System Diagram

```
┌──────────────────────────────────────────────────────────────┐
│  sales bounded context                                        │
│                                                               │
│  ┌─────────────────┐       ┌─────────────────┐               │
│  │   sale module   │       │  return module  │               │
│  │  (extended)     │       │  (NEW)          │               │
│  └────────┬────────┘       └────────┬────────┘               │
│           │                         │                         │
└───────────┼─────────────────────────┼─────────────────────────┘
            │                         │
            ▼                         ▼
┌──────────────────────────────────────────────────────────────┐
│  inventory bounded context                                    │
│                                                               │
│  ┌─────────────────┐  ┌────────────────────┐                 │
│  │   allocation    │  │ inventorymovement  │                 │
│  │  (simplified)   │  │  (refactored)      │                 │
│  └─────────────────┘  └────────────────────┘                 │
│  ┌─────────────────┐                                         │
│  │  productionrun  │                                         │
│  │  (unchanged)    │                                         │
│  └─────────────────┘                                         │
└──────────────────────────────────────────────────────────────┘
            │
            ▼
┌──────────────────────────────────────────────────────────────┐
│  distribution/catalog bounded contexts                        │
│  (unchanged: Distributor, Release, Label, ProductionRun)     │
└──────────────────────────────────────────────────────────────┘
```

### 3.2 Bounded Contexts & Modules

#### `inventory/inventorymovement` (refactored)

- **Package**: `org.omt.labelmanager.inventory.inventorymovement`
- **Public API**:
  - `InventoryMovementCommandApi` — `recordMovement(...)`, `deleteMovementsByReference(...)`
  - `InventoryMovementQueryApi` — `findByProductionRunId(...)`,
    `getCurrentInventory(productionRunId, distributorId)`,
    `getCurrentInventoryByLocation(productionRunId)`,
    `getMovementsForProductionRun(productionRunId)`
- **Domain model**: `InventoryMovement` record (refactored), `LocationType` enum (NEW)
- **Dependencies**: none

#### `inventory/allocation` (simplified)

- **Package**: `org.omt.labelmanager.inventory.allocation`
- **Public API**:
  - `AllocationCommandApi` — `createAllocation(...)`. `reduceAllocation()` **removed**.
  - `AllocationQueryApi` — `getAllocationsForProductionRun(...)`, `getTotalAllocated(...)`
- **Domain model**: `ChannelAllocation` record (remove `unitsSold`)
- **Dependencies**: `InventoryMovementCommandApi` (records ALLOCATION movement on creation)

#### `sales/sale` (extended)

- **Package**: `org.omt.labelmanager.sales.sale`
- **Public API**:
  - `SaleCommandApi` — `registerSale(...)`, `updateSale(...)`, `deleteSale(...)`
  - `SaleQueryApi` — `getSalesForLabel(...)`, `getSalesForDistributor(...)`,
    `getSalesForProductionRun(...)`, `findById(...)`
- **Domain model**: `Sale` record (add `distributorId`), `SaleLineItem`
- **Dependencies**: `AllocationQueryApi`, `InventoryMovementCommandApi`,
  `InventoryMovementQueryApi`, `ProductionRunQueryApi`, `DistributorQueryApi`,
  `LabelQueryApi`, `ReleaseQueryApi`

#### `sales/return` (NEW)

- **Package**: `org.omt.labelmanager.sales.distributor_return`
- **Public API**:
  - `DistributorReturnCommandApi` — `registerReturn(...)`, `updateReturn(...)`,
    `deleteReturn(...)`
  - `DistributorReturnQueryApi` — `getReturnsForLabel(...)`,
    `getReturnsForDistributor(...)`, `findById(...)`
- **Domain model**: `DistributorReturn` record, `ReturnLineItem` record
- **Dependencies**: `InventoryMovementCommandApi`, `InventoryMovementQueryApi`,
  `AllocationQueryApi`, `ProductionRunQueryApi`, `DistributorQueryApi`, `LabelQueryApi`,
  `ReleaseQueryApi`

### 3.3 Data Model

#### LocationType (NEW enum)

```java
// org.omt.labelmanager.inventory.domain.LocationType
public enum LocationType {
    WAREHOUSE,    // The label's own stock
    DISTRIBUTOR,  // An external distributor holding inventory
    EXTERNAL      // Outside the label's system (sold to customers)
}
```

Warehouse is implicit — it is the label's own storage, identified by `LocationType.WAREHOUSE`
in movements. There is no separate Warehouse entity; the label itself owns the warehouse stock.

#### InventoryMovement (refactored)

| Field              | Type         | Notes                                              |
|--------------------|--------------|----------------------------------------------------|
| `id`               | Long         | PK                                                 |
| `productionRunId`  | Long         | FK → production_run                                |
| `fromLocationType` | LocationType | WAREHOUSE, DISTRIBUTOR, or EXTERNAL                |
| `fromLocationId`   | Long?        | distributorId if fromLocationType=DISTRIBUTOR      |
| `toLocationType`   | LocationType | WAREHOUSE, DISTRIBUTOR, or EXTERNAL                |
| `toLocationId`     | Long?        | distributorId if toLocationType=DISTRIBUTOR        |
| `quantity`         | int          | Always positive                                    |
| `movementType`     | MovementType | ALLOCATION, SALE, RETURN                           |
| `occurredAt`       | Instant      | When the movement occurred                         |
| `referenceId`      | Long?        | saleId or returnId                                 |

**Standard movement patterns:**

| Business event | fromLocationType | fromLocationId | toLocationType | toLocationId |
|----------------|-----------------|----------------|----------------|--------------|
| Allocation     | WAREHOUSE        | null           | DISTRIBUTOR    | distributorId |
| Sale           | DISTRIBUTOR      | distributorId  | EXTERNAL       | null          |
| Return         | DISTRIBUTOR      | distributorId  | WAREHOUSE      | null          |

**Current inventory calculation:**

```
currentInventory(productionRunId, WAREHOUSE) =
    SUM(quantity WHERE toLocationType=WAREHOUSE)
    - SUM(quantity WHERE fromLocationType=WAREHOUSE)

currentInventory(productionRunId, DISTRIBUTOR, distributorId) =
    SUM(quantity WHERE toLocationType=DISTRIBUTOR AND toLocationId=distributorId)
    - SUM(quantity WHERE fromLocationType=DISTRIBUTOR AND fromLocationId=distributorId)
```

#### ChannelAllocation (simplified)

Remove `unitsSold` and `incrementUnitsSold()`. `quantity` remains immutable — it is the
original allocated amount. Validation of available inventory for sales/returns uses
`InventoryMovementQueryApi.getCurrentInventory(...)` instead.

#### Sale (extended)

Add `distributorId` field — which distributor this sale is attributed to. For DIRECT sales this
is the auto-assigned DIRECT distributor; for DISTRIBUTOR channel sales it is the explicit
distributor chosen by the user.

#### DistributorReturn (NEW)

| Field          | Type       | Notes                              |
|----------------|------------|------------------------------------|
| `id`           | Long       | PK                                 |
| `labelId`      | Long       | FK → label                         |
| `distributorId`| Long       | FK → distributor (returning from)  |
| `returnDate`   | LocalDate  |                                    |
| `notes`        | String?    |                                    |
| `lineItems`    | List       | One or more                        |
| `createdAt`    | Instant    |                                    |

#### ReturnLineItem (NEW)

| Field           | Type          | Notes                     |
|-----------------|---------------|---------------------------|
| `id`            | Long          | PK                        |
| `returnId`      | Long          | FK → distributor_return   |
| `releaseId`     | Long          | FK → release              |
| `format`        | ReleaseFormat |                           |
| `quantity`      | int           | Must be positive          |

### 3.4 Data Flow

#### Record a Sale

1. User submits sale form (distributor, date, line items with quantity and price).
2. `RegisterSaleUseCase` validates label and distributor exist.
3. For each line item:
   - Find most-recent production run for release+format.
   - Call `InventoryMovementQueryApi.getCurrentInventory(productionRunId, distributorId)`.
   - If `currentInventory < quantity`, throw `InsufficientInventoryException`.
   - Add line item entity to sale.
4. Save sale entity → get `saleId`.
5. For each line item, call `InventoryMovementCommandApi.recordMovement(...)` with:
   - `from=DISTRIBUTOR(distributorId)`, `to=EXTERNAL`, `movementType=SALE`,
     `referenceId=saleId`.

#### Update a Sale

1. Load existing sale.
2. For each original line item: call `recordMovement(from=EXTERNAL, to=DISTRIBUTOR, SALE, saleId)`
   to conceptually reverse — but we **hard-delete** old movements instead:
   call `deleteMovementsByReference(SALE, saleId)`.
3. For each new line item: validate inventory (as if original sale never existed, since movements
   are gone), then record new SALE movements with same `saleId`.
4. Update sale entity fields and line items.

#### Delete a Sale

1. Load existing sale.
2. `deleteMovementsByReference(SALE, saleId)` — removes all movement records for this sale.
3. Delete sale entity (cascades to line items).

#### Record a Return

1. User submits return form (distributor, date, line items with quantity only).
2. `RegisterReturnUseCase` validates label and distributor.
3. For each line item:
   - Find most-recent production run for release+format.
   - Call `getCurrentInventory(productionRunId, distributorId)`.
   - If `currentInventory < quantity`, throw `InsufficientInventoryException`.
4. Save return entity → get `returnId`.
5. For each line item, record movement: `from=DISTRIBUTOR(distributorId)`, `to=WAREHOUSE`,
   `movementType=RETURN`, `referenceId=returnId`.

#### Update / Delete a Return

Mirrors update/delete for sales, using `deleteMovementsByReference(RETURN, returnId)`.

---

## 4. API Contracts

### InventoryMovementCommandApi (refactored)

```java
public interface InventoryMovementCommandApi {

    void recordMovement(
        Long productionRunId,
        LocationType fromLocationType,
        Long fromLocationId,        // null unless DISTRIBUTOR
        LocationType toLocationType,
        Long toLocationId,          // null unless DISTRIBUTOR
        MovementType movementType,
        Long referenceId            // saleId or returnId, nullable
    );

    void deleteMovementsByReference(MovementType movementType, Long referenceId);
}
```

### InventoryMovementQueryApi (extended)

```java
public interface InventoryMovementQueryApi {

    // Existing
    List<InventoryMovement> findByProductionRunId(Long productionRunId);

    // New
    int getCurrentInventory(Long productionRunId, Long distributorId);

    int getWarehouseInventory(Long productionRunId);

    // Returns map of distributorId → current quantity (excludes warehouse)
    Map<Long, Integer> getCurrentInventoryByDistributor(Long productionRunId);

    List<InventoryMovement> getMovementsForProductionRun(Long productionRunId);
}
```

### AllocationCommandApi (simplified)

```java
public interface AllocationCommandApi {

    // createAllocation remains — records immutable allocation + creates ALLOCATION movement
    ChannelAllocation createAllocation(
        Long productionRunId, Long distributorId, int quantity
    );

    // reduceAllocation REMOVED — inventory tracking now fully in movements
}
```

### SaleCommandApi (extended)

```java
public interface SaleCommandApi {

    Sale registerSale(
        Long labelId, LocalDate saleDate, ChannelType channel,
        String notes, Long distributorId, List<SaleLineItemInput> lineItems
    );

    void updateSale(
        Long saleId, LocalDate saleDate, ChannelType channel,
        String notes, Long distributorId, List<SaleLineItemInput> lineItems
    );

    void deleteSale(Long saleId);
}
```

### SaleQueryApi (extended)

```java
public interface SaleQueryApi {

    List<Sale> getSalesForLabel(Long labelId);
    List<Sale> getSalesForDistributor(Long distributorId);
    List<Sale> getSalesForProductionRun(Long productionRunId);
    Optional<Sale> findById(Long saleId);
    Money getTotalRevenueForLabel(Long labelId);
}
```

### DistributorReturnCommandApi (NEW)

```java
public interface DistributorReturnCommandApi {

    DistributorReturn registerReturn(
        Long labelId, Long distributorId, LocalDate returnDate,
        String notes, List<ReturnLineItemInput> lineItems
    );

    void updateReturn(
        Long returnId, LocalDate returnDate,
        String notes, List<ReturnLineItemInput> lineItems
    );

    void deleteReturn(Long returnId);
}
```

### DistributorReturnQueryApi (NEW)

```java
public interface DistributorReturnQueryApi {

    List<DistributorReturn> getReturnsForLabel(Long labelId);
    List<DistributorReturn> getReturnsForDistributor(Long distributorId);
    Optional<DistributorReturn> findById(Long returnId);
}
```

### HTTP Endpoints

#### Sale endpoints (extended)

| Method | Route                                | Description          |
|--------|--------------------------------------|----------------------|
| GET    | `/labels/{labelId}/sales`            | List sales for label |
| GET    | `/labels/{labelId}/sales/new`        | New sale form        |
| POST   | `/labels/{labelId}/sales`            | Submit new sale      |
| GET    | `/labels/{labelId}/sales/{saleId}`   | Sale detail          |
| GET    | `/labels/{labelId}/sales/{saleId}/edit` | Edit sale form    |
| POST   | `/labels/{labelId}/sales/{saleId}`   | Submit sale edit     |
| POST   | `/labels/{labelId}/sales/{saleId}/delete` | Delete sale   |

#### Return endpoints (NEW)

| Method | Route                                          | Description              |
|--------|------------------------------------------------|--------------------------|
| GET    | `/labels/{labelId}/returns`                    | List returns for label   |
| GET    | `/labels/{labelId}/returns/new`                | New return form          |
| POST   | `/labels/{labelId}/returns`                    | Submit new return        |
| GET    | `/labels/{labelId}/returns/{returnId}`         | Return detail            |
| GET    | `/labels/{labelId}/returns/{returnId}/edit`    | Edit return form         |
| POST   | `/labels/{labelId}/returns/{returnId}`         | Submit return edit       |
| POST   | `/labels/{labelId}/returns/{returnId}/delete`  | Delete return            |

---

## 5. Implementation Plan

### Phase 1: Refactor InventoryMovement to Bidirectional Model

**Description**: Replace the single-sided `(distributorId, quantityDelta)` schema with a
bidirectional `(fromLocationType, fromLocationId, toLocationType, toLocationId, quantity)` model.
This is the foundational change all other phases depend on.

**Files to create or modify**:
- `inventory/domain/LocationType.java` (NEW enum)
- `inventory/inventorymovement/domain/InventoryMovement.java` (refactor fields)
- `inventory/inventorymovement/infrastructure/InventoryMovementEntity.java` (refactor fields)
- `inventory/inventorymovement/infrastructure/InventoryMovementRepository.java`
  (add new queries)
- `inventory/inventorymovement/api/InventoryMovementCommandApi.java` (new signature + delete)
- `inventory/inventorymovement/api/InventoryMovementQueryApi.java` (add new query methods)
- `inventory/inventorymovement/application/RecordMovementUseCase.java` (new logic)
- `inventory/inventorymovement/application/DeleteMovementsUseCase.java` (NEW)
- `inventory/inventorymovement/application/InventoryMovementCommandApiImpl.java`
- `inventory/inventorymovement/application/InventoryMovementQueryApiImpl.java`
- `db/migration/V25__refactor_inventory_movement_bidirectional.sql`

**Acceptance criteria**:
- `V25` migration runs cleanly: adds new columns, migrates existing ALLOCATION records
  (`from=WAREHOUSE, to=DISTRIBUTOR`) and existing SALE records (`from=DISTRIBUTOR, to=EXTERNAL`),
  drops old columns.
- `InventoryMovementCommandApi.recordMovement(...)` accepts the new signature.
- `InventoryMovementCommandApi.deleteMovementsByReference(...)` deletes all movements with
  matching `movementType` and `referenceId`.
- `InventoryMovementQueryApi.getCurrentInventory(productionRunId, distributorId)` returns the
  correct sum.
- `InventoryMovementQueryApi.getWarehouseInventory(productionRunId)` returns the correct sum.
- All existing callers of the old API (`AllocationCommandApiImpl`, `RegisterSaleUseCase`)
  updated to use the new signature.
- Integration tests pass.

**Dependencies**: none

---

### Phase 2: Remove unitsSold from ChannelAllocation

**Description**: Remove the `unitsSold` denormalization from `ChannelAllocation` now that
current inventory is derived from movements. Remove `reduceAllocation()` from
`AllocationCommandApi`.

**Files to create or modify**:
- `db/migration/V26__remove_units_sold_from_channel_allocation.sql`
- `inventory/allocation/infrastructure/ChannelAllocationEntity.java` (remove field/method)
- `inventory/allocation/domain/ChannelAllocation.java` (remove `unitsSold`)
- `inventory/allocation/api/AllocationCommandApi.java` (remove `reduceAllocation`)
- `inventory/allocation/application/AllocationCommandApiImpl.java`
- `inventory/allocation/application/ReduceAllocationUseCase.java` (DELETE)
- `sales/sale/application/RegisterSaleUseCase.java` (remove `allocationCommandApi.reduceAllocation` call)

**Acceptance criteria**:
- `units_sold` column removed from `channel_allocation` table.
- `AllocationCommandApi` no longer has `reduceAllocation()`.
- `RegisterSaleUseCase` no longer calls `reduceAllocation()`.
- Integration tests pass.

**Dependencies**: Phase 1

---

### Phase 3: Add distributorId to Sale + Inventory Validation via Movements

**Description**: Store `distributorId` explicitly on the `Sale` entity. Update
`RegisterSaleUseCase` to validate inventory availability using
`InventoryMovementQueryApi.getCurrentInventory()` instead of the removed `reduceAllocation()`.

**Files to create or modify**:
- `db/migration/V27__add_distributor_id_to_sale.sql`
- `sales/sale/infrastructure/SaleEntity.java` (add `distributorId` field)
- `sales/sale/infrastructure/SaleRepository.java` (add `findByDistributorId`, `findByProductionRunId`)
- `sales/sale/domain/Sale.java` (add `distributorId`)
- `sales/sale/application/RegisterSaleUseCase.java` (validate via movements, store distributorId)
- `sales/sale/application/SaleQueryApiImpl.java` (add new query methods)
- `sales/sale/api/SaleQueryApi.java` (add `getSalesForDistributor`, `getSalesForProductionRun`)

**Acceptance criteria**:
- `sale.distributor_id` column exists and is NOT NULL.
- Existing sale records have `distributor_id` backfilled from the determined distributor.
- `RegisterSaleUseCase` throws `InsufficientInventoryException` when
  `getCurrentInventory(productionRunId, distributorId) < requestedQuantity`.
- `SaleQueryApi.getSalesForDistributor(distributorId)` returns correct results.
- Integration tests cover insufficient inventory rejection.

**Dependencies**: Phase 2

---

### Phase 4: Sale Edit and Delete

**Description**: Implement `updateSale()` and `deleteSale()` on `SaleCommandApi`. Add edit/delete
endpoints to `SaleController`.

**Files to create or modify**:
- `sales/sale/api/SaleCommandApi.java` (add `updateSale`, `deleteSale`)
- `sales/sale/api/SaleController.java` (add edit GET/POST, delete POST)
- `sales/sale/application/UpdateSaleUseCase.java` (NEW)
- `sales/sale/application/DeleteSaleUseCase.java` (NEW)
- `sales/sale/application/SaleCommandApiImpl.java`
- `templates/sale/edit.html` (NEW)
- `templates/sale/detail.html` (add Edit and Delete buttons)

**Edit logic** (`UpdateSaleUseCase`):
1. Delete all movements with `deleteMovementsByReference(SALE, saleId)`.
2. For each new line item: validate inventory, record new SALE movement with same `saleId`.
3. Update sale entity (replace line items, recalculate total).

**Delete logic** (`DeleteSaleUseCase`):
1. `deleteMovementsByReference(SALE, saleId)`.
2. `saleRepository.deleteById(saleId)`.

**Acceptance criteria**:
- Editing a sale adjusts movements and inventory correctly.
- Editing with an invalid quantity (exceeds available) returns a validation error.
- Deleting a sale removes the entity and all related movements.
- Integration tests cover edit and delete.

**Dependencies**: Phase 3

---

### Phase 5: Return Module

**Description**: Create the `sales/distributor_return` module for recording distributor returns
back to warehouse.

**Files to create or modify**:
- `db/migration/V28__create_distributor_return_tables.sql`
- `sales/distributor_return/domain/DistributorReturn.java` (NEW)
- `sales/distributor_return/domain/ReturnLineItem.java` (NEW)
- `sales/distributor_return/domain/ReturnLineItemInput.java` (NEW)
- `sales/distributor_return/infrastructure/DistributorReturnEntity.java` (NEW)
- `sales/distributor_return/infrastructure/ReturnLineItemEntity.java` (NEW)
- `sales/distributor_return/infrastructure/DistributorReturnRepository.java` (NEW)
- `sales/distributor_return/api/DistributorReturnCommandApi.java` (NEW)
- `sales/distributor_return/api/DistributorReturnQueryApi.java` (NEW)
- `sales/distributor_return/api/ReturnController.java` (NEW)
- `sales/distributor_return/api/RegisterReturnForm.java` (NEW)
- `sales/distributor_return/api/ReturnLineItemForm.java` (NEW)
- `sales/distributor_return/application/RegisterReturnUseCase.java` (NEW)
- `sales/distributor_return/application/UpdateReturnUseCase.java` (NEW)
- `sales/distributor_return/application/DeleteReturnUseCase.java` (NEW)
- `sales/distributor_return/application/DistributorReturnCommandApiImpl.java` (NEW)
- `sales/distributor_return/application/DistributorReturnQueryApiImpl.java` (NEW)
- `templates/return/list.html` (NEW)
- `templates/return/detail.html` (NEW)
- `templates/return/register.html` (NEW)
- `templates/return/edit.html` (NEW)

**RegisterReturn logic**:
1. Validate label and distributor exist.
2. For each line item:
   - Find most-recent production run for release+format.
   - `getCurrentInventory(productionRunId, distributorId) >= quantity`, else reject.
3. Save return entity → get `returnId`.
4. For each line item: `recordMovement(from=DISTRIBUTOR(distributorId), to=WAREHOUSE,
   RETURN, returnId)`.

**Acceptance criteria**:
- Can create, view, edit, and delete returns.
- Recording a return creates correct RETURN movements.
- Distributor's current inventory decreases; warehouse inventory increases.
- Cannot return more than current distributor inventory.
- Integration tests cover all CRUD operations and insufficient inventory rejection.

**Dependencies**: Phase 3

---

### Phase 6: Inventory Visibility on Release Page

**Description**: Add an inventory status section to the release detail page showing current
warehouse inventory, per-distributor breakdown (allocated vs current vs sold), and movement
history.

**Files to create or modify**:
- `inventory/inventorymovement/api/InventoryMovementQueryApi.java`
  (add `getCurrentInventoryByDistributor`, `getMovementsForProductionRun`)
- `inventory/inventorymovement/application/InventoryMovementQueryApiImpl.java`
- `inventory/inventorymovement/infrastructure/InventoryMovementRepository.java`
- `catalog/release/api/ReleaseController.java` (add inventory data to release detail model)
- `templates/release/detail.html` (add inventory section)

**View model** (assembled in controller from multiple APIs, not a new domain class):

```
For each production run of the release:
  - format, totalManufactured
  - warehouseInventory    ← getWarehouseInventory(productionRunId)
  - per distributor:
      - distributorName
      - allocated          ← ChannelAllocation.quantity (original, immutable)
      - current            ← getCurrentInventory(productionRunId, distributorId)
      - sold               ← allocated - current
  - movements (newest first) ← getMovementsForProductionRun(productionRunId)
```

**Acceptance criteria**:
- Release detail page shows inventory section.
- Warehouse and per-distributor inventory figures are correct.
- Movement history shows all ALLOCATION, SALE, and RETURN movements, newest first.
- No new domain model needed — view assembled in controller.

**Dependencies**: Phase 5

---

### Phase 7: Sales & Returns History on Distributor and Release Pages

**Description**: Add sales and return history to the distributor detail page. Add sales history
to the release detail page (by querying production runs for that release).

**Files to create or modify**:
- `distribution/distributor/api/DistributorController.java` (add sales and returns to detail)
- `templates/distributor/detail.html` (add sales and returns sections)
- `templates/release/detail.html` (add sales-by-release section)

**Acceptance criteria**:
- Distributor detail page lists all sales and returns for that distributor.
- Release detail page lists all sales for that release (across all distributors and production runs).
- Sales and returns are sorted newest first.

**Dependencies**: Phase 5

---

## 6. Technology Stack

| Layer          | Choice                | Rationale                       |
|----------------|-----------------------|---------------------------------|
| Language       | Java 25               | Existing                        |
| Framework      | Spring Boot 4         | Existing                        |
| Templates      | Thymeleaf + Bootstrap 5 | Existing                      |
| Database       | PostgreSQL            | Existing                        |
| Migrations     | Flyway                | Existing                        |
| Build          | Gradle                | Existing                        |
| Test (Java)    | JUnit 5, Mockito, TestContainers | Existing           |
| Test (JS)      | Vitest                | Existing                        |

---

## 7. Non-Functional Requirements

### Performance

- Inventory calculations (`getCurrentInventory`) query `inventory_movement` table with
  indexed `production_run_id`, `from_location_type`/`to_location_type` columns. Acceptable
  for the expected data volumes (hundreds to low thousands of movements per label).

### Security

- All endpoints require authentication (existing Spring Security configuration applies).
- Wholesale prices in sale records are not exposed publicly.

### Observability

- Use existing logging conventions: INFO for business events (sale created, return registered),
  DEBUG for internal steps (line item processed), WARN for validation failures.

### Data Integrity

- All inventory changes within a single sale/return must be atomic (all wrapped in
  `@Transactional`).
- Deleting a sale/return must delete movements in the same transaction.

---

## 8. Open Questions

None — all questions resolved in design conversation.

---

## 9. Database Migrations

### V25: Refactor inventory_movement to bidirectional model

```sql
-- 1. Add new columns
ALTER TABLE inventory_movement
    ADD COLUMN from_location_type VARCHAR(20),
    ADD COLUMN from_location_id   BIGINT,
    ADD COLUMN to_location_type   VARCHAR(20),
    ADD COLUMN to_location_id     BIGINT,
    ADD COLUMN quantity           INT;

-- 2. Migrate existing ALLOCATION records
--    Old: distributorId + positive quantityDelta = units going TO distributor
UPDATE inventory_movement SET
    from_location_type = 'WAREHOUSE',
    from_location_id   = NULL,
    to_location_type   = 'DISTRIBUTOR',
    to_location_id     = distributor_id,
    quantity           = quantity_delta
WHERE movement_type = 'ALLOCATION';

-- 3. Migrate existing SALE records
--    Old: distributorId + negative quantityDelta = units going OUT from distributor
UPDATE inventory_movement SET
    from_location_type = 'DISTRIBUTOR',
    from_location_id   = distributor_id,
    to_location_type   = 'EXTERNAL',
    to_location_id     = NULL,
    quantity           = ABS(quantity_delta)
WHERE movement_type = 'SALE';

-- 4. Make new columns NOT NULL
ALTER TABLE inventory_movement
    ALTER COLUMN from_location_type SET NOT NULL,
    ALTER COLUMN to_location_type   SET NOT NULL,
    ALTER COLUMN quantity            SET NOT NULL;

-- 5. Drop old columns
ALTER TABLE inventory_movement
    DROP COLUMN distributor_id,
    DROP COLUMN quantity_delta;

-- 6. Add index on new columns
CREATE INDEX idx_inventory_movement_production_run_id
    ON inventory_movement(production_run_id);
CREATE INDEX idx_inventory_movement_from_location
    ON inventory_movement(production_run_id, from_location_type, from_location_id);
CREATE INDEX idx_inventory_movement_to_location
    ON inventory_movement(production_run_id, to_location_type, to_location_id);
CREATE INDEX idx_inventory_movement_reference
    ON inventory_movement(movement_type, reference_id);
```

### V26: Remove units_sold from channel_allocation

```sql
ALTER TABLE channel_allocation DROP COLUMN units_sold;
```

### V27: Add distributor_id to sale

```sql
ALTER TABLE sale ADD COLUMN distributor_id BIGINT REFERENCES distributor(id);

-- Backfill: for DIRECT sales, find the DIRECT distributor for the label
UPDATE sale s
SET distributor_id = d.id
FROM distributor d
WHERE d.label_id = s.label_id
  AND d.channel_type = 'DIRECT'
  AND s.channel = 'DIRECT';

-- Backfill: for DISTRIBUTOR channel sales, the distributor_id was already selected
-- by the user but never persisted — must be resolved manually or from movement records
UPDATE sale s
SET distributor_id = (
    SELECT im.from_location_id
    FROM inventory_movement im
    WHERE im.reference_id = s.id
      AND im.movement_type = 'SALE'
      AND im.from_location_type = 'DISTRIBUTOR'
    LIMIT 1
)
WHERE s.channel != 'DIRECT' AND s.distributor_id IS NULL;

ALTER TABLE sale ALTER COLUMN distributor_id SET NOT NULL;
CREATE INDEX idx_sale_distributor_id ON sale(distributor_id);
```

### V28: Create distributor_return tables

```sql
CREATE TABLE distributor_return (
    id             BIGSERIAL PRIMARY KEY,
    label_id       BIGINT NOT NULL REFERENCES label(id) ON DELETE CASCADE,
    distributor_id BIGINT NOT NULL REFERENCES distributor(id),
    return_date    DATE NOT NULL,
    notes          TEXT,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_distributor_return_label_id       ON distributor_return(label_id);
CREATE INDEX idx_distributor_return_distributor_id ON distributor_return(distributor_id);
CREATE INDEX idx_distributor_return_date           ON distributor_return(return_date);

CREATE TABLE distributor_return_line_item (
    id        BIGSERIAL PRIMARY KEY,
    return_id BIGINT NOT NULL REFERENCES distributor_return(id) ON DELETE CASCADE,
    release_id BIGINT NOT NULL REFERENCES release(id),
    format    VARCHAR(20) NOT NULL,
    quantity  INT NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_return_line_item_return_id  ON distributor_return_line_item(return_id);
CREATE INDEX idx_return_line_item_release_id ON distributor_return_line_item(release_id);
```

---

## 10. File Structure

```
src/main/java/org/omt/labelmanager/

inventory/
  domain/
    LocationType.java                          ← NEW
    MovementType.java                          (unchanged)
  inventorymovement/
    domain/
      InventoryMovement.java                   ← MODIFIED (new fields)
    infrastructure/
      InventoryMovementEntity.java             ← MODIFIED (new schema)
      InventoryMovementRepository.java         ← MODIFIED (new queries)
    api/
      InventoryMovementCommandApi.java         ← MODIFIED (new signature)
      InventoryMovementQueryApi.java           ← MODIFIED (new methods)
    application/
      RecordMovementUseCase.java               ← MODIFIED
      DeleteMovementsUseCase.java              ← NEW
      InventoryMovementCommandApiImpl.java     ← MODIFIED
      InventoryMovementQueryApiImpl.java       ← MODIFIED
  allocation/
    domain/
      ChannelAllocation.java                   ← MODIFIED (remove unitsSold)
    infrastructure/
      ChannelAllocationEntity.java             ← MODIFIED (remove unitsSold)
    api/
      AllocationCommandApi.java                ← MODIFIED (remove reduceAllocation)
    application/
      AllocationCommandApiImpl.java            ← MODIFIED
      ReduceAllocationUseCase.java             ← DELETED

sales/
  sale/
    domain/
      Sale.java                                ← MODIFIED (add distributorId)
    infrastructure/
      SaleEntity.java                          ← MODIFIED (add distributorId)
      SaleRepository.java                      ← MODIFIED (add queries)
    api/
      SaleCommandApi.java                      ← MODIFIED (add update/delete)
      SaleQueryApi.java                        ← MODIFIED (add queries)
      SaleController.java                      ← MODIFIED (add edit/delete endpoints)
    application/
      RegisterSaleUseCase.java                 ← MODIFIED (new movement API, validation)
      UpdateSaleUseCase.java                   ← NEW
      DeleteSaleUseCase.java                   ← NEW
      SaleCommandApiImpl.java                  ← MODIFIED
      SaleQueryApiImpl.java                    ← MODIFIED

  distributor_return/                          ← NEW MODULE
    domain/
      DistributorReturn.java
      ReturnLineItem.java
      ReturnLineItemInput.java
    infrastructure/
      DistributorReturnEntity.java
      ReturnLineItemEntity.java
      DistributorReturnRepository.java
    api/
      DistributorReturnCommandApi.java
      DistributorReturnQueryApi.java
      ReturnController.java
      RegisterReturnForm.java
      ReturnLineItemForm.java
    application/
      RegisterReturnUseCase.java
      UpdateReturnUseCase.java
      DeleteReturnUseCase.java
      DistributorReturnCommandApiImpl.java
      DistributorReturnQueryApiImpl.java

distribution/
  distributor/
    api/
      DistributorController.java               ← MODIFIED (add sales/returns)

catalog/
  release/
    api/
      ReleaseController.java                   ← MODIFIED (add inventory section)

src/main/resources/db/migration/
  V25__refactor_inventory_movement_bidirectional.sql  ← NEW
  V26__remove_units_sold_from_channel_allocation.sql  ← NEW
  V27__add_distributor_id_to_sale.sql                 ← NEW
  V28__create_distributor_return_tables.sql            ← NEW

src/main/resources/templates/
  sale/
    list.html       (existing)
    register.html   (existing)
    detail.html     ← MODIFIED (add edit/delete buttons)
    edit.html       ← NEW
  return/
    list.html       ← NEW
    register.html   ← NEW
    detail.html     ← NEW
    edit.html       ← NEW
  distributor/
    detail.html     ← MODIFIED (add sales and returns)
  release/
    detail.html     ← MODIFIED (add inventory section and sales history)
```
