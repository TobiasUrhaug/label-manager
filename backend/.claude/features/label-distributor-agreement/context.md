# Context: Label-Distributor Pricing Agreement

## Status
Draft

## Background
When a label works with a physical distributor, they negotiate a commercial agreement before any inventory is allocated. This agreement specifies:
- The **label's unit price** for each release+format (what the label expects to be paid per unit sold)
- The **distributor's commission percentage** on top of that price (the distributor's margin when selling to stores)

Example: Label A and Distributor B agree that "Album X on vinyl" is priced at 10€/unit, with the distributor taking 30% commission on top (so they sell to stores at 13€ and owe the label 10€ per unit sold).

Currently, the app has no way to record this agreement. Sales are recorded manually from distributor statements, but there is no structured pricing data to validate or derive what the label is owed. This makes it impossible (without manual calculation) to determine outstanding amounts for future invoicing.

## User Story
As a label manager, I want to record the pricing agreement with each distributor per release+format, so that I have a clear, auditable record of what the label is owed per unit sold and can eventually generate invoices automatically.

## Dependencies
- **Distributor module** (`distribution/distributor`): Agreements are linked to an existing Distributor entity.
- **Inventory / Production Run module** (`inventory`): Agreements reference a specific release+format (production run or release+format pair).
- **Sales recording feature** (`sales-recording-distributor`): Existing sales records will eventually be joined with pricing agreements to compute outstanding amounts for invoicing. The sales recording UI does NOT auto-populate prices from the agreement — price entry stays manual.

## Constraints
- Single currency only (consistent with existing app constraint).
- An agreement is set once per distributor+release+format and does not change over time (no versioning or renegotiation support in this iteration).
- The sales recording workflow (existing feature) is not modified by this feature.
- Invoice generation is explicitly out of scope — but the data model must not preclude it.

## Prior Art
- `sales-recording-distributor` feature: established the Sale, Return, and InventoryMovement entities. Pricing agreement is a companion to this feature, not a replacement.
- `finance/` bounded context: existing Costs entity handles VAT. Pricing agreements do not involve VAT (distributor invoicing is out of scope).
- `distribution/distributor` module: Distributor entity currently holds `id`, `labelId`, `name`, `channelType`. Pricing agreements will be a new entity linked to Distributor.
