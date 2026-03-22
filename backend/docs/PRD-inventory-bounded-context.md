# Feature: Inventory Bounded Context

## Problem

Labels manufacture physical products (vinyl, CDs, cassettes) in production runs and distribute them across multiple sales channels (warehouse, Bandcamp, distributors, concert merch). Currently, there is no way to:

- Track how many units from each production run are allocated to each channel
- Record sales, transfers, or adjustments that affect stock levels
- Know the current stock balance per channel and production run

Without inventory tracking, label owners cannot answer basic questions like "How many copies of the black vinyl pressing do I have left at my warehouse?" or "How many units did I send to Distributor A?"

## Requirements

### Channel Allocation

- [ ] Allocate units from a production run to a sales channel
- [ ] Allow multiple allocations to the same channel (e.g., send 50 now, 50 later)
- [ ] Prevent allocations that exceed available unallocated quantity
- [ ] Show unallocated quantity remaining for each production run

### Inventory Movements

- [ ] Record all stock changes as movements with quantity delta and type
- [ ] Support movement types:
  - [ ] Allocation (+N to channel)
  - [ ] Sale (-N from channel)
  - [ ] TransferOut (-N from source channel)
  - [ ] TransferIn (+N to destination channel)
  - [ ] Return (+N to channel)
  - [ ] Adjustment (+/- N for corrections, damage, loss)
- [ ] Link movements to production run and sales channel
- [ ] Store timestamp and optional reference ID for each movement
- [ ] Prevent movements that would result in negative stock (configurable)

### Stock Balance

- [ ] Calculate current stock per production run and sales channel
- [ ] Derive balances from movement history (not stored directly)
- [ ] Display stock balances in the UI grouped by channel

### Transfers

- [ ] Transfer stock between sales channels
- [ ] Create paired movements (TransferOut + TransferIn) atomically
- [ ] Validate source channel has sufficient stock

## Acceptance Criteria

### Allocation

- Given a production run with 500 manufactured and 0 allocated, when I allocate 200 to Warehouse, then 200 units are allocated and 300 remain unallocated
- Given a production run with 100 unallocated, when I try to allocate 150 to a channel, then the allocation is rejected with an error message
- Given a production run with existing allocations, when I view it, then I see total allocated, unallocated, and breakdown by channel

### Movements

- Given an allocation of 100 units to Bandcamp, when the allocation is created, then an Allocation movement is recorded with +100 delta
- Given Bandcamp channel has 50 units on hand, when I record a sale of 10, then a Sale movement is recorded with -10 delta
- Given Warehouse has 100 units and Bandcamp has 20, when I transfer 30 from Warehouse to Bandcamp, then Warehouse has 70 and Bandcamp has 50

### Stock Balance

- Given movements: +100 Allocation, -30 Sale, -20 TransferOut, when I view stock balance, then it shows 50 on hand
- Given no movements for a production run/channel combination, when I view stock balance, then it shows 0 on hand

### Validation

- Given a channel has 10 units on hand, when I try to record a sale of 15, then the movement is rejected (if negative stock prevention is enabled)
- Given a channel has 0 units, when I try to transfer out, then the transfer is rejected

## Domain Model

### Entities

| Entity | Description | Key Fields |
|--------|-------------|------------|
| ChannelAllocation | Stock assigned from production run to channel | productionRunId, salesChannelId, quantity, allocatedAt |
| InventoryMovement | Ledger entry for any stock change | productionRunId, salesChannelId, quantityDelta, movementType, occurredAt, referenceId |
| StockBalance | Computed view of current stock | productionRunId, salesChannelId, quantityOnHand |

### Movement Types

| Type | Delta | Description |
|------|-------|-------------|
| ALLOCATION | +N | Initial stock placement from production run |
| SALE | -N | Units sold through channel |
| TRANSFER_OUT | -N | Units leaving channel for transfer |
| TRANSFER_IN | +N | Units arriving from transfer |
| RETURN | +N | Units returned (e.g., unsold concert merch) |
| ADJUSTMENT | +/-N | Corrections, damage, loss, found stock |

### Domain Rules

- Inventory is tracked per ProductionRun + SalesChannel combination
- Stock balances are derived from summing movements, not stored directly
- Total allocations for a production run cannot exceed manufactured quantity
- Sales and transfers cannot exceed channel stock (unless negative stock is allowed)
- Warehouse is modeled as a normal SalesChannel (type: WAREHOUSE)

## Technical Decisions

### Architecture

- Follows existing inventory bounded context structure (domain records, JPA entities, CRUD handlers)
- StockBalance is a read model computed from InventoryMovement, not a stored entity
- InventoryMovement is append-only (movements are never deleted or modified)

### Database

- New tables: `channel_allocation`, `inventory_movement`
- StockBalance computed via SQL aggregation query (SUM of quantityDelta grouped by productionRun + channel)
- Indexes on productionRunId, salesChannelId for efficient balance queries

### API Design

```
# Allocations
POST   /labels/{labelId}/production-runs/{runId}/allocations
GET    /labels/{labelId}/production-runs/{runId}/allocations
DELETE /labels/{labelId}/production-runs/{runId}/allocations/{allocationId}

# Movements (primarily system-generated, but manual adjustments allowed)
POST   /labels/{labelId}/channels/{channelId}/movements
GET    /labels/{labelId}/channels/{channelId}/movements

# Transfers
POST   /labels/{labelId}/transfers

# Stock Balances (read-only)
GET    /labels/{labelId}/stock-balances
GET    /labels/{labelId}/production-runs/{runId}/stock-balances
GET    /labels/{labelId}/channels/{channelId}/stock-balances
```

### Existing Dependencies

- Uses `ProductionRun` from inventory bounded context (already implemented)
- Uses `SalesChannel` from inventory bounded context (already implemented)
- References `Release` from catalog bounded context via productionRun.releaseId

## Out of Scope

- **Pricing and revenue**: Belongs to a future Sales bounded context
- **Accounting and financial reporting**: Belongs to finance bounded context
- **Edition/Variant model**: Format variants (e.g., "Black vinyl", "Clear vinyl") will be added between Release and ProductionRun in a future iteration - the current model is designed to accommodate this extension
- **Digital inventory**: Digital releases have unlimited stock and do not need tracking
- **Automatic reorder alerts**: Future enhancement
- **Batch/bulk import of movements**: Future enhancement
- **Integration with external sales platforms**: Future enhancement (Bandcamp API, etc.)
- **Historical stock reports**: Future enhancement

## Implementation Slices

Suggested order for incremental delivery:

1. **ChannelAllocation domain + persistence** - Create allocation records linking production runs to channels
2. **InventoryMovement domain + persistence** - Append-only ledger for stock changes
3. **Allocation workflow** - UI and API for allocating stock, with movement recording
4. **StockBalance read model** - Query service for computing current balances
5. **Stock balance UI** - Display balances on release/label views
6. **Transfer workflow** - UI and API for moving stock between channels
7. **Adjustment workflow** - Manual stock corrections
8. **Sale recording** - Manual sale entry (external integration deferred)
