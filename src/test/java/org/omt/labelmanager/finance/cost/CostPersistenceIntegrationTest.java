package org.omt.labelmanager.finance.cost;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.finance.cost.CostOwnerType;
import org.omt.labelmanager.finance.cost.CostType;
import org.omt.labelmanager.finance.cost.CostEntity;
import org.omt.labelmanager.finance.cost.CostOwnerEmbeddable;
import org.omt.labelmanager.finance.cost.CostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CostPersistenceIntegrationTest {

    private static final String MINIO_ACCESS_KEY = "minioadmin";
    private static final String MINIO_SECRET_KEY = "minioadmin";

    @Autowired
    CostRepository costRepository;

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

    @Test
    void savesAndRetrievesCost() {
        var entity = new CostEntity(
                "EUR",
                new BigDecimal("100.00"),
                new BigDecimal("25.00"),
                new BigDecimal("0.25"),
                new BigDecimal("125.00"),
                CostType.MASTERING,
                LocalDate.of(2024, 6, 15),
                "Mastering for album",
                new CostOwnerEmbeddable(CostOwnerType.RELEASE, 1L),
                "INV-2024-001",
                "costs/uuid/invoice.pdf"
        );

        var saved = costRepository.save(entity);

        var retrieved = costRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getCurrency()).isEqualTo("EUR");
        assertThat(retrieved.get().getNetAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(retrieved.get().getVatAmount()).isEqualTo(new BigDecimal("25.00"));
        assertThat(retrieved.get().getVatRate()).isEqualTo(new BigDecimal("0.2500"));
        assertThat(retrieved.get().getGrossAmount()).isEqualTo(new BigDecimal("125.00"));
        assertThat(retrieved.get().getCostType()).isEqualTo(CostType.MASTERING);
        assertThat(retrieved.get().getIncurredOn()).isEqualTo(LocalDate.of(2024, 6, 15));
        assertThat(retrieved.get().getDescription()).isEqualTo("Mastering for album");
        assertThat(retrieved.get().getOwner().getOwnerType()).isEqualTo(CostOwnerType.RELEASE);
        assertThat(retrieved.get().getOwner().getOwnerId()).isEqualTo(1L);
        assertThat(retrieved.get().getDocumentReference()).isEqualTo("INV-2024-001");
        assertThat(retrieved.get().getDocumentStorageKey()).isEqualTo("costs/uuid/invoice.pdf");
    }

    @Test
    void findsByOwner() {
        var releaseOwner = new CostOwnerEmbeddable(CostOwnerType.RELEASE, 42L);
        var labelOwner = new CostOwnerEmbeddable(CostOwnerType.LABEL, 10L);

        costRepository.save(new CostEntity(
                "EUR",
                new BigDecimal("50.00"),
                new BigDecimal("12.50"),
                new BigDecimal("0.25"),
                new BigDecimal("62.50"),
                CostType.MASTERING,
                LocalDate.of(2024, 1, 1),
                "Cost 1",
                releaseOwner,
                null,
                null
        ));

        costRepository.save(new CostEntity(
                "EUR",
                new BigDecimal("200.00"),
                new BigDecimal("50.00"),
                new BigDecimal("0.25"),
                new BigDecimal("250.00"),
                CostType.MANUFACTURING,
                LocalDate.of(2024, 2, 1),
                "Cost 2",
                releaseOwner,
                null,
                null
        ));

        costRepository.save(new CostEntity(
                "EUR",
                new BigDecimal("100.00"),
                new BigDecimal("25.00"),
                new BigDecimal("0.25"),
                new BigDecimal("125.00"),
                CostType.HOSTING,
                LocalDate.of(2024, 3, 1),
                "Cost 3",
                labelOwner,
                null,
                null
        ));

        var releaseCosts = costRepository.findByOwnerOwnerTypeAndOwnerOwnerId(
                CostOwnerType.RELEASE, 42L);

        assertThat(releaseCosts).hasSize(2);
        assertThat(releaseCosts).allMatch(c -> c.getOwner().getOwnerId().equals(42L));
    }
}
