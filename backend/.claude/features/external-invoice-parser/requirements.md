# Requirements: External Invoice Parser

## Overview
- **Business Problem**: The current invoice parsing pipeline is unimplemented — the internal OCR and field-parsing components exist as empty interfaces with no working implementation. Label staff cannot auto-populate cost fields from uploaded invoices.
- **Target Users**: Label staff who register costs and upload invoice documents to have fields pre-filled.
- **Business Value**: Unblocks the invoice extraction feature immediately by delegating all parsing work to a dedicated external service, rather than building and maintaining that logic in-house.

## Business Objectives
- **Primary Goal**: When a user uploads an invoice document, the application forwards it to an external parsing service and returns the extracted fields — with no parsing logic living inside this application.
- **Success Criteria**:
  - Uploading a supported document (PDF, PNG, JPEG) returns extracted invoice fields to the user.
  - No OCR, AI, or field-parsing logic exists inside this application.
  - The external service URL and API key are read from environment variables; nothing is hard-coded.

## Scope

### In Scope
- Forwarding uploaded invoice documents to an external parsing service.
- Returning the external service's extracted fields back to the caller via the existing endpoint (`POST /api/costs/extract`).
- Reading the external service URL and API key from environment variables (`INVOICE_PARSER_URL`, `INVOICE_PARSER_API_KEY`).
- Removing the internal two-stage extraction pipeline (`OcrPort`, `InvoiceParserPort`, `ExtractInvoiceDataUseCase`) since this application no longer does any parsing.
- Preserving the existing validated document types (PDF, PNG, JPEG) and the existing `ExtractionCommandApi` interface contract unchanged.

### Out of Scope
- Any parsing, OCR, or AI logic inside this application.
- Changes to the cost registration UI or the cost controller.
- Managing, hosting, or modifying the external parsing service.

## User Roles
- **Label Staff (cost registrar)**: Uploads invoice documents through the cost registration form. Expects extracted fields to be returned so they can be pre-filled without manual data entry.
- **System Administrator**: Sets `INVOICE_PARSER_URL` and `INVOICE_PARSER_API_KEY` in the deployment environment.

## Core Entities

### ExtractedInvoiceData
**Business Description**: The structured data parsed out of an invoice document. Used to pre-fill cost registration fields.

**Key Attributes**:
- `netAmount` (optional) — Invoice net amount before VAT
- `vatAmount` (optional) — VAT amount
- `vatRate` (optional) — VAT percentage rate
- `grossAmount` (optional) — Total invoice amount including VAT
- `invoiceDate` (optional) — Date printed on the invoice
- `invoiceReference` (optional) — Invoice number or reference string
- `currency` (optional) — ISO currency code (e.g. NOK, EUR, USD)

All fields are nullable — partial extraction (some fields populated, others not) is acceptable.

### External Service Configuration
**Business Description**: The connection details needed to reach the external parsing service.

**Key Attributes**:
- `INVOICE_PARSER_URL` (required) — Base URL of the external parsing service, read from environment.
- `INVOICE_PARSER_API_KEY` (required) — API key for authenticating requests, read from environment.

## Business Rules

### Rule: No Internal Extraction
**Description**: This application must not perform any document parsing itself.

**When**: A document is submitted for invoice extraction.
**Then**: The document is forwarded to the external service and its response is returned — no text analysis or field parsing happens inside this app.
**Example**: A user uploads `invoice-march.pdf`. The app sends it to the external service and returns whatever fields come back. It does not attempt any reading of the file contents itself.

### Rule: Preserve Accepted Document Types
**Description**: Only PDF, PNG, and JPEG documents are accepted. This validation happens before calling the external service.

**When**: A file is uploaded.
**Then**: If the content type is not in the allowed set, return a 400 Bad Request without calling the external service.

### Rule: Graceful Degradation on External Service Failure
**Description**: If the external service is unavailable or returns an error, the application must not crash. It returns an empty result instead.

**When**: The external service returns an error or cannot be reached.
**Then**: Return an empty `ExtractedInvoiceData` (all fields null) to the caller. The failure is logged for observability.
**Example**: External service returns HTTP 503. The user receives an empty response and can fill in the cost fields manually.

### Rule: Credentials from Environment Only
**Description**: The service URL and API key must never be hard-coded in source code or committed to version control.

**When**: The application starts.
**Then**: It reads `INVOICE_PARSER_URL` and `INVOICE_PARSER_API_KEY` from the environment. If either is missing, the application should raise a clear startup error.

## User Stories

### US-1: Extract invoice data via external service
**As a** label staff member registering a cost
**I want to** upload an invoice document and have its fields automatically extracted
**So that** I don't have to manually type amounts, dates, and references from the invoice

**Acceptance Criteria**:
- [ ] Uploading a valid PDF, PNG, or JPEG to `POST /api/costs/extract` results in a call to the configured external service.
- [ ] The extracted fields from the external service are returned as the response.
- [ ] Fields the external service could not extract are returned as `null`.
- [ ] Uploading an unsupported file type still returns HTTP 400 without calling the external service.
- [ ] No extraction code (OCR, AI, regex parsing) exists inside this application.

**Example Scenario**: Staff uploads `receipt.pdf`. The app forwards it to the configured external service. The service returns net amount, VAT rate, gross amount, and currency. The app returns these to the frontend for pre-filling the cost form.

### US-2: Graceful empty response when external service is unavailable
**As a** label staff member
**I want to** receive a graceful empty response when the external service is down
**So that** I can still register the cost manually without the app crashing

