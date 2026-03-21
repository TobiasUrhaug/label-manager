# Requirements: Label-Distributor Pricing Agreement

## Status
Draft

## Summary
Allow a label manager to record and manage the pricing agreement between a label and a distributor for each release+format: the label's unit price and the distributor's commission percentage.

---

## Functional Requirements

### Core Entity: Pricing Agreement

**FR-1:** The system shall allow a label manager to create a pricing agreement between a distributor and a specific release+format.

**FR-2:** A pricing agreement shall record:
- Distributor (required)
- Release+format (required) — identifies which production run the agreement applies to
- Label unit price (required) — the amount the distributor owes the label per unit sold, in the label's currency
- Distributor commission percentage (required) — the distributor's margin on top of the unit price when selling to stores

**FR-3:** The system shall enforce that only one pricing agreement exists per distributor+release+format combination. Attempting to create a duplicate shall be rejected with a clear error message.

**FR-4:** The system shall allow a label manager to view all pricing agreements for a given distributor, showing: release+format, unit price, commission percentage.

**FR-5:** The system shall allow a label manager to view all pricing agreements for a given release, showing: distributor, unit price, commission percentage.

**FR-6:** The system shall allow a label manager to edit an existing pricing agreement (unit price and/or commission percentage).

**FR-7:** The system shall allow a label manager to delete a pricing agreement.

### Agreement Management Screen

**FR-8:** The system shall provide a dedicated screen for managing pricing agreements, accessible from the distributor's detail page.

**FR-9:** From the agreement management screen, a label manager shall be able to:
- View all agreements for that distributor
- Create a new agreement
- Edit an existing agreement
- Delete an existing agreement

### Validation

**FR-10:** The system shall validate that:
- Label unit price is a positive monetary value (greater than zero)
- Commission percentage is a non-negative number between 0 and 100 (inclusive)
- Both distributor and release+format are selected before saving

**FR-11:** Given a validation failure when saving an agreement, the system shall display a clear error message and not persist the record.

### Future Invoicing Support

**FR-12:** The system shall store pricing agreements in a way that allows future computation of: `label unit price × units sold` for any set of recorded sales, per distributor per period. (This computation is not exposed in the UI in this iteration but must be structurally possible.)

---

## Non-Functional Requirements

**NFR-1: Data integrity.** A pricing agreement must always reference a valid distributor and a valid release+format. Deleting a distributor or production run that has associated pricing agreements shall be blocked, or agreements shall be deleted in cascade — this decision is for the Architect to specify.

**NFR-2: Consistency with existing UI patterns.** The agreement screen shall follow the same Bootstrap 5 / Thymeleaf patterns used in the rest of the application (distributor detail pages, allocation screens).

**NFR-3: Single currency.** Pricing agreements use the same single-currency constraint as the rest of the app. No currency selection is required.

**NFR-4: Access control.** No role-based restrictions — consistent with the existing application. Any authenticated user can create, edit, and delete pricing agreements.

**NFR-5: Auditability.** Pricing agreements shall be persisted with a creation timestamp to support future audit trails.

---

## User Stories

### US-1: Record a pricing agreement with a distributor

**As a** label manager
**I want to** record the agreed unit price and commission for a specific release+format with a distributor
**So that** I have a clear record of the commercial terms and can later derive what the distributor owes me

**Acceptance Criteria:**
- [ ] I can navigate to a distributor's detail page and open the pricing agreements screen
- [ ] I can select a release+format from a dropdown (showing only releases allocated to this distributor)
- [ ] I can enter a label unit price (positive decimal number)
- [ ] I can enter a commission percentage (0–100)
- [ ] On save, the agreement is persisted and shown in the agreements list
- [ ] If I try to add a second agreement for the same release+format, the system rejects it with an error

**Example Scenario:**
- Distributor: Big Music Distribution
- Release+format: Album A – Vinyl
- Label unit price: 10.00 €
- Commission: 30%
- Result: Agreement saved. Big Music Distribution's agreements list shows: Album A Vinyl | 10.00 € | 30%

### US-2: View agreements for a distributor

**As a** label manager
**I want to** see all pricing agreements for a distributor on one screen
**So that** I can quickly review the commercial terms for all releases distributed by them

**Acceptance Criteria:**
- [ ] Distributor detail page links to (or embeds) a list of all pricing agreements for that distributor
- [ ] Each row shows: release+format, label unit price, commission percentage
- [ ] I can click into an agreement to edit or delete it

### US-3: Edit a pricing agreement

