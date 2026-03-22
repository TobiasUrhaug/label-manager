# Spec: Label-Distributor Pricing Agreement

## Status
In Progress

## Amendment: Fixed-Amount Commission (Appendix A)

See requirements Appendix A. The `commission_percentage` field is replaced by two fields: `commission_type` (enum) and `commission_value` (decimal). All other design decisions are unchanged.

## Approach

Add a `distribution/agreement` module within the `distribution` bounded context. The module stores a `PricingAgreement` entity — one per distributor+production run pair — recording the label's unit price and the distributor's commission percentage.

A dedicated agreements screen is accessible from the distributor detail page. The create form shows only production runs already allocated to the distributor and not yet covered by an agreement. Full CRUD is exposed via a Spring MVC controller following existing patterns (Bootstrap 5 / Thymeleaf, POST-redirect-GET).

---

## Key Decisions

- **Module location:** `distribution/agreement/` (within the `distribution` bounded context)
  - Why: Pricing terms are a distribution concern. They sit alongside the distributor module. A new top-level bounded context would be over-engineering for this data size.
  - Alternative: A new `agreement/` bounded context — rejected; unnecessary at this scale.

- **Deletion (NFR-1):** ON DELETE CASCADE at the DB level on both foreign keys (`distributor_id`, `production_run_id`)
  - Why: Avoids adding cross-module awareness to the distributor and production run deletion paths. Application code does not need to check for agreements before deleting either entity.
  - Alternative: Block deletion when agreements exist — rejected; requires DeleteDistributorUseCase to call AgreementQueryApi, introducing coupling from `distribution/distributor` → `distribution/agreement`.

- **Release+format dropdown:** Filtered to production runs allocated to this distributor **and** not yet having a pricing agreement with them.
  - Why: Matches US-1 AC ("showing only releases allocated to this distributor"). Agreements for unallocated runs are meaningless; already-covered runs would violate the uniqueness constraint.
  - Requires adding `getAllocationsForDistributor(Long distributorId)` to `AllocationQueryApi`.

- **AllocationQueryApi extension:** Add `getAllocationsForDistributor(Long distributorId)` to expose distributor-specific allocations.
  - Why: Needed to build the filtered dropdown. Extending an existing public API is the correct pattern — no repository cross-injection.

- **Unique constraint:** Enforced at the DB level (`UNIQUE(distributor_id, production_run_id)`). The use case also checks before insert and throws `DuplicateAgreementException` (in `api/`) to produce a user-facing error.

- **Validation location:** Business rule validation (unit price > 0, commission 0–100) lives in `CreateAgreementUseCase` / `UpdateAgreementUseCase`. The controller catches thrown exceptions and re-renders the form with an error message.

---

## Data Models

### DB table — `pricing_agreement` (V29 migration)

```sql
CREATE TABLE pricing_agreement (
    id                    BIGSERIAL PRIMARY KEY,
    distributor_id        BIGINT NOT NULL REFERENCES distributor(id) ON DELETE CASCADE,
    production_run_id     BIGINT NOT NULL REFERENCES production_run(id) ON DELETE CASCADE,
    unit_price            NUMERIC(10, 2) NOT NULL,
    commission_percentage NUMERIC(5, 2) NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_pricing_agreement UNIQUE (distributor_id, production_run_id)
);
```

### Domain record — `PricingAgreement.java`

```java
public record PricingAgreement(
    Long id,
    Long distributorId,
    Long productionRunId,
    BigDecimal unitPrice,
    BigDecimal commissionPercentage,
    Instant createdAt
) {
    static PricingAgreement fromEntity(PricingAgreementEntity entity) { ... }
}
```

### JPA entity — `PricingAgreementEntity.java`

Fields: `id`, `distributorId`, `productionRunId`, `unitPrice` (NUMERIC 10,2), `commissionPercentage` (NUMERIC 5,2), `createdAt` (Instant, defaulted on insert).

### API interfaces

