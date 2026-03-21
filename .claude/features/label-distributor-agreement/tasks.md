# Tasks: Label-Distributor Pricing Agreement

## Status
In Review

## Tasks

### Task 1: DB migration
- [x] **1.1** Create `V29__create_pricing_agreement_table.sql` with `pricing_agreement` table: `id`, `distributor_id` (FK → distributor, ON DELETE CASCADE), `production_run_id` (FK → production_run, ON DELETE CASCADE), `unit_price` NUMERIC(10,2), `commission_percentage` NUMERIC(5,2), `created_at` TIMESTAMP WITH TIME ZONE DEFAULT NOW(), UNIQUE(distributor_id, production_run_id).
  - Verify: `./gradlew bootRun` starts without Flyway errors.

### Task 2: Domain record
- [x] **2.1** Create `distribution/agreement/domain/PricingAgreement.java` as a public record with fields: `id`, `distributorId`, `productionRunId`, `unitPrice` (BigDecimal), `commissionPercentage` (BigDecimal), `createdAt` (Instant). Add package-private `static PricingAgreement fromEntity(PricingAgreementEntity entity)`.
  - Verify: Compiles.

### Task 3: Infrastructure layer
- [x] **3.1** Create `distribution/agreement/infrastructure/PricingAgreementEntity.java` (package-private `@Entity`, `@Table(name = "pricing_agreement")`). Fields match schema. Use `@Column` constraints. `createdAt` defaults to `Instant.now()` on construction.
- [x] **3.2** Create `distribution/agreement/infrastructure/PricingAgreementRepository.java` (package-private, extends `JpaRepository<PricingAgreementEntity, Long>`). Methods:
  - `List<PricingAgreementEntity> findByDistributorId(Long distributorId)`
  - `boolean existsByDistributorIdAndProductionRunId(Long distributorId, Long productionRunId)`
  - Verify: `./gradlew build` compiles.

### Task 4: API interfaces and exceptions
- [x] **4.1** Create `distribution/agreement/api/AgreementCommandApi.java` (public interface):
  ```
  PricingAgreement create(Long distributorId, Long productionRunId, BigDecimal unitPrice, BigDecimal commissionPercentage);
  PricingAgreement update(Long agreementId, BigDecimal unitPrice, BigDecimal commissionPercentage);
  void delete(Long agreementId);
  ```
- [x] **4.2** Create `distribution/agreement/api/AgreementQueryApi.java` (public interface):
  ```
  Optional<PricingAgreement> findById(Long id);
  List<PricingAgreement> findByDistributorId(Long distributorId);
  boolean existsByDistributorIdAndProductionRunId(Long distributorId, Long productionRunId);
  ```
- [x] **4.3** Create `distribution/agreement/api/DuplicateAgreementException.java` (public, extends `RuntimeException`).
- [x] **4.4** Create `distribution/agreement/api/AgreementNotFoundException.java` (public, extends `RuntimeException`).

### Task 5: Application layer
- [x] **5.1** Create `distribution/agreement/application/CreateAgreementUseCase.java` (package-private `@Service`). Validate: `unitPrice > 0`, `commissionPercentage` in [0, 100]. Check uniqueness via repository; throw `DuplicateAgreementException` if duplicate. Save and return `PricingAgreement.fromEntity(...)`.
- [x] **5.2** Create `distribution/agreement/application/UpdateAgreementUseCase.java` (package-private `@Service`). Find by ID (throw `AgreementNotFoundException` if missing). Apply same validation. Save and return updated domain object.
- [x] **5.3** Create `distribution/agreement/application/DeleteAgreementUseCase.java` (package-private `@Service`). Find by ID (throw `AgreementNotFoundException` if missing). Delete.
- [x] **5.4** Create `distribution/agreement/application/AgreementQueryApiImpl.java` (package-private `@Service implements AgreementQueryApi`). Delegate to `PricingAgreementRepository`. Map entities to domain records via `PricingAgreement.fromEntity`.
- [x] **5.5** Create `distribution/agreement/application/AgreementCommandApiImpl.java` (package-private `@Service implements AgreementCommandApi`). Delegate to use cases.
  - Verify: `./gradlew build` passes.

### Task 6: Extend AllocationQueryApi
- [x] **6.1** Add `List<ChannelAllocation> getAllocationsForDistributor(Long distributorId)` to `inventory/allocation/api/AllocationQueryApi.java`.
- [x] **6.2** Add `List<ChannelAllocationEntity> findByDistributorId(Long distributorId)` to `inventory/allocation/infrastructure/AllocationRepository.java`.
- [x] **6.3** Implement the new method in `inventory/allocation/application/AllocationQueryApiImpl.java` — find by distributor, map to domain objects.
- [x] **6.4** Write a test in the existing `AllocationQueryApiImpl` test (or create one) that verifies `getAllocationsForDistributor` returns correct allocations.
  - Verify: `./gradlew test --tests "*AllocationQuery*"` passes.

