# Spec: external-invoice-parser

## Status
In Progress

## Approach

Replace the dead two-stage internal pipeline (`OcrPort → TesseractOcrAdapter → InvoiceParserPort → OllamaInvoiceParserAdapter → ExtractInvoiceDataUseCase`) with a single HTTP call to an external parsing service. The existing public API contract (`POST /api/costs/extract`, `ExtractionCommandApi` interface, `ExtractedInvoiceData` domain record) is preserved unchanged. All internal extraction code is deleted — none is ported or bypassed.

The external adapter is placed in `finance/extraction/infrastructure/` (module-specific, not cross-cutting) and injected directly into `ExtractionCommandApiImpl`. No new port interface is introduced — there is only one implementation and no planned swappability.

## Key Decisions

- **PDF-only validation**: Changed from PDF/PNG/JPEG to PDF only.
  - Why: The external service rejects non-PDF with HTTP 400. Forwarding PNG/JPEG files that we know will fail, then silently returning empty data, gives users a misleading success. A clear 400 is more honest.
  - Alternatives considered: (a) keep PNG/JPEG, forward and gracefully degrade — rejected because it's silently misleading; (b) keep PNG/JPEG in validation but reject before forwarding — adds dead code with no purpose.

- **No new port interface**: `ExtractionCommandApiImpl` directly depends on `ExternalInvoiceParserAdapter`.
  - Why: A port interface adds a layer of indirection that only pays off when multiple implementations exist or swapping is planned. Neither applies here.
  - Alternatives considered: Introduce `ExternalParserPort` — rejected as unnecessary abstraction.

- **`vatRate` field stays null**: The external service has no direct `vatRate` field. Deriving it from `netAmount`/`vatAmount` adds complexity for an optional field. Leave as null.

- **Named RestClient bean**: A dedicated `@Configuration` class creates a `RestClient` bean with the base URL pre-configured from `InvoiceParserProperties`. The adapter injects this qualified bean, keeping the URL concern out of the adapter.

## Files to Create

| File | Action | Purpose |
|------|--------|---------|
| `finance/extraction/infrastructure/InvoiceParserProperties.java` | Create | `@ConfigurationProperties("invoice.parser")` record — binds `INVOICE_PARSER_URL` → `url`, `INVOICE_PARSER_API_KEY` → `api-key`. Both `@NotBlank` for fail-fast startup validation. |
| `finance/extraction/infrastructure/InvoiceParserConfiguration.java` | Create | `@Configuration` class that `@EnableConfigurationProperties(InvoiceParserProperties.class)` and creates the named `RestClient` bean. |
| `finance/extraction/infrastructure/ExternalInvoiceParserAdapter.java` | Create | `@Component` — POSTs document to external service, maps response to `ExtractedInvoiceData`. On any failure: log and return `ExtractedInvoiceData.empty()`. |
| `finance/extraction/infrastructure/ExternalInvoiceResponse.java` | Create | Package-private record — internal DTO for deserializing external JSON response. |
| `.env.example` | Create | Documents `INVOICE_PARSER_URL` and `INVOICE_PARSER_API_KEY`. |

## Files to Modify

| File | Action | Purpose |
|------|--------|---------|
| `finance/extraction/application/ExtractionCommandApiImpl.java` | Modify | Replace `ExtractInvoiceDataUseCase` dependency with `ExternalInvoiceParserAdapter`. |
| `finance/extraction/api/InvoiceExtractionController.java` | Modify | Restrict `ALLOWED_CONTENT_TYPES` to `application/pdf` only. |
| `build.gradle.kts` | Modify | Remove `net.sourceforge.tess4j:tess4j` and any Ollama-related dependencies. |

## Files to Delete (production)

| File | Reason |
|------|--------|
| `finance/extraction/infrastructure/OcrPort.java` | Two-stage pipeline removed |
| `finance/extraction/infrastructure/InvoiceParserPort.java` | Two-stage pipeline removed |
| `finance/extraction/application/ExtractInvoiceDataUseCase.java` | Replaced by direct adapter delegation |
| `infrastructure/ocr/TesseractOcrAdapter.java` | OCR removed |
| `infrastructure/ocr/TesseractProperties.java` | OCR removed |
| `infrastructure/llm/OllamaInvoiceParserAdapter.java` | LLM parser removed |
| `infrastructure/llm/OllamaProperties.java` | LLM parser removed |

Delete `infrastructure/ocr/` and `infrastructure/llm/` directories if empty after removal.

## Files to Delete (tests)

| File | Reason |
|------|--------|
| `finance/extraction/application/ExtractInvoiceDataUseCaseTest.java` | Class deleted |
| `infrastructure/ocr/TesseractOcrAdapterTest.java` | Class deleted |
| `infrastructure/llm/OllamaInvoiceParserAdapterTest.java` | Class deleted |
| `finance/extraction/InvoiceExtractionSystemTest.java` | Mocks deleted ports — must be replaced |

## Files to Create (tests)

