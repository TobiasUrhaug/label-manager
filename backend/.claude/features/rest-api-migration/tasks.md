# Tasks: rest-api-migration

## Status
In Progress

## Goal

Replace all Thymeleaf `@Controller` classes with `@RestController` classes under an `/api/` prefix.
Delete all Thymeleaf templates, static JS, and JS tests. Remove Thymeleaf dependencies from the build.

The domain and application layers (use cases, CommandApi/QueryApi interfaces, repositories) are **untouched**.
Only the HTTP adapter layer changes.

## Already REST (no action needed)
- `RegisterController` → `POST /api/auth/register`
- `SessionController` → `GET /api/session`
- `InvoiceExtractionController` → `POST /api/costs/extract`
- `SecurityConfig` → already SPA-ready (`csrf().spa()`, JSON auth handlers)

## Conventions for all tasks

**Controller:**
- `@RestController` (not `@Controller`)
- All routes under `/api/` prefix
- Return values: JSON body / `ResponseEntity`
- POST (create) → `201 Created`
- GET → `200 OK` with JSON body
- PUT (update) → `200 OK` with updated resource or `204 No Content`
- DELETE → `204 No Content`
- Replace `redirect:` strings with proper status codes
- Replace `@ModelAttribute FormClass form` with `@RequestBody RequestRecord request`
- Form classes that previously bound HTML params are deleted and replaced by request records (inline records in the controller or dedicated files in the `api` package)

**Tests:**
- `@WebMvcTest` + `@Import(TestSecurityConfig.class)` stays the same
- Mutations: `.contentType(APPLICATION_JSON).content("""{ ... }""")` instead of `.param(...)`
- Assertions: `jsonPath("$.field")` instead of `model().attribute(...)` / `view()`
- No `view()` or `model()` assertions
- DELETE/204: `andExpect(status().isNoContent())`
- POST/201: `andExpect(status().isCreated())`
- CSRF: keep `.with(csrf())` on all mutation requests

Follow Red-Green-Refactor: update the test first (it will fail), then implement the new controller.

---

### Task 1: Delete AuthController and Thymeleaf register page

`AuthController` serves `GET /register` and `POST /register` as a Thymeleaf form.
`RegisterController` already handles register as REST at `POST /api/auth/register`. The Thymeleaf version is dead.

**What to delete:**
- `identity/api/user/AuthController.java`
- `identity/api/user/RegistrationForm.java` (if it exists — check before deleting)
- `src/main/resources/templates/auth/register.html`

**What to update:**
- `SecurityConfig`: remove `/register` from the `permitAll()` matcher (React SPA handles its own register route, which calls the REST endpoint; the Spring-managed `/register` path no longer exists)

Run `./gradlew build` to confirm no compile errors.

- [x] Task 1: Delete `AuthController`, its form class, the Thymeleaf register template, and clean up `SecurityConfig`

---

### Task 2: Convert DashboardController → `GET /api/dashboard`

**Current:** `GET /dashboard` → Thymeleaf view with `labels` and `artists` model attributes.

**New:** `GET /api/dashboard` → JSON response.

**Route:** `GET /api/dashboard`

**Response body:**
```json
{
  "labels": [ { /* Label fields */ } ],
  "artists": [ { /* Artist fields */ } ]
}
```

Create a `DashboardResponse` record in the `dashboard` package (or as a private record inside the controller) to hold the two lists. The response fields mirror what the domain objects expose.

**Test (`DashboardControllerTest`):**
- Existing test: `GET /dashboard` → `view("dashboard")` and `model` assertions
- Rewrite to: `GET /api/dashboard` → `status().isOk()` + `jsonPath("$.labels")` + `jsonPath("$.artists")`

- [x] Task 2: Convert `DashboardController` to `@RestController` at `GET /api/dashboard`, update test

---

### Task 3: Convert ArtistController

**Current routes:** `GET /artists/{id}`, `POST /artists`, `PUT /artists/{id}`, `DELETE /artists/{id}`

**New routes:** all under `/api/artists`

| Old | New | Response |
|-----|-----|----------|
| `GET /artists/{id}` | `GET /api/artists/{id}` | `200` artist JSON |
| `POST /artists` | `POST /api/artists` | `201` |
| `PUT /artists/{id}` | `PUT /api/artists/{id}` | `204` |
| `DELETE /artists/{id}` | `DELETE /api/artists/{id}` | `204` |

