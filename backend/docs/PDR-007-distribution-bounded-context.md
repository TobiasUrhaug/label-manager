# PDR-007: Create Distribution Bounded Context with Separate Channel Types

## Status

Accepted

## Context

The inventory bounded context currently contains two distinct concepts that are scattered across packages:

1. **ProductionRun**: Manufactured units of a given format for a release (e.g., 500 vinyl copies)
2. **SalesChannel**: Distribution partners through which products are sold

Initial analysis suggested SalesChannel might be an inventory concern (a destination for stock allocation). However, deeper domain exploration revealed that "sales channels" encompass fundamentally different business models:

- **Distributors/Retailers**: Physical partners who receive inventory on consignment. The label invoices them periodically for what they've sold. They add ~30% commission on top of the label's price.
- **Platforms** (Bandcamp, iTunes, Spotify): Digital marketplaces that sell on the label's behalf. They pay the label after taking their commission (~15%). No physical inventory is held.
- **Direct Sales**: Events and concerts where the label sells directly to customers through a merch table.

These business models differ in:
- **Payment flow**: Label invoices distributor vs platform pays label
- **Inventory model**: Consignment vs no inventory vs direct fulfillment
- **Commission structure**: Markup vs revenue share vs none
- **Product types**: Physical only vs digital vs both

The domain expert naturally uses **different terms** for these concepts in business conversations, not a unified "sales channel" term.

## Decision

Create a new **distribution** bounded context, separate from inventory, with distinct modules for each channel type:

```
org.omt.labelmanager/
├── inventory/              # Physical stock tracking
│   ├── productionrun/      # Module: manufactured units
│   └── allocation/         # Module: allocating stock to distributors
│
├── distribution/           # NEW: Commercial distribution partners
│   ├── distributor/        # Module: physical consignment partners
│   ├── platform/           # Module: digital marketplaces (future)
│   └── directsales/        # Module: events/merch table (future)
│
├── catalog/               # Labels, releases, artists
├── identity/              # Users, authentication
├── finance/               # Costs, invoicing
└── infrastructure/        # Cross-cutting concerns
```

### Module Responsibilities

**inventory/productionrun/**
- Track manufactured batches of physical products
- Record quantity, format, manufacturer, manufacturing date
- Source of truth for available stock

**inventory/allocation/**
- Allocate production run inventory to distributors
- Track how many units are allocated where
- References distributor by ID only (loose coupling)

**distribution/distributor/**
- Manage physical distribution partners (retailers, warehouses)
- Store business relationship data (commission terms, contact info)
- Will support invoicing for consignment sales
- Types: RETAIL, WAREHOUSE, EVENT (deprecated - moves to directsales)

**distribution/platform/** (future)
- Manage digital marketplace partners (Bandcamp, iTunes, Spotify)
- Store commission rates, API credentials, settlement terms
- Import sales statements from platforms
- Types: MARKETPLACE, STREAMING

**distribution/directsales/** (future)
- Manage direct sales at events and concerts
- Track inventory taken to events
- Record direct sales transactions

## Rationale

### Why separate bounded contexts?

**Different concerns:**
- Inventory: WHERE physical products are, HOW MANY remain
- Distribution: WHO you sell through, commercial relationships, invoicing

**Different lifecycles:**
- ProductionRuns are created when manufacturing completes
- Distributors exist independently as business partners

**Future extensibility:**
- Distributors will gain invoicing, commission calculations, sales reporting
- These are commercial/financial concerns, not inventory tracking

### Why separate modules per channel type?

**Matches ubiquitous language:**
- Domain expert uses "distributor", "Bandcamp", "event" - not generic "sales channel"
- Code should mirror the language of the business

**Different operational models:**
- Each type has unique fields: Distributor has markup%, Platform has API credentials
- Different business logic: invoice distributor vs parse platform statement
- Avoids god object anti-pattern

**Clear boundaries:**
- Easy to see which operations apply where
- Can't accidentally "invoice" a streaming service

**Type safety:**
- Compile-time guarantees that you're working with the right concept
- No runtime checking of "channelType" discriminator

## Consequences

### Positive

- ✅ Code aligns with domain language and business reality
- ✅ Each channel type can evolve independently with type-specific behavior
- ✅ Clear separation between inventory tracking and commercial relationships
- ✅ Easier to onboard new developers ("distributors are the ones we invoice")
- ✅ Inventory allocation remains focused on physical stock management

### Negative

- ❌ Cannot easily query "all channels" with a single repository call (need to query each type)
- ❌ Slightly more modules to navigate (but reflects genuine domain complexity)
- ❌ Existing code must be refactored to new structure

### Migration Path

**Phase 1** (Current): Refactor existing SalesChannel
1. Restructure inventory to modular architecture (productionrun module)
2. Create distribution bounded context
3. Move SalesChannel → Distributor in distribution/distributor module
4. Update allocation to reference Distributor by ID
5. Update ChannelType enum to focus on distributor subtypes

**Phase 2** (Future): Add digital channels
- Create distribution/platform module for Bandcamp, iTunes, Spotify
- Create distribution/directsales module for event sales
- Migrate WAREHOUSE, EVENT from Distributor to appropriate new modules

**Phase 3** (Future): Add invoicing
- Create finance/invoice module
- Integrate with distribution/distributor for consignment invoicing
- Integrate with distribution/platform for statement import

### Alternatives Considered

**Option B: Unified SalesChannel with strategy pattern**
- Keep single SalesChannel entity with ChannelType discriminator
- Use strategy pattern for type-specific behavior

Rejected because:
- Doesn't match domain expert's natural language
- Forces shared abstraction where business models fundamentally differ
- Type-specific fields require complex entity inheritance or nullable fields
- Harder to understand which operations apply to which type

## References

- PDR-001: Bounded Contexts
- CLAUDE.md: Modular Architecture Pattern
- Discussion: User clarified that distributors work on consignment (label invoices them), while platforms like Bandcamp pay the label directly after sales
