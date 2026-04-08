# Tasks: external-invoice-parser

## Status
In Progress

## Tasks

Follow Red-Green-Refactor: write the failing test first, then make it pass, then clean up.

---

### Task 1: InvoiceParserProperties — configuration binding with fail-fast validation

**What**: Create `InvoiceParserProperties.java` in `finance/extraction/infrastructure/`.

```java
@ConfigurationProperties("invoice.parser")
@Validated
record InvoiceParserProperties(
    @NotBlank String url,
    @NotBlank String apiKey
) {}
```

Spring relaxed binding maps `INVOICE_PARSER_URL` → `url` and `INVOICE_PARSER_API_KEY` → `api-key`.

Create `InvoiceParserConfiguration.java` in same package to register the properties and the named RestClient bean:

```java
@Configuration
@EnableConfigurationProperties(InvoiceParserProperties.class)
class InvoiceParserConfiguration {
    @Bean("invoiceParserRestClient")
    RestClient invoiceParserRestClient(InvoiceParserProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.url())
                .defaultHeader("X-Api-Key", properties.apiKey())
                .build();
    }
}
```

**Test**: Write a `@SpringBootTest` slice test that verifies the application context fails to load when `invoice.parser.url` is missing. Use `@TestPropertySource` to remove the property.

File: `finance/extraction/infrastructure/InvoiceParserPropertiesTest.java`

- [ ] Task 1: Create `InvoiceParserProperties` and `InvoiceParserConfiguration` with fail-fast startup validation (test: context fails without env vars)

---

### Task 2: ExternalInvoiceParserAdapter — HTTP call with response mapping

**What**: Create the adapter in `finance/extraction/infrastructure/`. Also create the internal response DTO.

`ExternalInvoiceResponse.java` (package-private record):
```java
record ExternalInvoiceResponse(
    String invoiceDate,
    String invoiceReference,
    MoneyAmount netAmount,
    MoneyAmount vatAmount,
    MoneyAmount totalAmount
) {
    record MoneyAmount(String amount, String currency) {}
}
```

`ExternalInvoiceParserAdapter.java`:
```java
@Component
class ExternalInvoiceParserAdapter {

    private static final Logger log = LoggerFactory.getLogger(ExternalInvoiceParserAdapter.class);

    private final RestClient restClient;

    ExternalInvoiceParserAdapter(@Qualifier("invoiceParserRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public ExtractedInvoiceData extract(InputStream content, String contentType) {
        try {
            // POST multipart/form-data with field "file"
            // return mapped ExtractedInvoiceData
        } catch (Exception e) {
            log.warn("External invoice parser failed: {}", e.getMessage());
            return ExtractedInvoiceData.empty();
        }
    }
}
```

Response mapping (see spec.md for full mapping table):
- `invoiceDate` → parse `LocalDate` from ISO string (null-safe)
- `netAmount.amount` → `netAmount` as `BigDecimal`
- `vatAmount.amount` → `vatAmount` as `BigDecimal`
- `totalAmount.amount` → `grossAmount` as `BigDecimal`
- `netAmount.currency` → `currency`
- `vatRate` always null

On any HTTP error or exception: log and return `ExtractedInvoiceData.empty()`. Log the `X-Request-Id` response header if present in HTTP error responses.

**Test**: `ExternalInvoiceParserAdapterTest.java` using `MockRestServiceServer`.

Setup:
```java
RestTemplate restTemplate = new RestTemplate();
MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
RestClient restClient = RestClient.builder(restTemplate).baseUrl("http://test").build();
ExternalInvoiceParserAdapter adapter = new ExternalInvoiceParserAdapter(restClient);
```

Test cases (write failing test → implement → pass):
1. Full response → all fields mapped correctly
2. Partial response (some nulls) → null fields propagate
3. External service returns HTTP 400 → returns `ExtractedInvoiceData.empty()`
4. External service returns HTTP 503 → returns `ExtractedInvoiceData.empty()`
5. Network error (connection refused, simulated by server.expect throwing) → returns `ExtractedInvoiceData.empty()`

- [ ] Task 2: Create `ExternalInvoiceResponse` and `ExternalInvoiceParserAdapter` with full response mapping and graceful degradation (TDD with `MockRestServiceServer`)

---

### Task 3: Wire adapter into ExtractionCommandApiImpl

**What**: Modify `ExtractionCommandApiImpl` to depend on `ExternalInvoiceParserAdapter` instead of `ExtractInvoiceDataUseCase`.

Before:
```java
class ExtractionCommandApiImpl implements ExtractionCommandApi {
    private final ExtractInvoiceDataUseCase extractInvoiceData;
    // ...
}
```

After:
```java
class ExtractionCommandApiImpl implements ExtractionCommandApi {
    private final ExternalInvoiceParserAdapter externalInvoiceParser;

    ExtractionCommandApiImpl(ExternalInvoiceParserAdapter externalInvoiceParser) {
        this.externalInvoiceParser = externalInvoiceParser;
    }

    @Override
    public ExtractedInvoiceData extract(InputStream content, String contentType) {
        return externalInvoiceParser.extract(content, contentType);
    }
}
```