```java
// api/AgreementCommandApi.java
public interface AgreementCommandApi {
    PricingAgreement create(Long distributorId, Long productionRunId,
                            BigDecimal unitPrice, BigDecimal commissionPercentage);
    PricingAgreement update(Long agreementId,
                            BigDecimal unitPrice, BigDecimal commissionPercentage);
    void delete(Long agreementId);
}

// api/AgreementQueryApi.java
public interface AgreementQueryApi {
    Optional<PricingAgreement> findById(Long id);
    List<PricingAgreement> findByDistributorId(Long distributorId);
    boolean existsByDistributorIdAndProductionRunId(Long distributorId, Long productionRunId);
}
```

### Form binding — `AgreementForm.java`

```java
public class AgreementForm {
    private Long productionRunId;   // for create only
    private BigDecimal unitPrice;
    private BigDecimal commissionPercentage;
}
```

### Exceptions (in `api/`)

```java
// api/DuplicateAgreementException.java
public class DuplicateAgreementException extends RuntimeException { ... }

// api/AgreementNotFoundException.java
public class AgreementNotFoundException extends RuntimeException { ... }
```

### AllocationQueryApi addition

```java
// Add to inventory/allocation/api/AllocationQueryApi.java
List<ChannelAllocation> getAllocationsForDistributor(Long distributorId);
```

### View record (package-private in `api/`)

```java
// api/AvailableProductionRunView.java (package-private)
record AvailableProductionRunView(Long productionRunId, String displayName) {}
// displayName = "<release title> – <format>", assembled in the controller
```

---

## URL Routes

| Method | URL | Action |
|--------|-----|--------|
| GET  | `/labels/{labelId}/distributors/{distributorId}/agreements`           | List all agreements for distributor |
| GET  | `/labels/{labelId}/distributors/{distributorId}/agreements/new`       | Show create form |
| POST | `/labels/{labelId}/distributors/{distributorId}/agreements`           | Submit create |
| GET  | `/labels/{labelId}/distributors/{distributorId}/agreements/{id}/edit` | Show edit form |
| POST | `/labels/{labelId}/distributors/{distributorId}/agreements/{id}`      | Submit edit (PUT override) |
| POST | `/labels/{labelId}/distributors/{distributorId}/agreements/{id}/delete` | Delete |

---

## Files to Create or Modify

### New files

| File | Purpose |
|------|---------|
| `src/main/resources/db/migration/V29__create_pricing_agreement_table.sql` | DB schema |
| `src/main/java/.../distribution/agreement/domain/PricingAgreement.java` | Domain record |
| `src/main/java/.../distribution/agreement/infrastructure/PricingAgreementEntity.java` | JPA entity |
| `src/main/java/.../distribution/agreement/infrastructure/PricingAgreementRepository.java` | Spring Data repo |
| `src/main/java/.../distribution/agreement/api/AgreementCommandApi.java` | Public command interface |
| `src/main/java/.../distribution/agreement/api/AgreementQueryApi.java` | Public query interface |
| `src/main/java/.../distribution/agreement/api/AgreementForm.java` | Form binding |
| `src/main/java/.../distribution/agreement/api/DuplicateAgreementException.java` | Domain exception |
| `src/main/java/.../distribution/agreement/api/AgreementNotFoundException.java` | Domain exception |
| `src/main/java/.../distribution/agreement/api/AvailableProductionRunView.java` | Controller-local view record (package-private) |
| `src/main/java/.../distribution/agreement/api/AgreementController.java` | Spring MVC controller |
| `src/main/java/.../distribution/agreement/application/CreateAgreementUseCase.java` | Use case |
| `src/main/java/.../distribution/agreement/application/UpdateAgreementUseCase.java` | Use case |
| `src/main/java/.../distribution/agreement/application/DeleteAgreementUseCase.java` | Use case |
| `src/main/java/.../distribution/agreement/application/AgreementCommandApiImpl.java` | Command API impl |
| `src/main/java/.../distribution/agreement/application/AgreementQueryApiImpl.java` | Query API impl |
| `src/main/resources/templates/distributor/agreements.html` | Agreements list page |
| `src/main/resources/templates/distributor/agreement-form.html` | Create/edit form |
| `src/main/resources/static/js/agreement-delete.js` | Delete confirmation JS |
| `src/test/js/agreement-delete.test.js` | Vitest unit test for JS |
| `src/test/java/.../distribution/agreement/api/AgreementControllerTest.java` | Controller integration tests |

### Modified files