**As a** label manager
**I want to** update an existing pricing agreement
**So that** I can correct data entry errors

**Acceptance Criteria:**
- [ ] I can edit the unit price and/or commission percentage of an existing agreement
- [ ] Validation rules apply on save
- [ ] The updated values are immediately reflected in the agreements list

### US-4: Delete a pricing agreement

**As a** label manager
**I want to** delete a pricing agreement that is no longer relevant
**So that** the agreements list stays clean and accurate

**Acceptance Criteria:**
- [ ] I can delete a pricing agreement from the agreements list or detail view
- [ ] The system asks for confirmation before deleting
- [ ] The agreement is removed and no longer appears in any list

---

## Business Rules

### Rule: One agreement per distributor+release+format
**When:** A label manager attempts to save a new pricing agreement
**Then:** The system checks that no agreement already exists for the same distributor and release+format
**Example:** Big Music Distribution already has an agreement for "Album A – Vinyl". Trying to add another is blocked.

### Rule: Unit price must be positive
**When:** A label manager saves or edits an agreement
**Then:** The label unit price must be greater than zero
**Example:** Entering 0.00 or -5.00 is rejected.

### Rule: Commission percentage must be 0–100
**When:** A label manager saves or edits an agreement
**Then:** The commission percentage must be a number between 0 and 100 inclusive
**Example:** Entering 150 is rejected; entering 0 is valid (a distributor taking no commission).

---

## Out of Scope

- **Invoice generation:** Creating, formatting, or sending invoices to distributors is a future feature. This feature only stores the pricing data.
- **Auto-populating the sales form:** The existing sales recording screen is not modified. Entering unit prices in a sale remains manual.
- **Agreement versioning / renegotiation:** If terms change, the user edits the existing agreement. Historical sales are not re-priced.
- **VAT on distributor invoices:** Not applicable in this feature.
- **Multi-currency:** Single currency only, consistent with the rest of the app.
- **Role-based access control:** All authenticated users have full access, consistent with the rest of the app.

---

## Open Questions for Architect

- [ ] Should deleting a distributor or production run cascade-delete associated pricing agreements, or should deletion be blocked when agreements exist?
- [ ] Should the release+format dropdown in the agreement creation form be limited to releases already allocated to this distributor, or all releases for the label?
- [ ] Where does the pricing agreement entity live in the bounded context structure — under `distribution/` or a new `agreement/` context?

---

## Appendix A: Fixed-Amount Commission

### Discovery
Some distributors negotiate a fixed per-unit amount as their commission rather than a percentage. This requirement was identified after the initial specification was written.

### Changes to Core Entity (addendum to FR-2)

A pricing agreement's commission is **either** a percentage **or** a fixed amount per unit — never both.

The commission field in FR-2 is therefore replaced by two mutually exclusive fields:
- **Commission type** (required) — `PERCENTAGE` or `FIXED_AMOUNT`
- **Commission value** (required) — interpreted as a percentage (0–100) if type is `PERCENTAGE`, or as a positive monetary amount if type is `FIXED_AMOUNT`

All other fields (distributor, release+format, label unit price) remain unchanged.

**Example:**
- Label unit price: 4.00 €, commission type: `FIXED_AMOUNT`, commission value: 1.00 €
- The distributor keeps 1.00 € per unit sold and sells to stores at 5.00 €. The label is owed 4.00 € per unit sold.

### New/Updated Functional Requirements

**FR-2a:** A pricing agreement shall record:
- Distributor (required)
- Release+format (required)
- Label unit price (required) — amount the label is owed per unit sold
- Commission type (required) — `PERCENTAGE` or `FIXED_AMOUNT`
- Commission value (required) — percentage (0–100) or fixed monetary amount (> 0) depending on type

**FR-10a:** The system shall validate that:
- If commission type is `PERCENTAGE`: value is a number between 0 and 100 inclusive
- If commission type is `FIXED_AMOUNT`: value is a positive monetary amount (greater than zero)

### Display

The commission column in all agreements lists shall display the value with its unit:
- Percentage: e.g., `30%`
- Fixed amount: e.g., `1.00 €`

### Updated Business Rules

**Rule: Commission value must match its type**
- If type is `PERCENTAGE`: value must be 0–100
- If type is `FIXED_AMOUNT`: value must be greater than zero
- A single agreement cannot carry both a percentage and a fixed amount

### Impact on Future Invoicing (addendum to FR-12)

The invoicing computation remains `label unit price × units sold` regardless of commission type. The commission type and value are recorded for reference and transparency but do not change what the label is owed — that is always the label unit price.