### Task 7: Controller
- [x] **7.1** Create `distribution/agreement/api/AgreementForm.java` (public class, bean validation annotations): `productionRunId` (Long), `unitPrice` (BigDecimal, `@NotNull`), `commissionPercentage` (BigDecimal, `@NotNull`).
- [x] **7.2** Create `distribution/agreement/api/AvailableProductionRunView.java` (package-private record): `Long productionRunId`, `String displayName`.
- [x] **7.3** Create `distribution/agreement/api/AgreementController.java` (`@Controller`, `@RequestMapping("/labels/{labelId}/distributors/{distributorId}/agreements")`). Inject: `AgreementCommandApi`, `AgreementQueryApi`, `DistributorQueryApi`, `LabelQueryApi`, `AllocationQueryApi`, `ProductionRunQueryApi`, `ReleaseQueryApi`. Implement all 6 routes per spec. On `DuplicateAgreementException` or `IllegalArgumentException`, re-render form with `errorMessage` model attribute.
  - Verify: `./gradlew build` compiles.

### Task 8: Templates
- [x] **8.1** ~~Create `src/main/resources/templates/distributor/agreements.html`.~~ *Approach changed: agreements embedded inline in detail.html (see 8.3).*
- [x] **8.2** Create `src/main/resources/templates/distributor/agreement-form.html`. Create mode: show `productionRunId` dropdown (populated from `availableRuns`; if empty, show message "No production runs available"). Edit mode: show production run as read-only text (no dropdown). Fields: unit price (decimal input), commission % (decimal input). Validation error display. Submit button. CSRF token on form.
- [x] **8.3** Modified `src/main/resources/templates/distributor/detail.html`: added inline "Pricing Agreements" section (table with Edit/Delete, empty state). Updated `DistributorController.showDistributor` to pass `agreements` (list of `AgreementView`) to the model.

### Task 9: JavaScript
- [x] **9.1** Create `src/main/resources/static/js/agreement-delete.js`. Export `AgreementDelete = { confirmDelete(formId) { ... } }`.
- [x] **9.2** Create `src/main/resources/static/js/agreement-delete.test.js` (vitest config resolves tests from `src/main/resources/static/js/`). Test: when confirmed, form is submitted; when cancelled, form is not submitted. Mock `window.confirm` and `document.getElementById`.
  - Verify: `npm run test` passes (requires Node 18+; Node 24 works).

### Task 10: Controller integration tests
- [x] **10.1** Create `AgreementControllerTest.java`. Test coverage:
  - GET `/agreements` returns 200 with agreements list
  - GET `/agreements/new` returns 200 with available runs in model
  - POST `/agreements` with valid data redirects to list
  - POST `/agreements` with duplicate throws error and re-renders form with message
  - POST `/agreements` with invalid unit price (≤ 0) re-renders form with error
  - POST `/agreements` with commission > 100 re-renders form with error
  - GET `/agreements/{id}/edit` returns 200 with populated form
  - POST `/agreements/{id}` with valid data redirects to list
  - POST `/agreements/{id}/delete` deletes and redirects
  - Verify: `./gradlew test --tests "AgreementControllerTest"` passes.

---

## Appendix A Tasks: Fixed-Amount Commission

### Task A: DB Migration
- [x] **A.1** Create `src/main/resources/db/migration/V30__add_commission_type_to_pricing_agreement.sql`:
  - `RENAME COLUMN commission_percentage TO commission_value`
  - `ADD COLUMN commission_type VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE'`
  - `ADD CONSTRAINT chk_commission_type CHECK (commission_type IN ('PERCENTAGE', 'FIXED_AMOUNT'))`
  - `ALTER COLUMN commission_type DROP DEFAULT`
  - Verify: `./gradlew bootRun` starts without Flyway errors.

### Task B: Domain
- [ ] **B.1** Create `distribution/agreement/domain/CommissionType.java` — public enum with values `PERCENTAGE`, `FIXED_AMOUNT`.
- [ ] **B.2** Update `distribution/agreement/domain/PricingAgreement.java` — replace `commissionPercentage` field with `commissionType` (CommissionType) and `commissionValue` (BigDecimal). Update `fromEntity` accordingly.
  - Verify: Compiles. Fix any references that break (there will be cascading compile errors — fix them all before running tests).