Note: `ExtractionCommandApiImpl` keeps its `@Service` annotation. The adapter is injected by type; no `@Qualifier` needed since there's only one `ExternalInvoiceParserAdapter` bean.

No separate test needed — the adapter's own test (Task 2) covers the logic. The system test (Task 7) covers the wiring.

- [ ] Task 3: Modify `ExtractionCommandApiImpl` to inject `ExternalInvoiceParserAdapter`

---

### Task 4: Restrict controller validation to PDF only

**What**: In `InvoiceExtractionController`, change `ALLOWED_CONTENT_TYPES` from `{application/pdf, image/png, image/jpeg}` to `{application/pdf}` only.

**Test**: Modify `InvoiceExtractionControllerTest`:
- Remove `extractsInvoiceDataFromImage()` (PNG was accepted; now returns 400)
- Add `returnsBadRequestForPngDocument()` — expects 400
- Add `returnsBadRequestForJpegDocument()` — expects 400
- All other existing tests remain unchanged

Write the updated tests first (they will fail because the controller still accepts PNG). Then update the controller.

- [ ] Task 4: Update controller test to expect 400 for PNG/JPEG, then restrict `ALLOWED_CONTENT_TYPES` to PDF only

---

### Task 5: Replace system test

**What**: Delete `finance/extraction/InvoiceExtractionSystemTest.java` and create a new one that:
- Removes `@MockitoBean OcrPort` and `@MockitoBean InvoiceParserPort` (deleted classes)
- Adds `@MockitoBean ExternalInvoiceParserAdapter` configured to return a populated `ExtractedInvoiceData`
- Adds `INVOICE_PARSER_URL` and `INVOICE_PARSER_API_KEY` to `DynamicPropertySource`
- Tests: PDF upload → 200 with data; PNG upload → 400; unsupported type → 400

Test structure:
```java
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class InvoiceExtractionSystemTest {

    @MockitoBean
    private ExternalInvoiceParserAdapter externalInvoiceParserAdapter;

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        // existing postgres + minio props...
        registry.add("invoice.parser.url", () -> "http://test-parser");
        registry.add("invoice.parser.api-key", () -> "test-key");
    }

    @BeforeEach
    void setUp() {
        when(externalInvoiceParserAdapter.extract(any(), any()))
            .thenReturn(new ExtractedInvoiceData(...));
    }
    // tests...
}
```

- [ ] Task 5: Delete old `InvoiceExtractionSystemTest` and create replacement that mocks `ExternalInvoiceParserAdapter`

---

### Task 6: Delete internal pipeline (production code)

Delete these files:
- `finance/extraction/infrastructure/OcrPort.java`
- `finance/extraction/infrastructure/InvoiceParserPort.java`
- `finance/extraction/application/ExtractInvoiceDataUseCase.java`
- `infrastructure/ocr/TesseractOcrAdapter.java`
- `infrastructure/ocr/TesseractProperties.java`
- `infrastructure/llm/OllamaInvoiceParserAdapter.java`
- `infrastructure/llm/OllamaProperties.java`

Then delete the now-empty directories:
- `infrastructure/ocr/` (if empty)
- `infrastructure/llm/` (if empty)

Run `./gradlew build` after deletion to confirm no compile errors.

- [ ] Task 6: Delete all internal pipeline production code and verify build passes

---

### Task 7: Delete internal pipeline tests

Delete these test files:
- `finance/extraction/application/ExtractInvoiceDataUseCaseTest.java`
- `infrastructure/ocr/TesseractOcrAdapterTest.java`
- `infrastructure/llm/OllamaInvoiceParserAdapterTest.java`

Run `./gradlew test` to confirm all remaining tests pass.

- [ ] Task 7: Delete old test files and verify all remaining tests pass

---

### Task 8: Remove Tesseract dependency from build.gradle.kts

**What**: Remove from `build.gradle.kts`:
```kotlin
implementation("net.sourceforge.tess4j:tess4j:5.13.0")
```

If there are any Ollama or LLM HTTP client dependencies (not visible in current build.gradle.kts — check), remove those too.

Run `./gradlew build` to confirm the build succeeds without these dependencies.

- [ ] Task 8: Remove `tess4j` from `build.gradle.kts` and verify build

---

### Task 9: Create `.env.example`

Create `.env.example` at the repo root (`/Users/tobiasurhaug/code/label-manager/.env.example`) documenting the required environment variables:

```dotenv
# External invoice parsing service
# Required — application will not start without these
INVOICE_PARSER_URL=https://your-invoice-parser-service.example.com
INVOICE_PARSER_API_KEY=your-api-key-here
```

- [ ] Task 9: Create `.env.example` documenting `INVOICE_PARSER_URL` and `INVOICE_PARSER_API_KEY`

---

## Blockers

None.

## Task Order Note

Tasks 1–4 are additive and can proceed while old code still compiles. Tasks 6–7 (deletion) should only run after Tasks 1–5 are complete and all tests pass. Tasks 8–9 are independent cleanup.