Delete `CreateArtistForm.java` and `UpdateArtistForm.java`. Replace with inline request records in the controller. The mapping logic currently in those forms (`toRealName()`, `toAddress()`) moves into the request records.

**Test (`ArtistControllerTest`):**
- `artist_returnsArtistView()` → assert `status().isOk()` + `jsonPath("$.artistName").value("DJ Cool")`
- `artist_returns404_whenNotFound()` → unchanged (`status().isNotFound()`)
- `createArtist_*` → POST with JSON body, assert `status().isCreated()`
- `updateArtist_*` → PUT with JSON body, assert `status().isNoContent()`
- `deleteArtist_*` → assert `status().isNoContent()`

- [x] Task 3: Convert `ArtistController` to `@RestController` at `/api/artists`, delete old form classes, update test

---

### Task 4: Convert LabelController

**Current routes:** `GET /labels/{id}`, `POST /labels`, `PUT /labels/{id}`, `DELETE /labels/{id}`

**New routes:** all under `/api/labels`

| Old | New | Response |
|-----|-----|----------|
| `GET /labels/{id}` | `GET /api/labels/{id}` | `200` — label + releases + artists + distributors |
| `POST /labels` | `POST /api/labels` | `201` |
| `PUT /labels/{id}` | `PUT /api/labels/{id}` | `204` |
| `DELETE /labels/{id}` | `DELETE /api/labels/{id}` | `204` |

The GET currently stuffs many model attributes (name, email, website, address, owner, releases, artists, allFormats, distributors, allChannelTypes). For the REST response, return a `LabelDetailResponse` record that bundles the label fields and the related lists. Drop `allFormats` and `allChannelTypes` — those were only needed to populate Thymeleaf select boxes.

Delete `CreateLabelForm.java` and `UpdateLabelForm.java`. The tests for `UpdateLabelForm` live in `api/UpdateLabelFormTest.java` — delete that test file too.

**Test (`LabelControllerTest`):**
- GET: assert `status().isOk()` + `jsonPath("$.name").value("My Label")` + `jsonPath("$.releases")` etc.
- POST: JSON body → `201`
- PUT: JSON body → `204`
- DELETE: `204`

- [x] Task 4: Convert `LabelController` to `@RestController` at `/api/labels`, delete old form classes and form test, update controller test

---

### Task 5: Convert ReleaseController

**Current routes:** all under `/labels/{labelId}/releases`

**New routes:** all under `/api/labels/{labelId}/releases`

| Old | New | Response |
|-----|-----|----------|
| `GET /labels/{labelId}/releases/{releaseId}` | `GET /api/labels/{labelId}/releases/{releaseId}` | `200` release detail JSON |
| `POST /labels/{labelId}/releases` | `POST /api/labels/{labelId}/releases` | `201` |
| `PUT /labels/{labelId}/releases/{releaseId}` | `PUT /api/labels/{labelId}/releases/{releaseId}` | `204` |
| `DELETE /labels/{labelId}/releases/{releaseId}` | `DELETE /api/labels/{labelId}/releases/{releaseId}` | `204` |

The GET response assembles: release name/date, artists, tracks (with artists resolved), formats, costs, production runs with allocation, distributors, release sales, totalUnitsSold. Keep all this as a single aggregate JSON response — retain the existing assembly logic but serialize to JSON instead of populating a Model.

The `TrackView`, `ReleaseSaleView`, `ProductionRunWithAllocation` types are already non-Thymeleaf data structures; they become JSON serialized directly.

Drop `allFormats`, `allCostTypes`, `physicalFormats` from the response — those were select-box population. The React client has its own enums.

Delete `CreateReleaseForm.java`, `UpdateReleaseForm.java`, and `api/UpdateReleaseFormTest.java`.

**Test (`ReleaseControllerTest`):**
- GET: assert `status().isOk()` + key `jsonPath` assertions on `$.name`, `$.artists`, `$.tracks`, `$.costs`
- POST: JSON body → `201`
- PUT: JSON body → `204`
- DELETE: `204`

- [x] Task 5: Convert `ReleaseController` to `@RestController` at `/api/labels/{labelId}/releases`, delete form classes and form test, update controller test

---

### Task 6: Convert DistributorController

**Current routes:** all under `/labels/{labelId}/distributors`

**New routes:** all under `/api/labels/{labelId}/distributors`

