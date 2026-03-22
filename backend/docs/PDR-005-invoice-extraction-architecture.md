# PDR-005: Invoice Data Extraction Architecture

## Status

Proposed

## Context

Users need to manually enter invoice details when registering costs. We want to automatically extract fields from uploaded invoice documents to reduce data entry. The solution must be open source and free, suitable for a small-scale application with relatively simple invoices.

## Decision

Use a **hybrid OCR + LLM approach** for invoice extraction:

1. **Tesseract OCR** (via Tess4J) extracts raw text from documents
2. **Ollama** with a vision-capable model (e.g., LLaVA) identifies and extracts structured fields from the text

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Frontend                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Add Cost Modal                                       │    │
│  │  ┌──────────────┐    ┌────────────────────────────┐ │    │
│  │  │ Upload File  │───▶│ "Extract" button appears   │ │    │
│  │  └──────────────┘    └────────────────────────────┘ │    │
│  │                              │                       │    │
│  │                              ▼                       │    │
│  │                      POST /api/extract               │    │
│  │                              │                       │    │
│  │                              ▼                       │    │
│  │                    ┌─────────────────────┐           │    │
│  │                    │ Auto-fill fields    │           │    │
│  │                    │ (with highlight)    │           │    │
│  │                    └─────────────────────┘           │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        Backend                               │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ InvoiceExtractionController                          │    │
│  │   POST /api/costs/extract                            │    │
│  └───────────────────────┬─────────────────────────────┘    │
│                          │                                   │
│                          ▼                                   │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ InvoiceExtractionUseCase                             │    │
│  │   - Orchestrates extraction flow                     │    │
│  │   - Returns ExtractedInvoiceData                     │    │
│  └───────────────────────┬─────────────────────────────┘    │
│                          │                                   │
│           ┌──────────────┴──────────────┐                   │
│           ▼                             ▼                   │
│  ┌─────────────────────┐    ┌─────────────────────────┐    │
│  │ OcrPort             │    │ InvoiceParserPort       │    │
│  │ (Tesseract/Tess4J)  │    │ (Ollama LLM)            │    │
│  │                     │    │                         │    │
│  │ - extractText(doc)  │    │ - parseInvoice(text)    │    │
│  │ - Supports PDF,     │    │ - Returns structured    │    │
│  │   PNG, JPG          │    │   field data            │    │
│  └─────────────────────┘    └─────────────────────────┘    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Docker Environment                        │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ App Container                                        │    │
│  │  - Spring Boot application                           │    │
│  │  - Tesseract OCR (installed via apt)                 │    │
│  │  - Tess4J calls Tesseract directly                   │    │
│  └─────────────────────────────────────────────────────┘    │
│                          │                                   │
│                          │ HTTP (port 11434)                 │
│                          ▼                                   │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Ollama Container                                     │    │
│  │  - ollama/ollama image                               │    │
│  │  - Model: LLaVA or similar                           │    │
│  │  - Persistent volume for models                      │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Package Structure

```
org.omt.labelmanager/
└── finance/
    ├── api/
    │   └── InvoiceExtractionController.java
    ├── application/
    │   ├── InvoiceExtractionUseCase.java
    │   └── ExtractedInvoiceData.java (record)
    ├── domain/
    │   ├── OcrPort.java (interface)
    │   └── InvoiceParserPort.java (interface)
    └── infrastructure/
        ├── TesseractOcrAdapter.java
        └── OllamaInvoiceParserAdapter.java
```

### Extracted Data Structure

```java
public record ExtractedInvoiceData(
    BigDecimal netAmount,
    BigDecimal vatAmount,
    BigDecimal vatRate,
    BigDecimal grossAmount,
    LocalDate invoiceDate,
    String invoiceReference,
    String currency
) {}
```

### API Contract

```
POST /api/costs/extract
Content-Type: multipart/form-data

Request:
  - document: MultipartFile (PDF, PNG, or JPG)

Response (200 OK):
{
  "netAmount": 100.00,
  "vatAmount": 21.00,
  "vatRate": 21.00,
  "grossAmount": 121.00,
  "invoiceDate": "2024-01-15",
  "invoiceReference": "INV-2024-001",
  "currency": "EUR"
}

Response (200 OK - partial extraction):
{
  "netAmount": 100.00,
  "vatAmount": null,
  "vatRate": null,
  "grossAmount": null,
  "invoiceDate": "2024-01-15",
  "invoiceReference": null,
  "currency": "EUR"
}
```