| File | Change |
|------|--------|
| `src/main/java/.../inventory/allocation/api/AllocationQueryApi.java` | Add `getAllocationsForDistributor` |
| `src/main/java/.../inventory/allocation/infrastructure/AllocationRepository.java` | Add `findByDistributorId` |
| `src/main/java/.../inventory/allocation/application/AllocationQueryApiImpl.java` | Implement new method |
| `src/main/resources/templates/distributor/detail.html` | Add "Pricing Agreements" section/link |

---

## Integration Points

| Module | How it's used | Who owns it |
|--------|---------------|-------------|
| `distribution/distributor` | `DistributorQueryApi` — verify distributor exists and belongs to label | `distribution/distributor` |
| `inventory/allocation` | `AllocationQueryApi.getAllocationsForDistributor` — build filtered dropdown | `inventory/allocation` |
| `inventory/productionrun` | `ProductionRunQueryApi` — enrich dropdown with release+format names | `inventory/productionrun` |
| `catalog/release` | `ReleaseQueryApi` — get release title for display in dropdown | `catalog/release` |

---

## Controller Responsibilities

**GET list:** Fetch agreements via `AgreementQueryApi.findByDistributorId`. Enrich each with ProductionRun (for format) and Release (for title) for display. Pass to template.

**GET create form:** Fetch allocations for distributor via `AllocationQueryApi.getAllocationsForDistributor`. Fetch all matching `ProductionRun` objects. Filter out those already having an agreement. Build `List<AvailableProductionRunView>` with display names (`"<Release Title> – <FORMAT>"`). Pass form + available runs to template.

**POST create:** Bind `AgreementForm`, call `AgreementCommandApi.create(...)`. Catch `DuplicateAgreementException` or `IllegalArgumentException` to re-render form with error message. On success, redirect to list.

**GET edit form:** Fetch agreement by ID, populate `AgreementForm`. Pass to template (productionRunId is read-only on edit).

**POST edit:** Bind form, call `AgreementCommandApi.update(...)`. On success, redirect to list.

**POST delete:** Call `AgreementCommandApi.delete(...)`. Redirect to list.

---

## JavaScript

`agreement-delete.js` exports a `confirmDelete(formId)` function that calls `window.confirm` and submits the form if confirmed. Used from the delete button in the agreements list template.

```javascript
// src/main/resources/static/js/agreement-delete.js
export const AgreementDelete = {
    confirmDelete(formId) {
        if (window.confirm('Delete this pricing agreement?')) {
            document.getElementById(formId).submit();
        }
    }
};
```

Template usage: `<button onclick="AgreementDelete.confirmDelete('delete-form-1')">Delete</button>`

---

## Assumptions

- `ProductionRunQueryApi` has `findById(Long id)` — used to get run details for display.
- `ReleaseQueryApi` has `findById(Long id)` — used to get release title for display.
- No new bounded context is needed; `distribution/` is sufficient.
- The existing Flyway sequence is at V28; next is V29.
- `AllocationRepository` can add `findByDistributorId` without breaking existing queries.

---

---

## Appendix A: Fixed-Amount Commission — Technical Design

### Key Decision: Enum storage

`CommissionType` is stored as `VARCHAR(20)` with a DB-level `CHECK` constraint (`'PERCENTAGE'`, `'FIXED_AMOUNT'`). The JPA entity uses `@Enumerated(EnumType.STRING)`. This avoids ordinal-fragility and keeps the DB column human-readable.

### Key Decision: No separate column

A single `commission_value NUMERIC(10,2)` column holds the value for both commission types. The interpretation (percentage 0–100 or positive monetary amount) is determined by `commission_type`. This is the simplest model; a second column would always be null for one type.

### DB Migration (V30)

```sql
ALTER TABLE pricing_agreement
    RENAME COLUMN commission_percentage TO commission_value;

ALTER TABLE pricing_agreement
    ADD COLUMN commission_type VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE';

ALTER TABLE pricing_agreement
    ADD CONSTRAINT chk_commission_type CHECK (commission_type IN ('PERCENTAGE', 'FIXED_AMOUNT'));

ALTER TABLE pricing_agreement
    ALTER COLUMN commission_type DROP DEFAULT;
```

Existing rows get `commission_type = 'PERCENTAGE'`, which correctly preserves their meaning.

