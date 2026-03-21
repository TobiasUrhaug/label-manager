# Spec: label-distributor-allocation

## Status
In Progress

## Approach

Remove the `inventory/allocation` module entirely. All stock changes (allocation, return, sale) are already captured in `inventory_movement`. The allocation module is redundant — `channel_allocation` rows are written in parallel with movement rows, so dropping the table loses no data.

After removal:
- The "Allocate" button posts to a new `AllocateController` (in `productionrun/api/`) that writes directly to `InventoryMovementCommandApi` after validating available warehouse stock.
- A new Bandcamp cancellation endpoint on the same controller records a RETURN movement from BANDCAMP → WAREHOUSE.
- `validateQuantityIsAvailable()` is rewritten to derive available warehouse stock from `InventoryMovementQueryApi.getWarehouseInventory()` (already returns the correct net delta; available = `productionRun.quantity() + warehouseInventoryDelta`).
- `AgreementController`, `SaleLineItemProcessor`, and `ReleaseController` drop their `AllocationQueryApi` dependencies and use movement-based queries instead.
- `BANDCAMP` is added to `LocationType` and `InventoryLocation` as the data model for Bandcamp reservations. No UI endpoint for recording Bandcamp sales is included — that is handled by the `sales-recording-distributor` feature.

The `inventorymovement` module gains two new query methods. No new module is introduced. The allocation HTTP endpoint moves from `AllocationController` to a new thin `AllocateController` within the `productionrun` module (sub-resource of production runs; same URL).

## Files to Create or Modify

### inventory/domain
| File | Action | Purpose |
|------|--------|---------|
| `inventory/domain/LocationType.java` | Modify | Add `BANDCAMP` enum value |
| `inventory/domain/InventoryLocation.java` | Modify | Add `bandcamp()` factory method |

### inventory/inventorymovement
| File | Action | Purpose |
|------|--------|---------|
| `inventory/inventorymovement/api/InventoryMovementQueryApi.java` | Modify | Add `getBandcampInventory(Long productionRunId)` and `getProductionRunIdsAllocatedToDistributor(Long distributorId)` |
| `inventory/inventorymovement/application/InventoryMovementQueryApiImpl.java` | Modify | Implement the two new methods |
| `inventory/inventorymovement/infrastructure/InventoryMovementRepository.java` | Modify | Add JPQL query for distinct production run IDs allocated to a distributor |

### inventory/productionrun
| File | Action | Purpose |
|------|--------|---------|
| `inventory/productionrun/application/ProductionRunQueryApiImpl.java` | Modify | Rewrite `validateQuantityIsAvailable()` to use `InventoryMovementQueryApi.getWarehouseInventory()` instead of `AllocationQueryApi` |
| `inventory/productionrun/api/AllocateController.java` | Create | Handles POST for allocate and Bandcamp cancellation; replaces `AllocationController` |
| `inventory/productionrun/api/AllocateForm.java` | Create | Form for allocation (distributorId, locationType, quantity); replaces `AddAllocationForm` |
| `inventory/productionrun/api/CancelBandcampReservationForm.java` | Create | Form for Bandcamp cancellation (quantity) |

### inventory/api (shared view models)
| File | Action | Purpose |
|------|--------|---------|
| `inventory/api/ProductionRunWithAllocation.java` | Modify | Remove `allocationViews` and `allocated`/`unallocated` fields; add `bandcampInventory` (int) |
| `inventory/api/AllocationView.java` | Delete | No longer needed; allocation records gone |
| `inventory/api/DistributorInventoryView.java` | Modify | Remove `allocated` field; keep only `name` and `current` (current stock derived from movements) |

### inventory/allocation (full package — delete)
| File | Action | Purpose |
|------|--------|---------|
| `inventory/allocation/api/AllocationCommandApi.java` | Delete | |
| `inventory/allocation/api/AllocationQueryApi.java` | Delete | |
| `inventory/allocation/api/AllocationController.java` | Delete | Replaced by `AllocateController` |
| `inventory/allocation/api/AddAllocationForm.java` | Delete | Replaced by `AllocateForm` |
| `inventory/allocation/application/AllocationCommandApiImpl.java` | Delete | |
| `inventory/allocation/application/AllocationQueryApiImpl.java` | Delete | |
| `inventory/allocation/domain/ChannelAllocation.java` | Delete | |
| `inventory/allocation/infrastructure/ChannelAllocationEntity.java` | Delete | |
| `inventory/allocation/infrastructure/ChannelAllocationRepository.java` | Delete | |

