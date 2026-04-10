# Progress: external-invoice-parser

## Current Phase
Done

## Last Completed Task
Review Round 1 — all comments resolved

## Next Action
Merge branch to main via pull request.

## Blockers
None.

## Session Log
- 2026-04-08: Analysis complete. External service API contract confirmed. Open questions resolved: fail-fast on missing env vars, all internal extraction tests deleted, fresh branch `feature/external-invoice-parser` to be created.
- 2026-04-08: Design complete. spec.md and tasks.md written. Key decisions: PDF-only validation (drop PNG/JPEG), no new port interface, named RestClient bean, vatRate always null.
- 2026-04-08: Task 2 complete. ExternalInvoiceResponse, ExternalInvoiceParserAdapter, and ExternalInvoiceParserAdapterTest created. Added src/test/resources/application.yaml with dummy invoice parser props to fix pre-existing test failures introduced by Task 1.
- 2026-04-08: Task 3 complete. ExtractionCommandApiImpl now delegates to ExternalInvoiceParserAdapter. Made adapter public to allow cross-package injection.
- 2026-04-08: Task 4 complete. Removed extractsInvoiceDataFromImage test, added returnsBadRequestForPngDocument and returnsBadRequestForJpegDocument. Restricted ALLOWED_CONTENT_TYPES to PDF only.
- 2026-04-09: Task 5 complete. Replaced InvoiceExtractionSystemTest: removed OcrPort/InvoiceParserPort mocks, added ExternalInvoiceParserAdapter mock, added invoice.parser props to DynamicPropertySource, updated PDF test (vatRate now null), replaced image test with returnsBadRequestForPngDocument.
- 2026-04-09: Tasks 6 & 7 complete. Deleted OcrPort, InvoiceParserPort, ExtractInvoiceDataUseCase, TesseractOcrAdapter, TesseractProperties, TesseractConfig, OllamaInvoiceParserAdapter, OllamaProperties, OllamaConfig and corresponding test files. Removed ocr/ and llm/ directories. Build compiles cleanly; extraction tests all pass.
- 2026-04-09: Task 8 complete. Removed tess4j dependency from build.gradle.kts. Extraction tests all pass.
- 2026-04-09: Review Round 1 complete. comments.md written. 1 🔴 (Task 9 incomplete), 3 🟡, 2 🟢. See comments.md.
- 2026-04-09: Developer addressed all review comments. All extraction tests pass.
- 2026-04-10: Re-review complete. All comments resolved. Full test suite passes. Feature is Done.
