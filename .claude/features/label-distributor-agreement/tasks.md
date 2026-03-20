# Tasks: Label-Distributor Pricing Agreement

## Status
Draft

## Tasks

### Task 1: DB migration
- [ ] **1.1** Create `V29__create_pricing_agreement_table.sql` with `pricing_agreement` table: `id`, `distributor_id` (FK → distributor, ON DELETE CASCADE), `production_run_id` (FK → production_run, ON DELETE CASCADE), `unit_price` NUMERIC(10,2), `commission_percentage` NUMERIC(5,2), `created_at` TIMESTAMP WITH TIME ZONE DEFAULT NOW(), UNIQUE(distributor_id, production_run_id).
  - Verify: `./gradlew bootRun` starts without Flyway errors.

### Task 2: Domain record
- [ ] **2.1** Create `distribution/agreement/domain/PricingAgreement.java` as a public record with fields: `id`, `distributorId`, `productionRunId`, `unitPrice` (BigDecimal), `commissionPercentage` (BigDecimal), `createdAt` (Instant). Add package-private `static PricingAgreement fromEntity(PricingAgreementEntity entity)`.
  - Verify: Compiles.

### Task 3: Infrastructure layer
- [ ] **3.1** Create `distribution/agreement/infrastructure/PricingAgreementEntity.java` (package-private `@Entity`, `@Table(name = "pricing_agreement")`). Fields match schema. Use `@Column` constraints. `createdAt` defaults to `Instant.now()` on construction.
- [ ] **3.2** Create `distribution/agreement/infrastructure/PricingAgreementRepository.java` (package-private, extends `JpaRepository<PricingAgreementEntity, Long>`). Methods:
  - `List<PricingAgreementEntity> findByDistributorId(Long distributorId)`
  - `boolean existsByDistributorIdAndProductionRunId(Long distributorId, Long productionRunId)`
  - Verify: `./gradlew build` compiles.

### Task 4: API interfaces and exceptions
- [ ] **4.1** Create `distribution/agreement/api/AgreementCommandApi.java` (public interface):
  ```
  PricingAgreement create(Long distributorId, Long productionRunId, BigDecimal unitPrice, BigDecimal commissionPercentage);
  PricingAgreement update(Long agreementId, BigDecimal unitPrice, BigDecimal commissionPercentage);
  void delete(Long agreementId);
  ```
- [ ] **4.2** Create `distribution/agreement/api/AgreementQueryApi.java` (public interface):
  ```
  Optional<PricingAgreement> findById(Long id);
  List<PricingAgreement> findByDistributorId(Long distributorId);
  boolean existsByDistributorIdAndProductionRunId(Long distributorId, Long productionRunId);
  ```
- [ ] **4.3** Create `distribution/agreement/api/DuplicateAgreementException.java` (public, extends `RuntimeException`).
- [ ] **4.4** Create `distribution/agreement/api/AgreementNotFoundException.java` (public, extends `RuntimeException`).

### Task 5: Application layer
- [ ] **5.1** Create `distribution/agreement/application/CreateAgreementUseCase.java` (package-private `@Service`). Validate: `unitPrice > 0`, `commissionPercentage` in [0, 100]. Check uniqueness via repository; throw `DuplicateAgreementException` if duplicate. Save and return `PricingAgreement.fromEntity(...)`.
- [ ] **5.2** Create `distribution/agreement/application/UpdateAgreementUseCase.java` (package-private `@Service`). Find by ID (throw `AgreementNotFoundException` if missing). Apply same validation. Save and return updated domain object.
- [ ] **5.3** Create `distribution/agreement/application/DeleteAgreementUseCase.java` (package-private `@Service`). Find by ID (throw `AgreementNotFoundException` if missing). Delete.
- [ ] **5.4** Create `distribution/agreement/application/AgreementQueryApiImpl.java` (package-private `@Service implements AgreementQueryApi`). Delegate to `PricingAgreementRepository`. Map entities to domain records via `PricingAgreement.fromEntity`.
- [ ] **5.5** Create `distribution/agreement/application/AgreementCommandApiImpl.java` (package-private `@Service implements AgreementCommandApi`). Delegate to use cases.
  - Verify: `./gradlew build` passes.

