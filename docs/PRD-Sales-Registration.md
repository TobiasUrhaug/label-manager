# Product Requirements Document: Sales Registration

**Version**: 1.0
**Date**: 2026-02-12
**Status**: Draft - Ready for Implementation

## Executive Summary

Enable label owners to register sales of their releases through various channels (events, record stores), track revenue, and automatically reduce inventory from their production run allocations.

---

## 1. Ubiquitous Language

Establishing the domain vocabulary:

### Core Concepts

**Sale**
- A recorded instance of selling one or more releases to end customers
- Contains: date, sales channel, notes
- Always associated with one label
- Contains one or more Sale Line Items

**Sale Line Item**
- A single line within a sale representing quantity sold of one release in one format
- Contains: release reference, format (vinyl/CD), quantity, unit price, line total
- Example: "10 × ReleaseA (Vinyl) @ €25 = €250"

**Sales Channel**
- The category of where the sale occurred
- Values: Event, Record Store, (future: Online, etc.)

**Production Run** (existing concept)
- A batch of physical products manufactured
- Example: 200 vinyl records pressed

**Allocation** (existing concept)
- Distribution of production run units to different parties
- Example: 200 vinyls → 100 to DistributorA, 100 to Label inventory
- The label's allocation represents their available inventory

**Distribution** (existing concept, distinct from Sale)
- The act of allocating stock to distributors or consignment to stores
- NOT a sale to end customers
- Tracked separately from sales

**Inventory / Stock**
- The label's available units from their allocation of a production run
- Reduced when sales are registered
- Increased when new allocations are made

---

## 2. Domain Model

### Key Entities

```
Sale
├── id: Long
├── labelId: Long (reference to Label)
├── saleDate: LocalDate
├── channel: SalesChannel (enum: EVENT, RECORD_STORE)
├── notes: String (optional - users can add venue/store names here)
├── totalAmount: BigDecimal (calculated from line items)
└── lineItems: List<SaleLineItem>

SaleLineItem
├── id: Long
├── saleId: Long (reference to Sale)
├── releaseId: Long (reference to Release)
├── format: ReleaseFormat (enum: VINYL, CD, DIGITAL)
├── quantity: Integer
├── unitPrice: BigDecimal (one simple price per line item)
└── lineTotal: BigDecimal (calculated: quantity × unitPrice)
```

### Relationships

- **Sale → Label**: One sale belongs to one label (many-to-one)
- **Sale → SaleLineItem**: One sale has many line items (one-to-many, composition)
- **SaleLineItem → Release**: Each line item references one release (many-to-one)
- **SaleLineItem → Allocation**: Reduces inventory from the label's allocation of the release/format

---

## 3. Bounded Context

This feature introduces a new **`sales`** bounded context:

```
org.omt.labelmanager/
├── catalog/           # Labels, releases, artists (existing)
├── distribution/      # Production runs, allocations (existing)
├── sales/             # NEW: Sales registration
│   ├── api/
│   ├── application/
│   ├── domain/
│   └── infrastructure/
├── identity/          # Users, authentication (existing)
├── finance/           # Costs (existing)
└── infrastructure/    # Cross-cutting (existing)
```

### Integration Points

- **sales → catalog**: Query releases (via `ReleaseQueryApi`)
- **sales → distribution**: Reduce allocation inventory when sale is registered (via `AllocationCommandApi`)
- **sales → identity**: Associate sales with users/labels (via existing security context)

---

## 4. Use Cases

### Primary Use Cases

#### UC1: Register a Sale
**Actor**: Label owner
**Description**: Record a sale of one or more releases through a specific channel

**Flow**:
1. User navigates to "Register Sale" for their label
2. User selects:
   - Sale date
   - Sales channel (Event, Record Store)
   - Notes (optional - can include venue/store name)
3. User adds line items:
   - Select release
   - Select format (Vinyl, CD)
   - Enter quantity sold
   - Enter unit price
4. System calculates line totals and sale total
5. System validates:
   - All releases belong to the label
   - Sufficient inventory exists in label's allocation for each line item