### Other modules
| File | Action | Purpose |
|------|--------|---------|
| `catalog/release/api/ReleaseController.java` | Modify | Remove `AllocationQueryApi` injection; rewrite `buildProductionRunWithAllocation()` and `buildDistributorInventories()` using only movement APIs; add Bandcamp inventory to model |
| `distribution/agreement/api/AgreementController.java` | Modify | Replace `allocationQueryApi.getAllocationsForDistributor()` with `inventoryMovementQueryApi.getProductionRunIdsAllocatedToDistributor()` |
| `sales/sale/application/SaleLineItemProcessor.java` | Modify | Remove `AllocationQueryApi` injection and the prior-allocation existence check |

### Frontend
| File | Action | Purpose |
|------|--------|---------|
| `src/main/resources/templates/release/release.html` (verify path) | Modify | Remove allocation details dropdown; update inventory table (remove Allocated column, add Bandcamp row); add Bandcamp cancellation modal; update allocate modal to support BANDCAMP as location type; update form action URL |
| `src/main/resources/static/js/allocate-form.js` | Create | Extract inline allocation JS from `release.html` |
| `src/test/js/allocate-form.test.js` | Create | Vitest unit tests for allocate-form.js |

### Database
| File | Action | Purpose |
|------|--------|---------|
| `src/main/resources/db/migration/V31__drop_channel_allocation_table.sql` | Create | Drop `channel_allocation` table and its indexes |

### Tests (delete)
| File | Action | Purpose |
|------|--------|---------|
| `AllocationControllerTest.java` | Delete | Replaced by `AllocateControllerTest` |

### Tests (create/modify)
| File | Action | Purpose |
|------|--------|---------|
| `inventory/productionrun/api/AllocateControllerTest.java` | Create | Tests for allocate and Bandcamp cancellation endpoints |
| `inventory/inventorymovement/application/InventoryMovementQueryApiImplTest.java` | Modify/Create | Tests for `getBandcampInventory()` and `getProductionRunIdsAllocatedToDistributor()` |
| `inventory/productionrun/application/ProductionRunQueryApiImplTest.java` | Modify/Create | Test rewritten `validateQuantityIsAvailable()` |
| `distribution/agreement/api/AgreementControllerTest.java` | Modify | Update mocks: remove `AllocationQueryApi`, add `InventoryMovementQueryApi` |
| `sales/sale/application/SaleLineItemProcessorTest.java` | Modify | Remove prior-allocation test cases; verify stock-only validation remains |

## Data Models / Interfaces

### LocationType (updated)
```java
public enum LocationType {
    WAREHOUSE,
    DISTRIBUTOR,
    BANDCAMP,   // new
    EXTERNAL
}
```

### InventoryLocation (updated)
```java
public static InventoryLocation bandcamp() {
    return new InventoryLocation(LocationType.BANDCAMP, null);
}
```

### InventoryMovementQueryApi (updated)
```java
/** Returns net Bandcamp stock: sum(ALLOCATION to BANDCAMP) − sum(SALE from BANDCAMP) − sum(RETURN from BANDCAMP). */
int getBandcampInventory(Long productionRunId);

/** Returns distinct production run IDs that have at least one ALLOCATION movement to the given distributor. */
List<Long> getProductionRunIdsAllocatedToDistributor(Long distributorId);
```

### InventoryMovementRepository (updated)
```java
@Query("""
    SELECT DISTINCT m.productionRunId
    FROM InventoryMovementEntity m
    WHERE m.toLocationType = 'DISTRIBUTOR'
      AND m.toLocationId = :distributorId
      AND m.movementType = 'ALLOCATION'
    """)
List<Long> findDistinctProductionRunIdsAllocatedToDistributor(@Param("distributorId") Long distributorId);
```

### AllocateController endpoints
```
POST /labels/{labelId}/releases/{releaseId}/production-runs/{runId}/allocations
  Body: AllocateForm { locationType (DISTRIBUTOR|BANDCAMP), distributorId (nullable), quantity }
  Flow:
    1. productionRunQueryApi.validateQuantityIsAvailable(runId, quantity)  — throws InsufficientInventoryException
    2. inventoryMovementCommandApi.recordMovement(runId, WAREHOUSE, toLocation, quantity, ALLOCATION, null)
    3. Redirect to release detail; flash error on InsufficientInventoryException

POST /labels/{labelId}/releases/{releaseId}/production-runs/{runId}/bandcamp-cancellations
  Body: CancelBandcampReservationForm { quantity }
  Flow:
    1. int held = inventoryMovementQueryApi.getBandcampInventory(runId)
    2. if quantity > held → flash error + redirect
    3. inventoryMovementCommandApi.recordMovement(runId, BANDCAMP, WAREHOUSE, quantity, RETURN, null)
    4. Redirect to release detail
```

