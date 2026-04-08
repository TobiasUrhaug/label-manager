# Progress: external-invoice-parser

## Current Phase
Implementation

## Last Completed Task
Task 1: InvoiceParserProperties and InvoiceParserConfiguration with fail-fast startup validation

## Next Action
Developer: Task 2 — ExternalInvoiceParserAdapter with HTTP call and response mapping

## Blockers
None.

## Session Log
- 2026-04-08: Analysis complete. External service API contract confirmed. Open questions resolved: fail-fast on missing env vars, all internal extraction tests deleted, fresh branch `feature/external-invoice-parser` to be created.
- 2026-04-08: Design complete. spec.md and tasks.md written. Key decisions: PDF-only validation (drop PNG/JPEG), no new port interface, named RestClient bean, vatRate always null.