| Old | New | Response |
|-----|-----|----------|
| `GET /labels/{labelId}/distributors/{distributorId}` | `GET /api/labels/{labelId}/distributors/{distributorId}` | `200` distributor detail JSON |
| `POST /labels/{labelId}/distributors` | `POST /api/labels/{labelId}/distributors` | `201` |
| `DELETE /labels/{labelId}/distributors/{distributorId}` | `DELETE /api/labels/{labelId}/distributors/{distributorId}` | `204` |

The GET response includes: distributor, label, sales, returns, agreements (enriched with production run display name). The `AgreementView` type and `enrichAgreement` helper stay; they serialize to JSON directly.

Delete `AddDistributorForm.java`.

**Test (`DistributorControllerTest`):**
- GET: `200` + jsonPath assertions
- POST: JSON body → `201`
- DELETE: `204`

- [x] Task 6: Convert `DistributorController` to `@RestController` at `/api/labels/{labelId}/distributors`, delete form class, update test

---

### Task 7: Convert AgreementController

**Current routes:** all under `/labels/{labelId}/distributors/{distributorId}/agreements`

**New routes:** all under `/api/labels/{labelId}/distributors/{distributorId}/agreements`

| Old | New | Response |
|-----|-----|----------|
| `GET /agreements` | remove — was just a redirect | — |
| `GET /agreements/new` | remove — was a Thymeleaf form page | — |
| `POST /agreements` | `POST /api/.../agreements` | `201` |
| `GET /agreements/{id}/edit` | remove — was a Thymeleaf form page | — |
| `POST /agreements/{id}` | `PUT /api/.../agreements/{id}` | `204` |
| `POST /agreements/{id}/delete` | `DELETE /api/.../agreements/{id}` | `204` |

The form-showing GET endpoints (`/new`, `/{id}/edit`) are Thymeleaf patterns with no REST equivalent. Delete them. The React client renders its own forms.

Error handling: `DuplicateAgreementException` and `IllegalArgumentException` were previously caught to re-render the form with an error message. In REST, let them propagate as `400 Bad Request` — add a `@ExceptionHandler` in the controller or rely on a global handler.

The `AvailableProductionRunView` record and `buildAvailableRuns()` helper were used to populate a Thymeleaf select. They may still be useful for a future "available runs" query endpoint, but drop them from this controller for now — remove any dead code once the form endpoints are gone.

Delete `AgreementForm.java`.

**Test (`AgreementControllerTest`):**
- POST: JSON body → `201`
- PUT: JSON body → `204`
- DELETE: `204`
- Error case (duplicate): → `400`

- [x] Task 7: Convert `AgreementController` to `@RestController`, remove form-showing endpoints, delete form class, update test

---

### Task 8: Convert CostController

**Current routes:** scattered across multiple paths (no `@RequestMapping` on the class).

**New routes:** keep the same path structure but add `/api/` prefix and switch from redirects to status codes.

| Old | New | Response |
|-----|-----|----------|
| `POST /labels/{labelId}/releases/{releaseId}/costs` | `POST /api/labels/{labelId}/releases/{releaseId}/costs` | `201` |
| `POST /labels/{labelId}/costs` | `POST /api/labels/{labelId}/costs` | `201` |
| `GET /costs/{costId}/document` | `GET /api/costs/{costId}/document` | `200` file stream (keep existing `ResponseEntity<InputStreamResource>` — no change) |
| `DELETE /labels/{labelId}/releases/{releaseId}/costs/{costId}` | `DELETE /api/labels/{labelId}/releases/{releaseId}/costs/{costId}` | `204` |
| `DELETE /labels/{labelId}/costs/{costId}` | `DELETE /api/labels/{labelId}/costs/{costId}` | `204` |
| `PUT /labels/{labelId}/releases/{releaseId}/costs/{costId}` | `PUT /api/labels/{labelId}/releases/{releaseId}/costs/{costId}` | `204` |
| `PUT /labels/{labelId}/costs/{costId}` | `PUT /api/labels/{labelId}/costs/{costId}` | `204` |

Cost registration takes multipart form data (JSON fields + optional file upload). Keep `@RequestParam` style for the multipart fields — do not switch to `@RequestBody` for the cost endpoints since file upload requires `multipart/form-data`. The `toDocumentUpload()` helper and `ALLOWED_CONTENT_TYPES` stay unchanged.