### ProductionRunWithAllocation (updated fields)
Remove: `allocated`, `unallocated`, `allocationViews`
Add: `bandcampInventory` (int — current units held by Bandcamp, from `getBandcampInventory()`)
Keep: `productionRun`, `warehouseInventory`, `distributorInventories`, `movements`

Note: `warehouseInventory` value stored in this record should be absolute available stock: `productionRun.quantity() + getWarehouseInventory()` (since `getWarehouseInventory()` returns a net delta).

### DistributorInventoryView (simplified)
Remove: `allocated` field
Keep: `name` (String), `current` (int — current stock from movements)
Remove: `sold()` derived method (or retain if the template uses it — Developer to verify)

### AllocateForm
```java
public class AllocateForm {
    private LocationType locationType;   // DISTRIBUTOR or BANDCAMP
    private Long distributorId;          // required when locationType == DISTRIBUTOR
    private int quantity;
}
```

### CancelBandcampReservationForm
```java
public class CancelBandcampReservationForm {
    private int quantity;
}
```

## Integration Points

| System/Module | How it's used | Who owns it |
|---------------|---------------|-------------|
| `InventoryMovementCommandApi` | Records all movements (allocate, cancel) | inventory/inventorymovement |
| `InventoryMovementQueryApi` | Derives warehouse/Bandcamp/distributor stock; finds allocated production runs per distributor | inventory/inventorymovement |
| `ProductionRunQueryApi` | `validateQuantityIsAvailable()` used in `AllocateController` | inventory/productionrun |
| `DistributorQueryApi` (or repo) | Resolve distributor name for display | distribution/distributor |
| `InsufficientInventoryException` | Thrown by `validateQuantityIsAvailable()`; caught by `AllocateController` | inventory (top-level api package) |

## Assumptions

- All existing allocation history is already fully mirrored in `inventory_movement` (confirmed by context.md and V26 migration).
- `getWarehouseInventory(productionRunId)` returns the net delta (inbound − outbound from warehouse perspective). Available warehouse stock = `productionRun.quantity() + delta`. Developer should verify this assumption against `InventoryMovementQueryApiImpl` before implementing Task 4.
- No production run has a `channel_allocation` row without a corresponding `inventory_movement` row. Safe to drop the table.
- FR-3c (Bandcamp sales) is data-model-only in this feature; the UI endpoint is out of scope.

## Risks

- **ReleaseController complexity**: `buildProductionRunWithAllocation()` currently assembles data from multiple sources. Removing allocation queries simplifies it, but the Developer should read the full method before modifying.
- **AgreementController query semantics**: The new `getProductionRunIdsAllocatedToDistributor()` query must return production runs with any historical ALLOCATION movement (even if all units have since been returned). This matches the old behaviour (AllocationQueryApi returned all allocation records regardless of returns). Developer must verify this is the intended behaviour.
- **DistributorInventoryView simplification**: Removing the `allocated` field may break Thymeleaf template references. Developer must grep for `allocated` in `release.html` before deleting the field.

## Key Decisions

- **AllocateController in `productionrun/api/`**: No new module is created. The controller is a thin HTTP adapter calling two existing module APIs. Moving it to `productionrun/api/` keeps it co-located with the resource it operates on and avoids creating a one-file module.
  - Why: The allocation operation's primary concern is the production run (validating and debiting its stock). Fits naturally as a sub-resource controller.
  - Alternatives considered: New `inventory/allocate/` module (unnecessary overhead for a thin controller), keep in `inventory/allocation/` (entire package is being deleted).

- **`getBandcampInventory()` implementation**: Uses the same `sumQuantityTo` / `sumQuantityFrom` pattern already in `InventoryMovementQueryApiImpl`, with `LocationType.BANDCAMP` and `locationId=null`.
  - Why: Consistent with existing `getWarehouseInventory()`. No new repository queries needed for Bandcamp inventory.

- **`getProductionRunIdsAllocatedToDistributor()` as a repository query**: Cross-production-run query; cannot be done with the existing per-run `findByProductionRunIdOrderByOccurredAtDesc` pattern. Requires a dedicated JPQL query.

- **`DistributorInventoryView.allocated` removed**: Requirements (AC-5.1) only specify showing current stock per distributor. The `allocated` field required querying allocation records; replacing it with a movement-derived value requires a new query method not justified by the requirements.

- **Bandcamp cancellation: stock-based validation**: The cancellable quantity is capped at `getBandcampInventory()` (current held units = allocated − sold − previously cancelled). This matches FR-3b: "Cancellable quantity = sum(ALLOCATION to BANDCAMP) − sum(SALE from BANDCAMP)". Note: RETURN movements also reduce the held quantity, so previously cancelled amounts are already excluded.

## Open Questions

None. FR-3c scope confirmed: BANDCAMP data model only; no sales UI endpoint in this feature.
