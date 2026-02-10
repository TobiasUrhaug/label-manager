package org.omt.labelmanager.finance.extraction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.finance.application.extraction.InvoiceParserPort;
import org.omt.labelmanager.finance.application.extraction.OcrPort;
import org.omt.labelmanager.finance.extraction.domain.ExtractedInvoiceData;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class InvoiceExtractionSystemTest {

    private static final String MINIO_ACCESS_KEY = "minioadmin";
    private static final String MINIO_SECRET_KEY = "minioadmin";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OcrPort ocrPort;

    @MockitoBean
    private InvoiceParserPort invoiceParserPort;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    static MinIOContainer minIO = new MinIOContainer("minio/minio:latest")
            .withUserName(MINIO_ACCESS_KEY)
            .withPassword(MINIO_SECRET_KEY);

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("storage.s3.endpoint", minIO::getS3URL);
        registry.add("storage.s3.bucket", () -> "costs");
        registry.add("storage.s3.region", () -> "us-east-1");
        registry.add("storage.s3.access-key", () -> MINIO_ACCESS_KEY);
        registry.add("storage.s3.secret-key", () -> MINIO_SECRET_KEY);
    }

    @BeforeEach
    void setUp() {
        when(ocrPort.extractText(any(), any())).thenReturn("Invoice OCR text");
        when(invoiceParserPort.parse(any())).thenReturn(new ExtractedInvoiceData(
                new BigDecimal("100.00"),
                new BigDecimal("21.00"),
                new BigDecimal("21"),
                new BigDecimal("121.00"),
                LocalDate.of(2024, 1, 15),
                "INV-2024-001",
                "EUR"
        ));
    }

    @Test
    void extractsInvoiceDataFromPdfDocument() throws Exception {
        MockMultipartFile document = new MockMultipartFile(
                "document",
                "invoice.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        mockMvc.perform(multipart("/api/costs/extract")
                        .file(document)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netAmount").value(100.00))
                .andExpect(jsonPath("$.vatAmount").value(21.00))
                .andExpect(jsonPath("$.vatRate").value(21))
                .andExpect(jsonPath("$.grossAmount").value(121.00))
                .andExpect(jsonPath("$.invoiceDate").value("2024-01-15"))
                .andExpect(jsonPath("$.invoiceReference").value("INV-2024-001"))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void extractsInvoiceDataFromImageDocument() throws Exception {
        MockMultipartFile document = new MockMultipartFile(
                "document",
                "invoice.png",
                "image/png",
                "image content".getBytes()
        );

        mockMvc.perform(multipart("/api/costs/extract")
                        .file(document)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netAmount").exists());
    }

    @Test
    void returnsBadRequestForUnsupportedDocumentType() throws Exception {
        MockMultipartFile document = new MockMultipartFile(
                "document",
                "invoice.doc",
                "application/msword",
                "doc content".getBytes()
        );

        mockMvc.perform(multipart("/api/costs/extract")
                        .file(document)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}