6. User submits sale
7. System:
   - Creates sale record
   - Reduces inventory from label's allocation for each line item
   - Displays confirmation

**Business Rules**:
- Cannot sell more units than available in label's allocation
- All line items must reference releases owned by the same label
- Quantity must be positive
- Unit price must be non-negative (could be 0 for promotional giveaways)
- Sale date cannot be in the future
- Only label owners can register sales

#### UC2: View Sales History
**Actor**: Label owner
**Description**: View all recorded sales for a label

**Flow**:
1. User views label detail page
2. System displays list of sales:
   - Date, channel, notes
   - Total amount
   - Number of line items
3. User clicks on a sale to view details:
   - All line items (release, format, quantity, unit price)
   - Notes
   - Total amount

#### UC3: View Sales on Release Page
**Actor**: Label owner
**Description**: See sales history for a specific release

**Flow**:
1. User views release detail page
2. System displays:
   - Total units sold (by format)
   - Total revenue from sales
   - List of recent sales including this release
   - Current inventory remaining (from allocation)

---

## 5. User Stories

### Story 1: Event Sales Registration
**As a** label owner
**I want to** register sales from a concert/festival
**So that** I can track inventory and revenue from live events

**Acceptance Criteria**:
- Can record sale date and channel (Event)
- Can add event details in notes field
- Can add multiple releases in one sale
- Can specify format (vinyl/CD) and quantity for each release
- Can enter the price I sold each item for
- System reduces my label's inventory by the quantities sold
- Can see the total revenue from the sale

**Example**:
- Channel: Event
- Date: 2026-08-08
- Notes: "Øya Festival 2026"
- Line items:
  - Release "Album X" (Vinyl) × 20 @ €25 = €500
  - Release "Album Y" (CD) × 10 @ €15 = €150
- Total: €650

### Story 2: Record Store Sales
**As a** label owner
**I want to** register sales from consignment to record stores
**So that** I can track which stores are selling my records

**Acceptance Criteria**:
- Can select "Record Store" as channel
- Can specify store name in notes
- Same functionality as event sales for line items

### Story 3: View Release Sales Performance
**As a** label owner
**I want to** see sales data on the release detail page
**So that** I can understand which releases are selling well

**Acceptance Criteria**:
- Release page shows total units sold (by format)
- Release page shows total revenue generated
- Release page shows current inventory remaining
- Can click through to see individual sales

---

## 6. Inventory Management

### How Sales Affect Inventory

When a sale is registered:

1. **System validates inventory**: Check that label's allocation has sufficient quantity
2. **System reduces allocation**: Subtract sold quantity from label's available inventory
3. **System records sale**: Create sale and line item records

### Example Scenario

**Initial State**:
- Production Run: 200 vinyl copies of "Album X"
- Allocation 1: 100 to DistributorA
- Allocation 2: 100 to Label (my inventory)

**After registering sale of 10 vinyls**:
- Allocation 2: 90 remaining (100 - 10)
- Production Run: 200 total (unchanged)
- Sale record created with 1 line item

**After registering sale of 20 more vinyls**:
- Allocation 2: 70 remaining (90 - 20)

**Attempting to sell 80 vinyls**:
- System rejects: Only 70 available in inventory

---

## 7. Out of Scope (V1)

The following are explicitly **not included** in the first version:

