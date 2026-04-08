# Context: external-invoice-parser

## Status
In Progress

## Background
The invoice extraction pipeline was built with an internal two-stage approach: Tesseract OCR extracts raw text from the uploaded document, then an Ollama LLM parses the text into structured fields. Both stages exist only as empty interfaces (`OcrPort`, `InvoiceParserPort`) with skeleton adapters — neither is working. Rather than implement and maintain these in-house, the decision is to delegate all parsing to an external service and remove the internal pipeline entirely.

## User Story
As a label staff member registering a cost, I want to upload an invoice and have its fields automatically extracted, so that I don't have to manually type amounts, dates, and references.

## Dependencies
- **External invoice parsing service**: Must be running and reachable. Accepts PDF documents via multipart upload, authenticates via `X-Api-Key` header, returns structured JSON.
- **Environment**: `INVOICE_PARSER_URL` and `INVOICE_PARSER_API_KEY` must be set before the application starts.
- **Existing endpoint**: `POST /api/costs/extract` and the `ExtractionCommandApi` interface contract must remain unchanged — the cost registration UI depends on them.

## Constraints
- The external service only accepts **PDF** documents (validates content type and magic bytes). The current app also accepts PNG and JPEG — this is a narrowing of supported types and must be handled: either restrict validation to PDF only, or reject PNG/JPEG with 400 before forwarding (since the external service would reject them anyway).
- All internal extraction logic (`OcrPort`, `InvoiceParserPort`, `ExtractInvoiceDataUseCase`, Tesseract adapter, Ollama adapter) must be **deleted**, not bypassed.
- All tests for deleted classes must also be deleted. No porting needed.
- Credentials must never appear in logs or source control.
- Application must **fail fast** at startup if `INVOICE_PARSER_URL` or `INVOICE_PARSER_API_KEY` are missing.

## External Service API Contract

**Endpoint**: `POST /api/v1/extract`
**Auth**: `X-Api-Key: <key>` header — returns 401 if missing or invalid
**Request**: `multipart/form-data`, field name `file`

**Success response (200)**:
```json
{
  "invoiceDate": "2024-03-15",
  "invoiceReference": "INV-2024-001",
  "netAmount":   { "amount": "1000.00", "currency": "NOK" },
  "vatAmount":   { "amount": "250.00",  "currency": "NOK" },
  "totalAmount": { "amount": "1250.00", "currency": "NOK" }
}
```
Any field or nested object may be `null`. Response header `X-Request-Id` carries a UUID for tracing.

**Error responses** (all return `{"error": "<message>"}`):
| Status | Condition |
|--------|-----------|
| 400    | Non-PDF content type or invalid magic bytes |
| 401    | Missing/invalid X-Api-Key |
| 413    | File exceeds size limit |
| 422    | Malformed multipart request |
| 503    | Model still loading at startup |
| 500    | Unhandled processing failure |

**Graceful degradation**: Any error response or unreachable service → return `ExtractedInvoiceData.empty()` to caller with HTTP 200. Log the failure (including `X-Request-Id` if available).

## Response Mapping

| External field         | Internal field (`ExtractedInvoiceData`) |
|------------------------|-----------------------------------------|
| `invoiceDate`          | `invoiceDate`                           |
| `invoiceReference`     | `invoiceReference`                      |
| `netAmount.amount`     | `netAmount`                             |
| `vatAmount.amount`     | `vatAmount`                             |
| `totalAmount.amount`   | `grossAmount`                           |
| `netAmount.currency`   | `currency` (primary source)             |

Note: The external service returns a `vatRate` equivalent only implicitly (it can be derived). There is no direct `vatRate` field in the response — `vatRate` in `ExtractedInvoiceData` will always be `null` unless the Architect decides to calculate it.

## Prior Art
- `InvoiceExtractionController` — existing controller, must be preserved unchanged
- `ExtractionCommandApi` / `ExtractionCommandApiImpl` — existing API layer, must be preserved unchanged
- `ExtractedInvoiceData` — existing domain object, must be preserved unchanged
- `ExtractInvoiceDataUseCase` — to be deleted and replaced
- `OcrPort`, `InvoiceParserPort` — to be deleted
- `TesseractOcrAdapter`, `OllamaInvoiceParserAdapter` — to be deleted