### Task C: Infrastructure
- [ ] **C.1** Update `distribution/agreement/infrastructure/PricingAgreementEntity.java`:
  - Rename field `commissionPercentage` → `commissionValue`, update column mapping to `commission_value`.
  - Add field `commissionType` (CommissionType), annotate `@Enumerated(EnumType.STRING)`, column `commission_type`.
  - Update getter/setter names accordingly.
  - Verify: `./gradlew build` compiles.

### Task D: API layer
- [ ] **D.1** Update `distribution/agreement/api/AgreementCommandApi.java`:
  - `create(...)`: replace `BigDecimal commissionPercentage` with `CommissionType commissionType, BigDecimal commissionValue`.
  - `update(...)`: same replacement.
- [ ] **D.2** Update `distribution/agreement/api/AgreementForm.java`:
  - Remove `commissionPercentage` field.
  - Add `@NotNull CommissionType commissionType`.
  - Add `@NotNull BigDecimal commissionValue`.
  - Verify: Compiles.

### Task E: Application layer
- [ ] **E.1** Update `distribution/agreement/application/AgreementValidator.java`:
  - Remove `validateCommissionPercentage`.
  - Add `static void validateCommissionValue(CommissionType type, BigDecimal value)`:
    - Throws `IllegalArgumentException("Commission value must be greater than zero")` if `value ≤ 0`.
    - Throws `IllegalArgumentException("Commission percentage must be between 0 and 100")` if `type == PERCENTAGE && value > 100`.
- [ ] **E.2** Update `distribution/agreement/application/CreateAgreementUseCase.java` — pass `commissionType` and `commissionValue`; call `AgreementValidator.validateCommissionValue(commissionType, commissionValue)` instead of `validateCommissionPercentage`.
- [ ] **E.3** Update `distribution/agreement/application/UpdateAgreementUseCase.java` — same changes as E.2.
- [ ] **E.4** Update `distribution/agreement/application/AgreementCommandApiImpl.java` — update delegation to pass `commissionType` and `commissionValue`.
  - Verify: `./gradlew build` passes.

### Task F: View record
- [ ] **F.1** Update `distribution/distributor/api/AgreementView.java` — add `displayCommission()` method:
  ```java
  public String displayCommission() {
      return switch (agreement.commissionType()) {
          case PERCENTAGE   -> agreement.commissionValue().stripTrailingZeros().toPlainString() + "%";
          case FIXED_AMOUNT -> agreement.commissionValue() + " €";
      };
  }
  ```

### Task G: Controller
- [ ] **G.1** Update `distribution/agreement/api/AgreementController.java`:
  - On GET create form and GET edit form: add `CommissionType.values()` to model as `"commissionTypes"`.
  - On POST create and POST update: pass `form.getCommissionType()` and `form.getCommissionValue()` to the command API.
  - Verify: `./gradlew build` compiles.

### Task H: Templates
- [ ] **H.1** Update `src/main/resources/templates/distributor/agreement-form.html`:
  - Replace the single commission % input with:
    1. A `<select>` (or radio group) bound to `th:field="*{commissionType}"`, populated from `commissionTypes` with labels "Percentage (%)" and "Fixed Amount (€)".
    2. A number input bound to `th:field="*{commissionValue}"` with `step="0.01"` and `min="0"` (no `max`).
  - Update label text accordingly.
- [ ] **H.2** Update `src/main/resources/templates/distributor/detail.html`:
  - Replace `th:text="${item.agreement.commissionPercentage}"` with `th:text="${item.displayCommission()}"`.
  - Update the column header from "Commission %" to "Commission".

### Task I: Tests
- [ ] **I.1** Update `AgreementControllerTest.java`:
  - Update all existing test fixtures to supply `commissionType` (PERCENTAGE) and `commissionValue` instead of `commissionPercentage`.
  - Add test: POST create with `FIXED_AMOUNT` and value `> 0` → success.
  - Add test: POST create with `PERCENTAGE` and value `> 100` → form re-rendered with error.
  - Add test: POST create with `FIXED_AMOUNT` and value `≤ 0` → form re-rendered with error.
  - Verify: `./gradlew test --tests "AgreementControllerTest"` passes.
- [ ] **I.2** Write or update unit tests for `AgreementValidator`:
  - PERCENTAGE with 0 → valid
  - PERCENTAGE with 100 → valid
  - PERCENTAGE with 100.01 → throws
  - FIXED_AMOUNT with 0.01 → valid
  - FIXED_AMOUNT with 0 → throws
  - FIXED_AMOUNT with 101 → valid (no upper bound)
  - Verify: `./gradlew test --tests "*AgreementValidator*"` passes.

## Blockers
None.