Delete `RegisterCostForm.java` if its fields can be bound directly via `@RequestParam` in the controller signature, or keep it as a multipart binding object — the developer's call.

**Test (`CostControllerTest`):**
- POST: multipart request → `201`
- DELETE: `204`
- PUT: multipart request → `204`
- Document download: `200` with correct content-type header (existing assertions likely already correct)

- [x] Task 8: Convert `CostController` to `@RestController` with `/api/` prefix, update test

---

### Task 9: Convert ProductionRunController and AllocateController

**ProductionRunController** — under `/labels/{labelId}/releases/{releaseId}/production-runs`:

| Old | New | Response |
|-----|-----|----------|
| `POST /production-runs` | `POST /api/labels/{labelId}/releases/{releaseId}/production-runs` | `201` |
| `DELETE /production-runs/{productionRunId}` | `DELETE /api/.../production-runs/{productionRunId}` | `204` |

Delete `AddProductionRunForm.java`.

**AllocateController** — under `/labels/{labelId}/releases/{releaseId}/production-runs/{runId}`:

| Old | New | Response |
|-----|-----|----------|
| `POST /allocations` | `POST /api/.../production-runs/{runId}/allocations` | `204` on success, `400` on validation failure |
| `POST /bandcamp-cancellations` | `POST /api/.../production-runs/{runId}/bandcamp-cancellations` | `204` on success, `400` on validation failure |

The current `AllocateController` uses `RedirectAttributes` flash messages for validation errors. In REST, return `400 Bad Request` with a JSON error body instead. The validation (quantity > 0, locationType not null, distributorId required for DISTRIBUTOR type) stays as-is but throws `ResponseStatusException(BAD_REQUEST)` or returns `ResponseEntity.badRequest()`.

`InsufficientInventoryException` → `400 Bad Request`.

Delete `AllocateForm.java` and `CancelBandcampReservationForm.java`.

**Tests (`ProductionRunControllerTest`, `AllocateControllerTest`):**
- POST: JSON body → `201` / `204`
- DELETE: `204`
- Allocation validation errors: `400`

- [ ] Task 9: Convert `ProductionRunController` and `AllocateController` to `@RestController` with `/api/` prefix, update tests

---

### Task 10: Convert SaleController

**Current routes:** all under `/labels/{labelId}/sales`

**New routes:** all under `/api/labels/{labelId}/sales`

| Old | New | Response |
|-----|-----|----------|
| `GET /sales` | `GET /api/labels/{labelId}/sales` | `200` list + totalRevenue |
| `GET /sales/new` | remove — Thymeleaf form page | — |
| `POST /sales` | `POST /api/labels/{labelId}/sales` | `201` |
| `GET /sales/{saleId}` | `GET /api/labels/{labelId}/sales/{saleId}` | `200` sale detail |
| `GET /sales/{saleId}/edit` | remove — Thymeleaf form page | — |
| `POST /sales/{saleId}` (update) | `PUT /api/labels/{labelId}/sales/{saleId}` | `200` updated sale |
| `POST /sales/{saleId}/delete` | `DELETE /api/labels/{labelId}/sales/{saleId}` | `204` |

The list GET (`/sales`) currently populates label, sales list, and totalRevenue. Return these as JSON.

The sale detail GET enriches line items with release names. Keep that enrichment; serialize to JSON.

Error handling for `registerSale` and `submitEdit` currently catches exceptions and re-renders the form. In REST, let domain exceptions propagate as `400 Bad Request`.

Delete `RegisterSaleForm.java`, `EditSaleForm.java`, `SaleLineItemForm.java`.

**Test (`SaleControllerTest`):**
- GET list: `200` + jsonPath on sales and totalRevenue
- POST: JSON body → `201`
- GET detail: `200` + jsonPath on sale fields + enriched line items
- PUT: JSON body → `200`
- DELETE: `204`

- [ ] Task 10: Convert `SaleController` to `@RestController` at `/api/labels/{labelId}/sales`, remove form-showing endpoints, delete form classes, update test

---

### Task 11: Convert ReturnController

**Current routes:** all under `/labels/{labelId}/returns`

**New routes:** all under `/api/labels/{labelId}/returns`

