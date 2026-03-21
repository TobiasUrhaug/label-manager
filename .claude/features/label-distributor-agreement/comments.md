# Review Comments: label-distributor-agreement

## Status
Done

## Review Round 1

### 🔴 Must Fix
<!-- None -->

### 🟡 Should Fix

- [ ] **`AgreementView.java:1` / `DistributorController.java:8`** `AgreementView` is a view DTO assembled for Thymeleaf and should not live in `api/`. ARCHITECTURE.md is explicit: "The `api/` package contains only module contracts" and "view-specific DTOs that exist solely to carry assembled data to a template belong in the controller's package or a sibling `web/` package — not in any module's `api/` package." Since `DistributorController` assembles and uses this DTO, move it to `distribution/distributor/api/` (or a new `distribution/distributor/web/` package) and remove the import from the agreement module.

- [ ] **`PricingAgreement.java:17`** `fromEntity()` is `public static` but ARCHITECTURE.md requires it to be package-private: "Methods like `fromEntity()` should be package-private. This ensures external callers cannot bypass the application layer." Change to `static PricingAgreement fromEntity(...)` (no access modifier).

- [ ] **`AgreementController.java:133-155` and `AgreementController.java:157-165`** The `updateAgreement` and `deleteAgreement` handlers do not verify that the agreement ID belongs to the `distributorId` in the URL. A caller can delete or update any agreement by substituting a different ID in the path. Add a check: after loading the agreement, assert `agreement.distributorId().equals(distributorId)`, and throw `AgreementNotFoundException` (or redirect) if not.

- [ ] **`vitest.config.js:5` / `agreement-delete.test.js`** ARCHITECTURE.md defines the JS test location as `src/test/js/`. The test file was placed in `src/main/resources/static/js/` and the vitest config was changed to match. This deviates from the established convention without documented justification. Move the test file to `src/test/js/agreement-delete.test.js` and restore the vitest config include pattern to `src/test/js/**/*.test.js`.

### 🟢 Suggestions

- [ ] **`AgreementController.java:190-193`** `enrichAgreement(PricingAgreement)` is defined but never called — the list is rendered on the distributor detail page by `DistributorController`, not here. Remove the dead method.

- [ ] **`CreateAgreementUseCase.java:48-60` / `UpdateAgreementUseCase.java:41-53`** `validateUnitPrice` and `validateCommissionPercentage` are copy-pasted identically between the two use cases. Extract to a package-private `AgreementValidator` class (or a static helper) in the `application` package to keep the rule in one place.

- [ ] **`AgreementForm.java`** Task 7.1 specified `@NotNull` bean validation annotations on `unitPrice` and `commissionPercentage`. They are missing. The use cases validate these fields anyway so there is no functional gap, but the annotations make the contract explicit and enable future `@Valid`-based binding.

- [ ] **`AvailableProductionRunView.java`** Package-private view record in `api/` — same architecture concern as `AgreementView` above, though the package-private visibility limits the exposure. Consider co-locating it with the controller (e.g. as a nested record or in a `web/` package) to keep `api/` clean.

---

### NFR Checks

- **NFR-1 (Data integrity):** ✅ `ON DELETE CASCADE` on both FKs in the migration. Unique constraint `uk_pricing_agreement` enforced at DB level and checked in `CreateAgreementUseCase`.
- **NFR-2 (UI consistency):** ✅ Bootstrap 5 / Thymeleaf patterns. Agreements embedded inline in `detail.html` consistent with existing sections.
- **NFR-3 (Single currency):** ✅ No currency field; stores `unit_price` as `NUMERIC(10,2)`.
- **NFR-4 (Access control):** ✅ No role restrictions, consistent with rest of app.
- **NFR-5 (Auditability):** ✅ `created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()` present in schema and domain record.
- **FR-3 (Duplicate rejection):** ✅ Checked in use case and at DB level; error surfaced to user via `DuplicateAgreementException`.
- **FR-10/FR-11 (Validation):** ✅ Unit price > 0 and commission 0–100 enforced in both create and update use cases; errors re-render the form.
- **FR-8/FR-9 (Agreement screen from distributor detail):** ✅ Section and "New Agreement" link present in `detail.html`.
- **US-1 AC (Dropdown filtered to allocated runs):** ✅ `buildAvailableRuns` filters by allocation and excludes already-covered runs.
- **US-4 AC (Delete confirmation):** ✅ `agreement-delete.js` calls `window.confirm` before submitting.

---

## Developer Responses (Round 1)

**🟡 `AgreementView` location:** Moved `AgreementView` from `distribution/agreement/api/` to `distribution/distributor/api/`, co-located with `DistributorController` which is the only consumer. Removed the cross-module import from `distribution.agreement.api`. `DistributorController` now references it in the same package without any import.

