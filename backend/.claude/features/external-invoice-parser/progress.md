# Progress: external-invoice-parser

## Current Phase
Design

## Last Completed Task
Design: spec.md and tasks.md produced

## Next Action
Developer: start Task 1 — create InvoiceParserProperties and InvoiceParserConfiguration

## Blockers
None.

## Session Log
- 2026-04-08: Analysis complete. External service API contract confirmed. Open questions resolved: fail-fast on missing env vars, all internal extraction tests deleted, fresh branch `feature/external-invoice-parser` to be created.
- 2026-04-08: Design complete. spec.md and tasks.md written. Key decisions: PDF-only validation (drop PNG/JPEG), no new port interface, named RestClient bean, vatRate always null.