**Acceptance Criteria**:
- [ ] If the external service is unreachable or returns an error, the endpoint returns HTTP 200 with all fields as `null`.
- [ ] The failure is logged so the issue can be investigated.

**Example Scenario**: External service is down for maintenance. Staff uploads an invoice. The app logs a warning and returns an empty result. Staff enters the cost fields manually.

### US-3: Configure external service via environment variables
**As a** system administrator
**I want to** supply the external service URL and API key via environment variables
**So that** credentials are not stored in source code and can differ between environments (local, staging, production)

**Acceptance Criteria**:
- [ ] `INVOICE_PARSER_URL` and `INVOICE_PARSER_API_KEY` are read from the environment at startup.
- [ ] The application fails to start with a clear error message if either variable is missing.
- [ ] Both variables are documented in `.env.example`.

## Workflows

### Invoice Extraction via External Service

**Trigger**: Label staff uploads a document to `POST /api/costs/extract`.

**Steps**:
1. Application receives the multipart file upload.
2. Application validates the document's content type.
   - If unsupported → return HTTP 400. Stop.
   - If supported → continue.
3. Application forwards the document to the external service using the configured URL and API key.
4. External service processes the document and returns extracted invoice fields.
5. Application maps the response to `ExtractedInvoiceData` and returns it with HTTP 200.

**Alternative Paths**:
- External service is unreachable or returns an error → log the failure, return `ExtractedInvoiceData` with all fields `null`, HTTP 200.
- Uploaded file is empty or null → return HTTP 400 (existing behaviour, unchanged).

## Integration Requirements

### External Invoice Parsing Service
**Business Purpose**: Performs all invoice text extraction and field parsing so this application does not need to.

**Data Needed**:
- **This app → External service**: Raw document bytes and document content type (MIME type), plus the API key for authentication.
- **External service → This app**: Extracted invoice fields — net amount, VAT amount, VAT rate, gross amount, invoice date, invoice reference, currency. Fields that could not be extracted may be omitted or null.

**Frequency**: On-demand — one call per user document upload.

**Authentication**: API key supplied with each request. Exact mechanism (header name, format) to be confirmed against the external service's API specification.

**Business Impact of Failure**: Invoice fields cannot be auto-populated. Staff must enter them manually. Cost registration itself is not blocked.

## User Interface Requirements
**Key Interactions**: No UI changes required. The extraction endpoint is already called from the existing cost registration page.

**Required Information Display**: Same as today — extracted fields are returned as JSON and used to pre-fill cost form inputs.

## Reporting & Analytics
No new reporting requirements for this feature.

## Access Control & Security
**Who Can Do What**:
- Authenticated label staff: Can call `POST /api/costs/extract` — same as today, no change.

**Data Sensitivity**: `INVOICE_PARSER_API_KEY` is a secret credential. It must never appear in application logs or be committed to source control.

**Audit Requirements**: Failures when calling the external service should be logged with enough context to diagnose the issue (timestamp, document name, HTTP status or error received).

## Business Constraints & Dependencies
**Constraints**:
- The public API contract (`POST /api/costs/extract`, the `ExtractionCommandApi` interface, and the `ExtractedInvoiceData` response shape) must not change — other parts of the codebase and the UI depend on it.
- All internal extraction logic must be removed, not just bypassed.

**Dependencies**:
- The external invoice parsing service must be operational and reachable from the deployment environment.
- `INVOICE_PARSER_URL` and `INVOICE_PARSER_API_KEY` must be set in the environment before the application starts.

**Risks**:
- If the external service changes its response format, the field mapping must be updated in this application.
- If the external service is unavailable, extraction silently degrades to an empty result (accepted trade-off per the graceful degradation rule).

## Success Metrics
**How to Measure Success**:
- **End-to-end extraction works**: Uploading a real invoice after deployment returns populated fields (confirmed by a manual smoke test).
- **No internal extraction code remains**: `OcrPort`, `InvoiceParserPort`, and `ExtractInvoiceDataUseCase` are deleted from the codebase.
- **No regressions**: All existing tests in the cost registration flow continue to pass.

## Resolved Questions

- [x] **External service API contract** — `POST /api/v1/extract`, multipart field `file`, auth via `X-Api-Key` header, JSON response with `invoiceDate`, `invoiceReference`, `netAmount`, `vatAmount`, `totalAmount` (each a `{amount, currency}` object or null). Full contract in `context.md`.
- [x] **Missing env vars at startup** — Application must **fail fast**: refuse to start with a clear error if `INVOICE_PARSER_URL` or `INVOICE_PARSER_API_KEY` are absent.
- [x] **Existing tests** — All tests for deleted classes (`ExtractInvoiceDataUseCase`, `OcrPort`, `InvoiceParserPort`, Tesseract and Ollama adapters) are to be **deleted**. No porting needed.

## Assumptions
- The external service accepts the document as a multipart file upload — **confirmed**.
- The API key is passed as `X-Api-Key` request header — **confirmed**.
- Partial results (some fields null) are acceptable and expected — **confirmed**.
- The external service only accepts PDF (not PNG/JPEG). The Architect must decide whether to keep PNG/JPEG validation in this app (rejecting before forwarding) or drop those types from the accepted set entirely.
- `vatRate` in `ExtractedInvoiceData` will be `null` — the external service has no direct `vatRate` field. The Architect may optionally calculate it from `netAmount` and `vatAmount` if both are present.
