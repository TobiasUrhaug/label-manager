# Progress: external-invoice-parser

## Current Phase
Implementation

## Last Completed Task
Task 2: ExternalInvoiceParserAdapter and ExternalInvoiceResponse with full response mapping and graceful degradation

## Next Action
Developer: Task 3 — Wire adapter into ExtractionCommandApiImpl

## Blockers
None.

## Session Log
- 2026-04-08: Analysis complete. External service API contract confirmed. Open questions resolved: fail-fast on missing env vars, all internal extraction tests deleted, fresh branch `feature/external-invoice-parser` to be created.
- 2026-04-08: Design complete. spec.md and tasks.md written. Key decisions: PDF-only validation (drop PNG/JPEG), no new port interface, named RestClient bean, vatRate always null.
- 2026-04-08: Task 2 complete. ExternalInvoiceResponse, ExternalInvoiceParserAdapter, and ExternalInvoiceParserAdapterTest created. Added src/test/resources/application.yaml with dummy invoice parser props to fix pre-existing test failures introduced by Task 1.
