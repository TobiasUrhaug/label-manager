# Requirements: label-distributor-allocation

## Status
In Progress

## Summary
Remove the standalone `allocation` module and replace it by modelling distributor allocation as an inventory movement, unifying all stock changes (allocation, sale, return) under the existing `inventory_movement` table.

---

## Business Context

When a label creates a production run (e.g. 500 vinyl records of ReleaseA), they distribute units across channels:

| Channel type         | Example          | Physical location      | Tracked separately? |
|----------------------|------------------|------------------------|---------------------|
| External distributor | EuropeDistro     | At distributor         | Yes — per distributor |
| External distributor | USDistro         | At distributor         | Yes — per distributor |
| Warehouse (label stock) | Label own stock | At warehouse          | No — implied by non-allocation |
| Platform reservation | Bandcamp         | At warehouse (stays)  | Yes — logical reservation |

Key facts:
- Allocating to an external distributor = physically shipping records = inventory movement (warehouse → distributor).
- Label own stock = warehouse. Unallocated units are implicitly in the warehouse. No explicit tracking needed.
- Bandcamp = a logical reservation. Units stay physically in the warehouse but a quantity is earmarked for Bandcamp, reducing freely-available warehouse stock.
- Allocations can be incremental (multiple batches from the same production run).
- Returns always go via warehouse (distributor → warehouse), never distributor-to-distributor.

---

## What Changes

### Removed
- `channel_allocation` database table
- Entire `inventory/allocation` package: entity, repository, `AllocationCommandApi`, `AllocationQueryApi`, `AllocationCommandApiImpl`, `AllocationQueryApiImpl`, `AllocationController`, `AddAllocationForm`, all tests
- Allocation details dropdown on the production run row in the UI
- The business rule requiring a prior allocation before recording a sale (see FR-6)

### Unchanged
- "Allocate" button on production runs — kept, but now writes directly to `inventory_movement`
- `inventory_movement` table and `MovementType.ALLOCATION` — already exists and is correct
- Inventory status section on the release page — kept, simplified (no "Allocated" column; current stock derived from movements)

### Updated
- `ProductionRunQueryApiImpl.validateQuantityIsAvailable()` — rewritten to derive available stock from `ALLOCATION` movements instead of `AllocationQueryApi`
- `ReleaseController` — build inventory view from movements only, remove allocation queries
- `AgreementController` — find runs a distributor has been allocated to via `ALLOCATION` movements instead of `AllocationQueryApi`
- `SaleLineItemProcessor` — remove the prior-allocation validation (see FR-6)
- UI allocate modal — simplified; posts directly to inventory movement endpoint

---

## Functional Requirements

### FR-1: Allocate units to an external distributor
The system shall allow a label manager to allocate a quantity of units from a production run to an external distributor by creating an inventory movement of type `ALLOCATION` (from warehouse to distributor). No separate allocation record is created.

Multiple allocations to the same distributor from the same production run are permitted.

**Acceptance criteria:**

- **AC-1.1 — Successful allocation**
  - Given: Production run of 500 units; 300 units currently available in warehouse
  - When: Label manager allocates 200 units to EuropeDistro
  - Then:
    - An `inventory_movement` row is created: type `ALLOCATION`, 200 units, from `WAREHOUSE` to `DISTRIBUTOR` (EuropeDistro)
    - No `channel_allocation` row is created
    - EuropeDistro's current stock = 200 units (derived from movements)
    - Warehouse available stock = 100 units (derived from movements)

- **AC-1.2 — Incremental allocation to same distributor**
  - Given: EuropeDistro already has 200 units; warehouse has 100 units available
  - When: Label manager allocates 50 more units to EuropeDistro
  - Then:
    - A second `ALLOCATION` movement is recorded: 50 units, warehouse → EuropeDistro
    - EuropeDistro total stock = 250 units
    - Warehouse available stock = 50 units

- **AC-1.3 — Allocation to multiple distributors**
  - Given: Production run of 500 units; all in warehouse
  - When: Label manager allocates 200 to EuropeDistro, then 150 to USDistro
  - Then:
    - EuropeDistro stock = 200 units
    - USDistro stock = 150 units
    - Warehouse available stock = 150 units

### FR-2: Prevent over-allocation
The system shall reject an allocation if the requested quantity exceeds the currently available warehouse stock.