### Not Included Now
- ❌ Returns/refunds (sales are final)
- ❌ Editing or deleting sales after creation
- ❌ Sales to distributors (that's Distribution, not Sale)
- ❌ Digital download sales tracking
- ❌ Online store integration
- ❌ Consignment tracking (what's in stores vs sold)
- ❌ Payment tracking (paid vs unpaid)
- ❌ Sales forecasting or analytics
- ❌ Multi-currency support
- ❌ Tax/VAT calculation on sales
- ❌ Connecting sales to existing Finance/Costs features
- ❌ Location/venue as separate field (use notes instead)
- ❌ Bulk discounts or variable pricing (one price per line item)
- ❌ Permissions for label members (only owners for now)

### Could Be Added Later
- ✅ Edit/delete sales (with inventory adjustment)
- ✅ Returns (negative sales or separate entity)
- ✅ Sales analytics dashboard
- ✅ Export sales data (CSV, PDF)
- ✅ Integration with accounting/finance module
- ✅ Track consignment inventory separately
- ✅ Customer information (who bought what)
- ✅ Venue/store entity with predefined locations
- ✅ Pricing variations (discounts, bulk pricing)
- ✅ Granular permissions (members can register sales)

---

## 8. Technical Considerations

### Database Schema (Preliminary)

```sql
CREATE TABLE sales (
    id BIGSERIAL PRIMARY KEY,
    label_id BIGINT NOT NULL REFERENCES labels(id),
    sale_date DATE NOT NULL,
    channel VARCHAR(50) NOT NULL,
    notes TEXT,
    total_amount DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE sale_line_items (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    release_id BIGINT NOT NULL REFERENCES releases(id),
    format VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10, 2) NOT NULL CHECK (unit_price >= 0),
    line_total DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sales_label_id ON sales(label_id);
CREATE INDEX idx_sales_sale_date ON sales(sale_date DESC);
CREATE INDEX idx_sale_line_items_sale_id ON sale_line_items(sale_id);
CREATE INDEX idx_sale_line_items_release_id ON sale_line_items(release_id);
```

### Module Structure

Following the modular architecture pattern:

```
sales/
├── api/
│   ├── SaleCommandApi.java
│   ├── SaleQueryApi.java
│   ├── SaleController.java
│   └── RegisterSaleForm.java
├── application/
│   ├── RegisterSaleUseCase.java
│   ├── GetSalesForLabelUseCase.java
│   ├── GetSalesForReleaseUseCase.java
│   ├── SaleCommandApiImpl.java
│   └── SaleQueryApiImpl.java
├── domain/
│   ├── Sale.java
│   ├── SaleLineItem.java
│   └── SalesChannel.java (enum)
└── infrastructure/
    ├── SaleEntity.java
    ├── SaleLineItemEntity.java
    └── SaleRepository.java
```

### Integration with Distribution

The `RegisterSaleUseCase` will need to call `AllocationCommandApi` to reduce inventory:

```java
@Service
class RegisterSaleUseCase {
    private final SaleRepository saleRepository;
    private final AllocationCommandApi allocationApi;
    private final ReleaseQueryApi releaseQuery;

    public Sale execute(RegisterSaleCommand command) {
        // Validate releases exist and belong to label
        validateReleases(command);

        // Validate and reduce inventory for each line item
        for (var item : command.lineItems()) {
            allocationApi.reduceLabelInventory(
                command.labelId(),
                item.releaseId(),
                item.format(),
                item.quantity()
            );
        }

        // Create and persist sale
        return createSale(command);
    }
}
```

---

## 9. Success Criteria

This feature is successful when:

1. ✅ Label owners can register sales from events and stores
2. ✅ Sales correctly reduce inventory from label allocations
3. ✅ Sales history is visible on label and release pages
4. ✅ Revenue totals are accurately calculated
5. ✅ System prevents overselling (cannot sell more than inventory)
6. ✅ All tests pass (unit, integration, system, E2E)

---

## 10. Decisions

### 1. Distribution Module
**Decision**: Integrate with existing `distribution` bounded context via `AllocationCommandApi`

### 2. Pricing Model
**Decision**: Simple one price per line item. No bulk discounts or price variations in V1.

### 3. Location Field
**Decision**: No separate location field. Users add venue/store names in the notes field.

### 4. Permissions
**Decision**: Only label owners can register sales (no concept of members yet).

---

## Next Steps

1. ✅ PRD complete and reviewed
2. ⬜ Create ADR (Architecture Decision Record) for sales bounded context
3. ⬜ Break down into TDD implementation slices
4. ⬜ Start implementation (domain model first)
5. ⬜ Create Flyway migrations
6. ⬜ Implement use cases with integration tests
7. ⬜ Add controllers with controller tests
8. ⬜ Update UI templates
9. ⬜ Add E2E tests
