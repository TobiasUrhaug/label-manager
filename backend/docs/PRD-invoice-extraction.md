# Feature: Invoice Data Extraction

## Problem

When registering costs for a label or release, users must manually enter all invoice details (amounts, VAT, date, reference number) from uploaded documents. This is tedious and error-prone, especially for users who process many invoices. Automatic extraction of invoice fields would significantly reduce data entry time and improve accuracy.

## Requirements

- [ ] When a user uploads a document in the Add Cost modal, an "Extract" button appears
- [ ] Clicking "Extract" reads the document and pre-fills form fields with extracted data
- [ ] Support extraction from PDF documents
- [ ] Support extraction from image files (PNG, JPG)
- [ ] Extract the following fields:
  - [ ] Net amount
  - [ ] VAT amount
  - [ ] VAT rate (calculated from net/VAT or extracted directly)
  - [ ] Gross amount
  - [ ] Invoice date (maps to "incurred on" field)
  - [ ] Invoice number/reference (maps to "document reference" field)
  - [ ] Currency (detect from document, fallback to EUR)
- [ ] Show a loading spinner on the Extract button during processing
- [ ] Subtly highlight fields that were auto-filled
- [ ] If extraction fails or finds no data, silently leave fields empty (no error shown)

## Acceptance Criteria

- Given a user has uploaded a PDF invoice, when they click "Extract", then the form fields are populated with extracted values
- Given a user has uploaded an image of an invoice, when they click "Extract", then the form fields are populated with extracted values
- Given extraction finds an amount like "€123.45", when populating the net amount field, then it fills "123.45" and detects EUR currency
- Given extraction cannot read a field, when populating the form, then that field is left empty for manual entry
- Given extraction is in progress, when the user views the modal, then the Extract button shows a spinner and is disabled
- Given fields are auto-filled, when viewing the form, then those fields have a subtle visual indicator (highlight)
- Given extraction fails completely, when the process completes, then no error is shown and the user can fill fields manually

## Technical Decisions

- Use Tesseract OCR (via Tess4J Java library) for text extraction
- Use Ollama with a vision-capable model for intelligent field identification
- Process extraction on the backend (Java)
- Extraction is only available in the Add Cost modal (not Edit)
- EUR is the fallback currency if detection fails
- No caching of OCR text (future enhancement)

## Out of Scope

- Extraction in the Edit Cost modal
- Automatic cost type detection from invoice content
- Vendor/description extraction
- Batch invoice processing
- Training on user-specific invoice formats
- Caching extracted text for debugging (planned for future iteration)