Available warehouse stock = `production_run.quantity − sum(ALLOCATION movements) + sum(RETURN movements)`

**Acceptance criteria:**

- **AC-2.1 — Allocation exceeds available stock**
  - Given: Production run of 500 units; 100 units available in warehouse
  - When: Label manager attempts to allocate 150 units to USDistro
  - Then:
    - System rejects with error: "Insufficient warehouse stock. 100 units available, 150 requested."
    - No inventory movement is created

### FR-3: Reserve units for Bandcamp
The system shall allow a label manager to reserve a quantity of units from a production run for Bandcamp by creating an inventory movement of type `ALLOCATION` from `WAREHOUSE` to `BANDCAMP`. Units physically remain in the warehouse. The reservation reduces freely available warehouse stock.

**Acceptance criteria:**

- **AC-3.1 — Successful Bandcamp reservation**
  - Given: 300 units freely available in warehouse
  - When: Label manager reserves 50 units for Bandcamp
  - Then:
    - An `inventory_movement` row is created: type `ALLOCATION`, 50 units, from `WAREHOUSE` to `BANDCAMP`
    - Bandcamp available units = 50
    - Freely available warehouse stock = 250 units
    - Units are not physically moved

- **AC-3.2 — Bandcamp reservation reduces allocatable stock**
  - Given: 300 units freely available; 50 reserved for Bandcamp = 250 freely available
  - When: Label manager attempts to allocate 300 units to EuropeDistro
  - Then:
    - System rejects: "Insufficient warehouse stock. 250 units available, 300 requested."

- **AC-3.3 — Bandcamp reservation cannot exceed freely available stock**
  - Given: 100 units freely available in warehouse
  - When: Label manager attempts to reserve 150 units for Bandcamp
  - Then:
    - System rejects with an appropriate error

### FR-3b: Cancel a Bandcamp reservation (partial or full)
The system shall allow a label manager to cancel part or all of an existing Bandcamp reservation by creating an inventory movement of type `RETURN` from `BANDCAMP` to `WAREHOUSE`. The cancellable quantity is capped at units currently held by Bandcamp (allocated minus sold).

Cancellable quantity = `sum(ALLOCATION movements to BANDCAMP) − sum(SALE movements from BANDCAMP)`

**Acceptance criteria:**

- **AC-3b.1 — Partial cancellation**
  - Given: 50 units allocated to Bandcamp; 10 units sold from Bandcamp → 40 units currently held
  - When: Label manager cancels 20 units
  - Then:
    - A `RETURN` movement is recorded: 20 units, from `BANDCAMP` to `WAREHOUSE`
    - Bandcamp held units = 20
    - Freely available warehouse stock increases by 20

- **AC-3b.2 — Full cancellation**
  - Given: 40 units currently held by Bandcamp
  - When: Label manager cancels all 40 units
  - Then:
    - A `RETURN` movement is recorded: 40 units, from `BANDCAMP` to `WAREHOUSE`
    - Bandcamp held units = 0

- **AC-3b.3 — Cancellation exceeds held quantity**
  - Given: 40 units currently held by Bandcamp
  - When: Label manager attempts to cancel 50 units
  - Then:
    - System rejects with an appropriate error; no movement is created

### FR-3c: Record Bandcamp sales
The system shall allow a label manager to record sales from Bandcamp stock as an inventory movement of type `SALE` from `BANDCAMP`. Sales are entered manually based on periodic Bandcamp sales reports.

**Acceptance criteria:**

- **AC-3c.1 — Successful Bandcamp sale**
  - Given: Bandcamp holds 40 units
  - When: Label manager records a sale of 15 units from Bandcamp
  - Then:
    - A `SALE` movement is recorded: 15 units, from `BANDCAMP`
    - Bandcamp held units = 25

- **AC-3c.2 — Sale exceeds Bandcamp stock**
  - Given: Bandcamp holds 10 units
  - When: Label manager attempts to record a sale of 20 units from Bandcamp
  - Then:
    - System rejects with insufficient stock error; no movement is created

### FR-4: Return units from distributor to warehouse
The system shall allow a label manager to record a return of units from a distributor back to the warehouse as an inventory movement of type `RETURN`.

**Acceptance criteria:**

