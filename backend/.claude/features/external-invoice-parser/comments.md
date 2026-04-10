# Review Comments: external-invoice-parser

## Status
Done

## Review Round 1

### 🔴 Must Fix

- [x] **[tasks.md:274]** Task 9 is unchecked and incomplete. The `.env.example` exists at `backend/.env.example` but the task specifies it must be created at the **repo root** (`/Users/tobiasurhaug/code/label-manager/.env.example`). The content is correct — it just needs to be at the right location. Per the Definition of Done, all tasks must be checked off. **Resolved** — `.env.example` created at repo root; task checked off.

### 🟡 Should Fix

- [x] **[application.yaml:37]** Default values for `INVOICE_PARSER_URL` and `INVOICE_PARSER_API_KEY` in `application.yaml` undermine the fail-fast requirement. With `${INVOICE_PARSER_API_KEY:dev-secret-key}`, if the env var is not set in production, the app silently starts using `dev-secret-key` and the `@NotBlank` check passes. The requirement says "The application fails to start with a clear error message if either variable is missing." Dev defaults are convenient, but they bypass the startup validation entirely. Options: (a) remove the defaults from `application.yaml` and rely on a local `.env` file for dev, or (b) document clearly that the defaults are intentional dev-only values and acknowledge this as an accepted deviation from the requirement. **Resolved** — Defaults removed; `application.yaml` now uses bare `${INVOICE_PARSER_URL}` and `${INVOICE_PARSER_API_KEY}`. Missing env var causes startup failure via Spring placeholder resolution. `src/test/resources/application.yaml` provides test values so no tests regressed.

- [x] **[ExternalInvoiceParserAdapter.java:30]** The `contentType` parameter is accepted in `extract(InputStream content, String contentType)` but never used — `postToExternalParser` only takes the `InputStream`. Since the external service only accepts PDF (validated upstream), this is harmless in practice, but the parameter is misleading. Either remove it from the adapter's method signature (callers already know what type they're sending) or use it to explicitly set the content type on the `InputStreamResource` part so the external service receives a proper `Content-Type` on the file part instead of `application/octet-stream`. **Resolved** — `contentType` now passed to `postToExternalParser`; file part wrapped in `HttpEntity` with `Content-Type` header set accordingly.

- [x] **[ExternalInvoiceParserAdapter.java:32]** If the external service returns a 200 with an empty body, `restClient.retrieve().body(ExternalInvoiceResponse.class)` returns `null`. `mapToExtractedInvoiceData(null)` then throws a `NullPointerException` on `response.netAmount()`. The catch-all handles it and returns `ExtractedInvoiceData.empty()`, but the log message will be a confusing NPE stack trace rather than a meaningful warning. Add an explicit null check: `if (response == null) { log.warn("External invoice parser returned empty body"); return ExtractedInvoiceData.empty(); }`. **Resolved** — Explicit null check added with clear log message.

### 🟢 Suggestions

- [x] **[InvoiceExtractionControllerTest.java:51]** The `extractsInvoiceDataFromPdf()` test mocks the command API to return `vatRate = new BigDecimal("21")`. Since the real adapter always returns `vatRate = null` (by design — the external service has no vatRate field), this mock is inconsistent with actual runtime behavior. The test still exercises the controller correctly, but setting `vatRate = null` in the mock would better reflect reality and match what the system test does. **Resolved** — Mock updated to `vatRate = null`; assertion changed to `.isEmpty()`.

- [x] **[InvoiceParserConfiguration.java:13]** The `X-Api-Key` header is set via `defaultHeader` on the `RestClient`, which means it is included in every request made by this bean. At Spring `DEBUG` log level (`org.springframework.web.client`), request headers including `X-Api-Key` can appear in logs. This is low risk in practice (DEBUG is not enabled by default in production), but worth noting in a comment or in ops runbooks: do not enable `DEBUG` for `org.springframework.web.client` in production environments. **Resolved** — Comment added to the bean method.

### NFR Checks

- [x] **Performance**: No blocking I/O concerns. The external HTTP call is synchronous and on-demand (one call per user upload). `RestClient` uses the underlying `RestTemplate` with default connection pooling. Acceptable for the expected load.
- [x] **Security**: API key never appears in log output. YAML defaults removed — fail-fast now works correctly. DEBUG logging risk documented in configuration class.
- [x] **Correctness of deletions**: All internal pipeline files (`OcrPort`, `InvoiceParserPort`, `ExtractInvoiceDataUseCase`, `TesseractOcrAdapter`, `TesseractProperties`, `OllamaInvoiceParserAdapter`, `OllamaProperties`) and their directories (`infrastructure/ocr/`, `infrastructure/llm/`) are confirmed deleted. No lingering references found.
- [x] **Architecture compliance**: Infrastructure adapter placed in `finance/extraction/infrastructure/` (module-specific, not cross-cutting). `ExtractionCommandApiImpl` is package-private in `application/`. `ExternalInvoiceParserAdapter` is `public` (required for cross-package injection and `@MockitoBean`) — consistent with the architecture's rule that infrastructure classes may be public.
- [x] **Test coverage**: Unit tests for the adapter cover all 5 required cases (full mapping, partial nulls, HTTP 400, HTTP 503, network error). Properties tests cover both missing-url and missing-api-key failure paths plus the happy path. Controller tests cover PDF (200), PNG (400), JPEG (400), unsupported type (400), no document (400), and graceful empty response. System test validates end-to-end wiring with real Spring context.

---

## Developer Responses (Round 1)

**🔴 Task 9**: Created `.env.example` at repo root (`/Users/tobiasurhaug/code/label-manager/.env.example`) with all required env vars documented. Task checked off in `tasks.md`.

**🟡 application.yaml defaults**: Removed hardcoded defaults from `application.yaml` — changed to `${INVOICE_PARSER_URL}` and `${INVOICE_PARSER_API_KEY}` (no fallback). `@NotBlank` now enforces fail-fast if either env var is absent. Tests are unaffected — `src/test/resources/application.yaml` already provides test values.

**🟡 contentType unused**: Now passed to `postToExternalParser`. The file part is wrapped in an `HttpEntity` with `Content-Type` set from the `contentType` param, so the external service receives the correct MIME type on the multipart part.

**🟡 Null response guard**: Added explicit null check after `postToExternalParser` returns. Logs `"External invoice parser returned empty body"` and returns `ExtractedInvoiceData.empty()` rather than relying on the catch-all to handle an NPE.

**🟢 Controller test vatRate**: Changed mock to return `vatRate = null` and updated assertion from `.value(21)` to `.isEmpty()`.

**🟢 InvoiceParserConfiguration comment**: Added comment warning against enabling `DEBUG` logging for `org.springframework.web.client` in production.

---

## Review Round 2 (if needed)
<!-- Reviewer adds new comments or follow-ups here -->
