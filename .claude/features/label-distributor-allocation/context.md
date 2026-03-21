# Context: label-distributor-allocation

## Status
In Progress

## Background
The system has a standalone `allocation` module (`inventory/allocation`) that tracks how many units from a production run have been assigned to each distributor via a dedicated `channel_allocation` table.

However, when an allocation is created today, **two records are written**:
1. A row in `channel_allocation`
2. A row in `inventory_movement` with `movement_type = ALLOCATION`

The `channel_allocation` table is therefore redundant — the inventory movement captures the same information. The allocation module is a separate domain concept that adds complexity without adding value.

This feature removes the allocation module entirely and replaces it with direct inventory movement writes, unifying all stock changes (allocation, sale, return) under the `inventory_movement` table.

## User Story
As a label manager, when I create a production run of physical records, I want to record how many units are shipped to each distributor and how many are reserved for Bandcamp, so that I always know where my inventory is and how much is freely available in the warehouse.

## Existing Code Affected

| File / Class | What changes |
|---|---|
| `inventory/allocation/` (entire package) | Deleted |
| `inventory/productionrun/application/ProductionRunQueryApiImpl` | `validateQuantityIsAvailable()` rewritten to sum `ALLOCATION` movements instead of calling `AllocationQueryApi` |
| `catalog/release/api/ReleaseController` | Build inventory view from movements only; remove all `AllocationQueryApi` calls |
| `distribution/agreement/api/AgreementController` | Find distributor's allocated production runs via `ALLOCATION` movements instead of `AllocationQueryApi` |
| `sales/sale/application/SaleLineItemProcessor` | Remove prior-allocation validation; stock-only check remains |
| `release.html` (Thymeleaf template) | Remove allocation details dropdown; keep allocate button (simplified); simplify inventory table |
| `V_next__drop_channel_allocation_table.sql` | New migration to drop `channel_allocation` table |

## Dependencies

- **inventory/productionrun module**: Provides the production run total (manufactured quantity). Allocation writes directly into inventory movements referencing the production run.
- **inventory/inventorymovement module**: The `ALLOCATION` movement type already exists. The allocate button will now call `InventoryMovementCommandApi` directly.
- **distribution/distributor module**: Allocations reference distributors by ID.
- **sales-recording-distributor feature**: Depends on distributor current stock being derivable from movements. This is already how it works — the change is that the fallback allocation check in `SaleLineItemProcessor` is removed. No functional change to sales recording beyond that.

## Constraints

- The `channel_allocation` table can be safely dropped. Existing allocation history is already fully captured in `inventory_movement` (both were written in parallel on every allocation).
- Returns must always go to warehouse first — no direct distributor-to-distributor transfers.
- Warehouse stock is implicit: unallocated units are in the warehouse.
- Bandcamp is modelled as `LocationType.BANDCAMP`. A reservation = `ALLOCATION` from `WAREHOUSE → BANDCAMP`. Units physically stay in the warehouse but are no longer freely available. Cancellations = `RETURN` from `BANDCAMP → WAREHOUSE`, capped at `allocated − sold`. Sales are recorded as `SALE` from `BANDCAMP`, entered manually from periodic Bandcamp reports.

## Prior Art

- `sales-recording-distributor` feature (`requirements.md`) already defines the inventory model used here: movements with `from`/`to` locations and `movement_type`. This feature aligns with and simplifies that model.
- The V25 migration (`refactor_inventory_movement_bidirectional`) already migrated the movement table to a bidirectional from/to model. The data is ready.
- V26 migration already removed `units_sold` from `channel_allocation`, confirming the direction of travel: tracking via movements, not allocation records.