| File | Purpose |
|------|---------|
| `finance/extraction/infrastructure/ExternalInvoiceParserAdapterTest.java` | Unit test using `MockRestServiceServer`: happy path, partial nulls, HTTP errors, network failure |
| `finance/extraction/InvoiceExtractionSystemTest.java` | Replacement system test — no OcrPort/InvoiceParserPort; mocks `ExternalInvoiceParserAdapter` |

## Files to Modify (tests)

| File | Change |
|------|--------|
| `finance/extraction/api/InvoiceExtractionControllerTest.java` | Remove `extractsInvoiceDataFromImage()` (PNG now 400); add `returnsBadRequestForPngDocument()` and `returnsBadRequestForJpegDocument()` |

## Data Models / Interfaces

### InvoiceParserProperties (new)
```java
@ConfigurationProperties("invoice.parser")
@Validated
record InvoiceParserProperties(
    @NotBlank String url,
    @NotBlank String apiKey
) {}
```

Spring Boot relaxed binding maps:
- `INVOICE_PARSER_URL` → `invoice.parser.url`
- `INVOICE_PARSER_API_KEY` → `invoice.parser.api-key`

### ExternalInvoiceResponse (new, package-private)
```java
record ExternalInvoiceResponse(
    String invoiceDate,        // nullable ISO date string, e.g. "2024-03-15"
    String invoiceReference,   // nullable
    MoneyAmount netAmount,     // nullable
    MoneyAmount vatAmount,     // nullable
    MoneyAmount totalAmount,   // nullable
) {
    record MoneyAmount(
        String amount,    // decimal string, e.g. "1000.00"
        String currency   // ISO code, e.g. "NOK"
    ) {}
}
```

### Response Mapping

| External field         | `ExtractedInvoiceData` field | Notes |
|------------------------|------------------------------|-------|
| `invoiceDate`          | `invoiceDate`                | Parse as `LocalDate` (ISO format); null if absent |
| `invoiceReference`     | `invoiceReference`           | Pass through; null if absent |
| `netAmount.amount`     | `netAmount`                  | Parse as `BigDecimal`; null if `netAmount` is null |
| `vatAmount.amount`     | `vatAmount`                  | Parse as `BigDecimal`; null if `vatAmount` is null |
| `totalAmount.amount`   | `grossAmount`                | Parse as `BigDecimal`; null if `totalAmount` is null |
| `netAmount.currency`   | `currency`                   | Primary source; null if `netAmount` is null |
| *(none)*               | `vatRate`                    | Always null — external service has no `vatRate` |

### ExtractionCommandApi (unchanged)
```java
public interface ExtractionCommandApi {
    ExtractedInvoiceData extract(InputStream content, String contentType);
}
```

### External HTTP Request

```
POST {INVOICE_PARSER_URL}/api/v1/extract
Content-Type: multipart/form-data
X-Api-Key: {INVOICE_PARSER_API_KEY}

file=<document bytes>
```

## Integration Points

| System/Module | How it's used | Who owns it |
|---------------|---------------|-------------|
| External invoice parsing service | HTTP POST of document bytes; receives structured JSON response | External (third party) |
| `ExtractionCommandApi` | Public interface preserved — controller depends on it | `finance/extraction/api/` |
| `ExtractedInvoiceData` | Domain record preserved — response type of the API | `finance/extraction/domain/` |
| Spring `RestClient` | HTTP client for the external call | Spring Framework |
| Spring Boot `@ConfigurationProperties` + `@Validated` | Fail-fast startup binding of env vars | Spring Boot |
| `MockRestServiceServer` | Test HTTP layer without network | Spring Test |

## Error Handling

| Condition | Application behaviour |
|-----------|----------------------|
| External service returns 4xx or 5xx | Log warning (include `X-Request-Id` response header if present), return `ExtractedInvoiceData.empty()` |
| External service unreachable (network error) | Log warning with exception, return `ExtractedInvoiceData.empty()` |
| Response body missing or malformed JSON | Log warning, return `ExtractedInvoiceData.empty()` |
| `INVOICE_PARSER_URL` or `INVOICE_PARSER_API_KEY` missing at startup | Application refuses to start with clear `BindValidationException` |

`INVOICE_PARSER_API_KEY` must **never** appear in log output.

## Assumptions

- Spring Boot 4.0 / Spring Framework 7 `RestClient` is available (it is — already used by `OllamaInvoiceParserAdapter`).
- `MockRestServiceServer.bindTo(RestClient)` is available in the test classpath — it is, as `spring-boot-starter-test` includes it.
- No new test dependencies are required.
- The external service base URL does not include a trailing path segment — the adapter appends `/api/v1/extract`.

## Risks

- External service changes its response schema → field mapping breaks silently (fields become null). Mitigation: log the raw response on failure, integration smoke test after deployment.
- `INVOICE_PARSER_API_KEY` accidentally logged by Spring or library internals → mitigated by not logging properties directly and treating the key as opaque.