| Old | New | Response |
|-----|-----|----------|
| `GET /returns` | `GET /api/labels/{labelId}/returns` | `200` list |
| `GET /returns/new` | remove | — |
| `POST /returns` | `POST /api/labels/{labelId}/returns` | `201` |
| `GET /returns/{returnId}` | `GET /api/labels/{labelId}/returns/{returnId}` | `200` detail |
| `GET /returns/{returnId}/edit` | remove | — |
| `POST /returns/{returnId}` (update) | `PUT /api/labels/{labelId}/returns/{returnId}` | `200` |
| `POST /returns/{returnId}/delete` | `DELETE /api/labels/{labelId}/returns/{returnId}` | `204` |

The list GET currently includes label, returns, distributors, distributorNames. Return as JSON.

The detail GET enriches line items with release names and resolves the distributor. Keep this; serialize to JSON.

Delete `RegisterReturnForm.java`, `EditReturnForm.java`, `ReturnLineItemForm.java`.

**Test (`ReturnControllerTest`):**
- GET list: `200` + jsonPath
- POST: JSON body → `201`
- GET detail: `200` + jsonPath with enriched data
- PUT: JSON body → `200`
- DELETE: `204`

- [ ] Task 11: Convert `ReturnController` to `@RestController` at `/api/labels/{labelId}/returns`, remove form-showing endpoints, delete form classes, update test

---

### Task 12: Update SecurityConfig

Remove `/register` and `/css/**` / `/js/**` from the `permitAll()` matcher — these Thymeleaf-era paths no longer exist.

New `permitAll()` set should be: `/login`, `/api/auth/register`.

The static assets (`/css/**`, `/js/**`) were served from `src/main/resources/static/` for Thymeleaf. Once that directory is deleted, they no longer exist. Remove them from the security config.

Verify the SPA handlers (success, failure, logout, entry point) are still present and correct — no changes expected there.

Run `./gradlew build` to confirm.

- [ ] Task 12: Clean up `SecurityConfig` — remove Thymeleaf-era `permitAll()` paths

---

### Task 13: Delete all Thymeleaf artifacts and update build

**Delete production code:**
- `src/main/resources/templates/` (entire directory)
- `src/main/resources/static/js/` (entire directory)

**Delete test code:**
- `src/test/js/` (entire directory)
- `backend/package.json`
- `backend/vitest.config.js`
- `src/main/resources/static/js/*.test.js` (if any remain — they were colocated with production JS)

**Update `build.gradle.kts` — remove:**
```kotlin
implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
testImplementation("org.springframework.boot:spring-boot-starter-thymeleaf-test")
testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
```

Note: `spring-boot-starter-webmvc` (not `-test`) stays — the REST API still uses Spring MVC.
`spring-boot-starter-webmvc-test` (the Thymeleaf test slice) can be removed; `@WebMvcTest` for REST controllers uses `spring-boot-starter-test` which is already present.

Run `./gradlew build` then `./gradlew test` to confirm everything passes.

- [ ] Task 13: Delete templates, static JS, JS test files, package.json, vitest.config.js; remove Thymeleaf deps from build.gradle.kts; verify build and tests pass

---

### Task 14: Update CLAUDE.md files

The root `CLAUDE.md` and `backend/CLAUDE.md` contain language about the Thymeleaf migration being in progress and Thymeleaf being "temporary". After this feature, that language is stale and will confuse future agents.

**Root `CLAUDE.md`:**
- Remove the "Migration Strategy" section (or replace it with a note that migration is complete)
- Remove references to `src/main/resources/templates/`, `src/main/resources/static/js/`, `src/test/js/`, `backend/package.json`, `backend/vitest.config.js` from the cleanup-pass description
- Update the `backend/` table row: remove "Also contains Thymeleaf templates and static JS — these are temporary..."
- Update `make test-js` entry in Common Commands (remove or note it no longer applies)

**`backend/CLAUDE.md`:**
- No migration-specific language to remove; check for any references to Thymeleaf patterns

- [ ] Task 14: Update root CLAUDE.md and backend CLAUDE.md to remove Thymeleaf migration language

---

## Task order

Tasks 1–11 are largely independent and can be worked in any order. Each is a self-contained controller conversion.

Task 12 (SecurityConfig) should run after Task 1 (AuthController deletion) to avoid referencing a removed route.

Task 13 (delete artifacts + build cleanup) should run last — after all controllers are converted — so the build stays green throughout.

Task 14 can run at any point, but logically belongs at the end.

## Blockers

None.