All fields are nullable. The endpoint always returns 200 OK with whatever could be extracted.

## Rationale

### Why Hybrid (Tesseract + LLM)?

- **Tesseract alone** requires complex regex patterns per invoice format
- **LLM alone** may hallucinate or struggle with poor image quality
- **Hybrid approach**: Tesseract provides reliable text extraction, LLM provides intelligent field identification

### Why Ollama?

- Open source and free (runs locally)
- Supports vision models like LLaVA
- Easy to set up and integrate
- Can be swapped for cloud LLM later if needed

### Why Tess4J?

- Mature Java wrapper for Tesseract
- Well-documented and actively maintained
- Handles PDF and image formats
- Tesseract easily bundled in Docker image via apt-get

### Why Backend Processing?

- More control over processing pipeline
- Can use Java libraries directly (Tess4J)
- Keeps extraction logic centralized
- No need to ship large JS libraries to browser

### Why Manual "Extract" Trigger?

- User controls when to invoke extraction (avoids surprise API calls)
- Allows upload without extraction if user prefers manual entry
- Clearer mental model for users

## Alternatives Considered

### Cloud AI Services (AWS Textract, Google Document AI)

- **Pros**: Higher accuracy, specialized invoice models
- **Cons**: Pay-per-use cost, vendor lock-in, requires cloud account
- **Rejected**: User requirement for free/open source solution

### Browser-Based OCR (Tesseract.js)

- **Pros**: No server processing, instant feedback
- **Cons**: Large JS bundle, limited processing power, no LLM integration
- **Rejected**: Limited capabilities for intelligent field extraction

### Tesseract-Only with Regex

- **Pros**: Simpler architecture, no LLM dependency
- **Cons**: Brittle regex patterns, poor handling of varied invoice formats
- **Rejected**: Too fragile for real-world invoice variety

## Consequences

### Positive

- Zero ongoing cost for extraction
- No external cloud service dependencies
- User retains control over when extraction happens
- Graceful degradation (manual entry always available)
- **Containerized deployment**: Consistent environment across dev/staging/prod
- **Tesseract bundled**: No separate system installation needed in production

### Negative

- Larger Docker image (Tesseract adds ~50MB)
- Requires Ollama container running (additional service to manage)
- Extraction quality depends on document quality and LLM model
- Ollama container needs GPU for best performance (CPU works but slower)
- Must pull LLM model after first container start

### Future Considerations

- **Debug logging**: Add optional logging of OCR text for troubleshooting
- **Model tuning**: Test different Ollama models for best accuracy
- **Confidence scores**: Return confidence levels for extracted fields
- **Cloud option**: Add configurable endpoint to use cloud LLM if needed

## Dependencies

### Docker Setup

Tesseract is bundled in the application container. Ollama runs as a separate container.

**Dockerfile** (add Tesseract to existing app image):

```dockerfile
FROM eclipse-temurin:25-jdk

# Install Tesseract OCR with English language data
RUN apt-get update && \
    apt-get install -y tesseract-ocr tesseract-ocr-eng && \
    rm -rf /var/lib/apt/lists/*

# ... rest of existing Dockerfile
```

**docker-compose.yml** (add Ollama service):

```yaml
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      INVOICE_EXTRACTION_OLLAMA_BASE_URL: http://ollama:11434
    depends_on:
      - ollama

  ollama:
    image: ollama/ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama

volumes:
  ollama_data:
```

**Initial model setup** (run once after first `docker compose up`):

```bash
docker compose exec ollama ollama pull llava
```

### Java Dependencies

```gradle
implementation 'net.sourceforge.tess4j:tess4j:5.x.x'
```

### Configuration

```yaml
# application.yml
invoice-extraction:
  ollama:
    base-url: ${INVOICE_EXTRACTION_OLLAMA_BASE_URL:http://localhost:11434}
    model: llava
  tesseract:
    data-path: /usr/share/tesseract-ocr/5/tessdata  # Debian/Ubuntu path in container
  default-currency: EUR
```

### Local Development (without Docker)

For local development without Docker, install dependencies directly:

```bash
# macOS
brew install tesseract
brew install ollama
ollama serve &
ollama pull llava

# Then configure application.yml with local paths:
# tesseract.data-path: /opt/homebrew/share/tessdata
```