### Updated Domain Record — `PricingAgreement.java`

```java
public record PricingAgreement(
    Long id,
    Long distributorId,
    Long productionRunId,
    BigDecimal unitPrice,
    CommissionType commissionType,
    BigDecimal commissionValue,
    Instant createdAt
) { ... }
```

### New Enum — `CommissionType.java` (in `distribution/agreement/domain/`)

```java
public enum CommissionType {
    PERCENTAGE,
    FIXED_AMOUNT
}
```

### Updated `AgreementForm.java`

```java
public class AgreementForm {
    private Long productionRunId;
    @NotNull private BigDecimal unitPrice;
    @NotNull private CommissionType commissionType;
    @NotNull private BigDecimal commissionValue;
}
```

### Updated API Signatures

```java
// AgreementCommandApi
PricingAgreement create(Long distributorId, Long productionRunId,
                        BigDecimal unitPrice, CommissionType commissionType, BigDecimal commissionValue);
PricingAgreement update(Long agreementId,
                        BigDecimal unitPrice, CommissionType commissionType, BigDecimal commissionValue);
```

### Updated `AgreementValidator.java`

Replace `validateCommissionPercentage` with:

```java
static void validateCommissionValue(CommissionType type, BigDecimal value) {
    if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) { ... }
    if (type == PERCENTAGE && value.compareTo(new BigDecimal("100")) > 0) { ... }
}
```

Both types require `value > 0`. Only `PERCENTAGE` additionally requires `value ≤ 100`.

### Display

`AgreementView` (in `distribution/distributor/api/`) gains a `displayCommission()` method:

```java
public String displayCommission() {
    return switch (agreement.commissionType()) {
        case PERCENTAGE    -> agreement.commissionValue().stripTrailingZeros().toPlainString() + "%";
        case FIXED_AMOUNT  -> agreement.commissionValue() + " €";
    };
}
```

The Thymeleaf templates call `${item.displayCommission()}` instead of accessing `commissionPercentage` directly.

### Form UI (agreement-form.html)

The commission section changes from one number input to:
1. A radio group or `<select>` for `commissionType` (PERCENTAGE / FIXED AMOUNT)
2. A number input for `commissionValue` with `step="0.01"` and `min="0"`. The `max="100"` constraint is removed (it was only valid for percentages); server-side validation enforces the range.

### Updated files

| File | Change |
|------|--------|
| `V30__add_commission_type_to_pricing_agreement.sql` | **New** — migration |
| `distribution/agreement/domain/CommissionType.java` | **New** — enum |
| `distribution/agreement/domain/PricingAgreement.java` | Replace `commissionPercentage` → `commissionType` + `commissionValue` |
| `distribution/agreement/infrastructure/PricingAgreementEntity.java` | Same field swap; `@Enumerated(EnumType.STRING)` on type |
| `distribution/agreement/api/AgreementCommandApi.java` | Updated method signatures |
| `distribution/agreement/api/AgreementForm.java` | Add `commissionType`; rename field |
| `distribution/agreement/application/AgreementValidator.java` | Replace `validateCommissionPercentage` with `validateCommissionValue(type, value)` |
| `distribution/agreement/application/CreateAgreementUseCase.java` | Pass new fields; call new validator |
| `distribution/agreement/application/UpdateAgreementUseCase.java` | Same |
| `distribution/agreement/application/AgreementCommandApiImpl.java` | Updated delegation |
| `distribution/distributor/api/AgreementView.java` | Add `displayCommission()` |
| `distribution/agreement/api/AgreementController.java` | Pass `CommissionType.values()` to form model |
| `templates/distributor/agreement-form.html` | Commission type selector + renamed value field |
| `templates/distributor/detail.html` | Use `displayCommission()` instead of raw `commissionPercentage` |
| `AgreementControllerTest.java` | Add/update tests for new fields and validation paths |

---

## Risks

- **Dropdown empty on create:** If a distributor has no allocations, the dropdown will be empty. The form should show a helpful message ("No production runs allocated to this distributor yet") rather than an empty select.
- **AllocationQueryApi extension:** Adding a method to a public interface is backwards-compatible but requires updating the impl class. Check for any mock-based tests of `AllocationQueryApi` that may need the new method added.