- **AC-4.1 — Successful return**
  - Given: EuropeDistro has 200 units (derived from movements)
  - When: Label manager records a return of 50 units from EuropeDistro
  - Then:
    - An `RETURN` movement is recorded: 50 units, from `DISTRIBUTOR` (EuropeDistro) to `WAREHOUSE`
    - EuropeDistro stock = 150 units
    - Warehouse available stock increases by 50 units

- **AC-4.2 — Return exceeds distributor stock**
  - Given: EuropeDistro has 150 units
  - When: Label manager attempts to return 200 units
  - Then:
    - System rejects: "Cannot return 200 units. EuropeDistro currently holds 150 units."
    - No movement is created

- **AC-4.3 — No direct distributor-to-distributor transfers**
  - The system shall not allow transfers between distributors. A return must go to warehouse first.

### FR-5: Inventory visibility per production run
The system shall display a clear breakdown of where units from a production run are located, derived entirely from inventory movements.

**Acceptance criteria:**

- **AC-5.1 — Inventory summary on release page**
  - Given: Production run of 500 units with movements recorded
  - When: Label manager views the release detail page
  - Then: The page shows:
    - Total manufactured: 500 units
    - Warehouse available: `total − sum(ALLOCATION movements) + sum(RETURN movements) − Bandcamp reservations`
    - Per distributor: current stock = `sum(ALLOCATION) − sum(SALE) + sum(RETURN)` for that distributor
    - Bandcamp: held units = `sum(ALLOCATION to BANDCAMP) − sum(SALE from BANDCAMP) + sum(RETURN from BANDCAMP)`

- **AC-5.2 — Movement history**
  - The page shows a chronological movement log with: date, type, from/to, quantity.
  - The allocation details dropdown (previously shown per production run row) is removed. Movement history replaces it.

### FR-6: Sale validation does not require a prior allocation
The system shall not require that a distributor has a prior allocation before a sale can be recorded. Sale validation is based solely on current stock.

**Rationale:** If a distributor has never been allocated any units, their current stock is 0. A sale of any quantity will fail the stock check (FR-7) regardless. The prior-allocation guard is therefore redundant and is removed.

**Acceptance criteria:**

- **AC-6.1 — Sale without prior allocation is validated by stock only**
  - Given: EuropeDistro has never received an allocation for production run X (stock = 0)
  - When: Label manager attempts to record a sale of 10 units from EuropeDistro
  - Then:
    - System rejects with insufficient stock error (not "no allocation exists" error)

### FR-7: Sale quantity cannot exceed distributor's current stock
The system shall validate that a sale quantity does not exceed the distributor's current stock, where current stock is derived from inventory movements.

Current stock = `sum(ALLOCATION movements to distributor) − sum(SALE movements from distributor) + sum(RETURN movements from distributor)`

**Acceptance criteria:**

- **AC-7.1 — Sale within stock**
  - Given: EuropeDistro has 200 units
  - When: Label manager records a sale of 150 units
  - Then: Sale is recorded; EuropeDistro stock = 50 units

- **AC-7.2 — Sale exceeds stock**
  - Given: EuropeDistro has 50 units
  - When: Label manager attempts to record a sale of 100 units
  - Then: System rejects with insufficient stock error

---

## Non-Functional Requirements

- NFR-1: Stock values (warehouse available, per-distributor current) must be consistent at all times. No negative stock permitted.
- NFR-2: All inventory changes must be persisted as movements to provide a complete audit trail.
- NFR-3: The database migration must safely drop the `channel_allocation` table. Existing allocation history is already captured in `inventory_movement` rows (they were written in parallel), so no data migration is needed.

---

## Out of Scope

- Bandcamp sales recording (handled by `sales-recording-distributor` feature).
- Direct distributor-to-distributor transfers.
- Financial tracking of shipment costs.
- Automated import from distributor or Bandcamp systems.

---

## Open Questions

None. All open questions resolved in session 2026-03-21.

### Resolved
- **OQ-1 (resolved):** Bandcamp is modelled as a `LocationType.BANDCAMP` — a `WAREHOUSE → BANDCAMP` allocation movement. No separate entity needed.
- **OQ-2 (resolved):** Partial and full cancellation are supported as `BANDCAMP → WAREHOUSE` return movements. Cap = allocated minus sold.