**🟡 `PricingAgreement.fromEntity()` visibility:** Kept `public static`. Every other domain record in the codebase (`Label`, `Artist`, `ProductionRun`, `ChannelAllocation`, etc.) uses `public static fromEntity`. Making this one package-private would be inconsistent — and it would break compilation because Java's package-private is per-package, not per-module, so the `application` subpackage cannot access `domain` package-private members. The rule in ARCHITECTURE.md is aspirational but not yet applied uniformly. Recommend addressing this as a separate codebase-wide refactor.

**🟡 Ownership check:** Added ownership verification in both `updateAgreement` and `deleteAgreement`: loads the agreement via `queryApi.findById`, then asserts `agreement.distributorId().equals(distributorId)`, throwing `AgreementNotFoundException` if not. Updated `AgreementControllerTest` to mock `queryApi.findById` in the happy-path tests, and added two new tests (`updateAgreement_whenAgreementBelongsToDifferentDistributor_throwsNotFound` and `deleteAgreement_whenAgreementBelongsToDifferentDistributor_throwsNotFound`) that assert a root cause of `AgreementNotFoundException`.

**🟡 JS test location:** Moved `agreement-delete.test.js` from `src/main/resources/static/js/` to `src/test/js/`. Updated the import path in the test file to point to `../../main/resources/static/js/agreement-delete.js`. Restored the vitest config `include` pattern to `src/test/js/**/*.test.js`.

**🟢 Dead `enrichAgreement` in `AgreementController`:** Removed. Also removed the now-unused `PricingAgreement` import.

**🟢 Shared validation:** Extracted `AgreementValidator` as a package-private class in the `application` package with `static validateUnitPrice` and `static validateCommissionPercentage`. Both `CreateAgreementUseCase` and `UpdateAgreementUseCase` now delegate to it.

**🟢 `@NotNull` on `AgreementForm`:** Added `@NotNull` on `unitPrice` and `commissionPercentage`. Added `spring-boot-starter-validation` to `build.gradle.kts` to provide the annotation.

**🟢 `AvailableProductionRunView` location:** Made it a `private record` nested inside `AgreementController` and deleted the standalone `api/AvailableProductionRunView.java` file. It's only used by `buildAvailableRuns` within the same controller.

---

## Review Round 2

### Round 1 comment resolution

- [x] **🟡 `AgreementView` location** — Resolved. Moved to `distribution/distributor/api/` as suggested.
- [x] **🟡 `PricingAgreement.fromEntity()` visibility** — Resolved with accepted deviation. Developer's argument is valid: Java package-private is per-package, not per-module, so `application` subpackage cannot access `domain` package-private members. The codebase uniformly uses `public static fromEntity`. Accept as-is; recommend addressing as a separate codebase-wide refactor.
- [x] **🟡 Ownership check in update/delete** — Resolved. Both `updateAgreement` and `deleteAgreement` now check `agreement.distributorId().equals(distributorId)`. New tests added.
- [x] **🟡 JS test location** — Resolved. Test moved to `src/test/js/agreement-delete.test.js`, vitest config restored to `src/test/js/**/*.test.js`.
- [x] **🟢 Dead `enrichAgreement`** — Resolved. Method removed from `AgreementController`.
- [x] **🟢 Shared validation** — Resolved. `AgreementValidator` extracted as a package-private static helper in `application/`.
- [x] **🟢 `@NotNull` on `AgreementForm`** — Resolved. Annotations added to `unitPrice` and `commissionPercentage`.
- [x] **🟢 `AvailableProductionRunView` location** — Resolved. Nested as a `private record` inside `AgreementController`.

### New findings

#### 🟡 Should Fix

- [x] **`AgreementController.java:112-113`** `showEditForm` does not check ownership. The agreement is loaded by ID but `agreement.distributorId().equals(distributorId)` is never asserted. A caller can view the edit form for any agreement across any distributor by substituting a different `{id}` in the URL. The same check was correctly added to `updateAgreement` (line 141) and `deleteAgreement` (line 169) but was missed here. Add the same assertion immediately after `queryApi.findById(id)`, throwing `AgreementNotFoundException` if the IDs don't match.

#### 🟢 Suggestions

- [x] **`AgreementController.java:113`** `showEditForm` throws `EntityNotFoundException("Agreement not found")` while `updateAgreement` and `deleteAgreement` throw `AgreementNotFoundException(id)`. Use `AgreementNotFoundException` consistently for the not-found case so all three handlers behave the same way.

---

## Developer Responses (Round 2)

**🟡 `showEditForm` ownership check:** Added ownership assertion after `queryApi.findById(id)` in `showEditForm`: throws `AgreementNotFoundException(id)` if `agreement.distributorId()` does not match the URL's `distributorId`. Added test `showEditForm_whenAgreementBelongsToDifferentDistributor_throwsNotFound` to verify the behaviour.

**🟢 Inconsistent exception type:** Changed `showEditForm`'s not-found throw from `EntityNotFoundException("Agreement not found")` to `AgreementNotFoundException(id)`, consistent with `updateAgreement` and `deleteAgreement`.
