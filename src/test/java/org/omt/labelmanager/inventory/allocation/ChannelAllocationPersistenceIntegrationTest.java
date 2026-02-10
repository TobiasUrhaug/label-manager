package org.omt.labelmanager.inventory.allocation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.release.ReleaseFormat;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.inventory.domain.ChannelType;
import org.omt.labelmanager.inventory.infrastructure.persistence.ProductionRunEntity;
import org.omt.labelmanager.inventory.infrastructure.persistence.ProductionRunRepository;
import org.omt.labelmanager.inventory.infrastructure.persistence.SalesChannelEntity;
import org.omt.labelmanager.inventory.infrastructure.persistence.SalesChannelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChannelAllocationPersistenceIntegrationTest {

    private static final String MINIO_ACCESS_KEY = "minioadmin";
    private static final String MINIO_SECRET_KEY = "minioadmin";

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

    @Autowired
    private ChannelAllocationRepository channelAllocationRepository;

    @Autowired
    private ProductionRunRepository productionRunRepository;

    @Autowired
    private SalesChannelRepository salesChannelRepository;

    @Autowired
    private ReleaseTestHelper releaseTestHelper;

    @Autowired
    private LabelTestHelper labelTestHelper;

    private Long productionRunId;
    private Long salesChannelId;

    @BeforeEach
    void setUp() {
        channelAllocationRepository.deleteAll();
        productionRunRepository.deleteAll();
        salesChannelRepository.deleteAll();

        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity(
                "Test Release", label.id());

        ProductionRunEntity productionRun = productionRunRepository.save(
                new ProductionRunEntity(
                        releaseId,
                        ReleaseFormat.VINYL,
                        "First pressing",
                        "Plant A",
                        LocalDate.of(2025, 1, 1),
                        500
                ));
        productionRunId = productionRun.getId();

        SalesChannelEntity salesChannel = salesChannelRepository.save(
                new SalesChannelEntity(label.id(), "Direct Sales", ChannelType.DIRECT));
        salesChannelId = salesChannel.getId();
    }

    @Test
    void savesAndRetrievesChannelAllocation() {
        Instant allocatedAt = Instant.parse("2025-06-15T10:00:00Z");
        var entity = new ChannelAllocationEntity(
                productionRunId, salesChannelId, 100, allocatedAt);

        var saved = channelAllocationRepository.save(entity);

        var retrieved = channelAllocationRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getProductionRunId()).isEqualTo(productionRunId);
        assertThat(retrieved.get().getSalesChannelId()).isEqualTo(salesChannelId);
        assertThat(retrieved.get().getQuantity()).isEqualTo(100);
        assertThat(retrieved.get().getAllocatedAt()).isEqualTo(allocatedAt);
    }

    @Test
    void findsByProductionRunId() {
        Instant allocatedAt = Instant.now();
        channelAllocationRepository.save(
                new ChannelAllocationEntity(productionRunId, salesChannelId, 100, allocatedAt));
        channelAllocationRepository.save(
                new ChannelAllocationEntity(productionRunId, salesChannelId, 50, allocatedAt));

        var allocations = channelAllocationRepository.findByProductionRunId(productionRunId);

        assertThat(allocations).hasSize(2);
        assertThat(allocations)
                .allMatch(a -> a.getProductionRunId().equals(productionRunId));
    }

    @Test
    void sumQuantityByProductionRunIdReturnsTotal() {
        Instant allocatedAt = Instant.now();
        channelAllocationRepository.save(
                new ChannelAllocationEntity(productionRunId, salesChannelId, 100, allocatedAt));
        channelAllocationRepository.save(
                new ChannelAllocationEntity(productionRunId, salesChannelId, 150, allocatedAt));

        int total = channelAllocationRepository.sumQuantityByProductionRunId(productionRunId);

        assertThat(total).isEqualTo(250);
    }

    @Test
    void sumQuantityByProductionRunIdReturnsZeroWhenNoAllocations() {
        int total = channelAllocationRepository.sumQuantityByProductionRunId(productionRunId);

        assertThat(total).isZero();
    }

    @Test
    void deletesAllocationWhenProductionRunDeleted() {
        Instant allocatedAt = Instant.now();
        channelAllocationRepository.save(
                new ChannelAllocationEntity(productionRunId, salesChannelId, 100, allocatedAt));

        assertThat(channelAllocationRepository.findByProductionRunId(productionRunId)).hasSize(1);

        productionRunRepository.deleteById(productionRunId);

        assertThat(channelAllocationRepository.findByProductionRunId(productionRunId)).isEmpty();
    }
}