### Task 6: Extend AllocationQueryApi
- [ ] **6.1** Add `List<ChannelAllocation> getAllocationsForDistributor(Long distributorId)` to `inventory/allocation/api/AllocationQueryApi.java`.
- [ ] **6.2** Add `List<ChannelAllocationEntity> findByDistributorId(Long distributorId)` to `inventory/allocation/infrastructure/AllocationRepository.java`.
- [ ] **6.3** Implement the new method in `inventory/allocation/application/AllocationQueryApiImpl.java` — find by distributor, map to domain objects.
- [ ] **6.4** Write a test in the existing `AllocationQueryApiImpl` test (or create one) that verifies `getAllocationsForDistributor` returns correct allocations.
  - Verify: `./gradlew test --tests "*AllocationQuery*"` passes.

### Task 7: Controller
- [ ] **7.1** Create `distribution/agreement/api/AgreementForm.java` (public class, bean validation annotations): `productionRunId` (Long), `unitPrice` (BigDecimal, `@NotNull`), `commissionPercentage` (BigDecimal, `@NotNull`).
- [ ] **7.2** Create `distribution/agreement/api/AvailableProductionRunView.java` (package-private record): `Long productionRunId`, `String displayName`.
- [ ] **7.3** Create `distribution/agreement/api/AgreementController.java` (`@Controller`, `@RequestMapping("/labels/{labelId}/distributors/{distributorId}/agreements")`). Inject: `AgreementCommandApi`, `AgreementQueryApi`, `DistributorQueryApi`, `LabelQueryApi`, `AllocationQueryApi`, `ProductionRunQueryApi`, `ReleaseQueryApi`. Implement all 6 routes per spec. On `DuplicateAgreementException` or `IllegalArgumentException`, re-render form with `errorMessage` model attribute.
  - Verify: `./gradlew build` compiles.

### Task 8: Templates
- [ ] **8.1** Create `src/main/resources/templates/distributor/agreements.html`. Layout: title "Pricing Agreements – {distributorName}", back link to distributor detail, "New Agreement" button. Table with columns: Production Run, Unit Price, Commission %, Actions (Edit, Delete). Empty state message. Each Delete button triggers `AgreementDelete.confirmDelete(formId)`. Each delete form is a POST to `.../agreements/{id}/delete` with CSRF token.
- [ ] **8.2** Create `src/main/resources/templates/distributor/agreement-form.html`. Create mode: show `productionRunId` dropdown (populated from `availableRuns`; if empty, show message "No production runs available"). Edit mode: show production run as read-only text (no dropdown). Fields: unit price (decimal input), commission % (decimal input). Validation error display. Submit button. CSRF token on form.
- [ ] **8.3** Modify `src/main/resources/templates/distributor/detail.html`: add a "Pricing Agreements" card below the Returns section. Show count of agreements (or "No agreements yet"). Link to `/labels/{labelId}/distributors/{distributorId}/agreements`. Requires the controller to pass `agreementCount` (or `agreements` list) to the model — update `DistributorController.showDistributor` accordingly.

### Task 9: JavaScript
- [ ] **9.1** Create `src/main/resources/static/js/agreement-delete.js`. Export `AgreementDelete = { confirmDelete(formId) { ... } }`.
- [ ] **9.2** Create `src/test/js/agreement-delete.test.js`. Test: when confirmed, form is submitted; when cancelled, form is not submitted. Mock `window.confirm` and `document.getElementById`.
  - Verify: `npm run test` passes.

### Task 10: Controller integration tests
- [ ] **10.1** Create `AgreementControllerTest.java`. Test coverage:
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

## Blockers
None.
