package org.omt.labelmanager.inventory.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.infrastructure.persistence.label.LabelEntity;
import org.omt.labelmanager.catalog.infrastructure.persistence.label.LabelRepository;
import org.omt.labelmanager.inventory.domain.ChannelType;
import org.omt.labelmanager.inventory.infrastructure.persistence.SalesChannelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SalesChannelCRUDHandlerIntegrationTest {

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
    private SalesChannelCRUDHandler salesChannelCRUDHandler;

    @Autowired
    private SalesChannelQueryService salesChannelQueryService;

    @Autowired
    private SalesChannelRepository salesChannelRepository;

    @Autowired
    private LabelRepository labelRepository;

    private Long labelId;

    @BeforeEach
    void setUp() {
        salesChannelRepository.deleteAll();
        labelRepository.deleteAll();

        LabelEntity label = labelRepository.save(new LabelEntity("Test Label", null, null));
        labelId = label.getId();
    }

    @Test
    void createsSalesChannel() {
        var salesChannel = salesChannelCRUDHandler.create(
                labelId,
                "Bandcamp",
                ChannelType.DIRECT
        );

        assertThat(salesChannel.id()).isNotNull();
        assertThat(salesChannel.labelId()).isEqualTo(labelId);
        assertThat(salesChannel.name()).isEqualTo("Bandcamp");
        assertThat(salesChannel.channelType()).isEqualTo(ChannelType.DIRECT);
    }

    @Test
    void findsSalesChannelsByLabelId() {
        salesChannelCRUDHandler.create(labelId, "Direct Sales", ChannelType.DIRECT);
        salesChannelCRUDHandler.create(labelId, "Cargo Records", ChannelType.DISTRIBUTOR);

        var salesChannels = salesChannelCRUDHandler.findByLabelId(labelId);

        assertThat(salesChannels).hasSize(2);
    }

    @Test
    void queryServiceFindsSalesChannelsByLabelId() {
        salesChannelCRUDHandler.create(labelId, "Direct Sales", ChannelType.DIRECT);
        salesChannelCRUDHandler.create(labelId, "Cargo Records", ChannelType.DISTRIBUTOR);

        var salesChannels = salesChannelQueryService.getSalesChannelsForLabel(labelId);

        assertThat(salesChannels).hasSize(2);
    }

    @Test
    void deletesSalesChannel() {
        var salesChannel = salesChannelCRUDHandler.create(
                labelId,
                "Bandcamp",
                ChannelType.DIRECT
        );

        boolean deleted = salesChannelCRUDHandler.delete(salesChannel.id());

        assertThat(deleted).isTrue();
        assertThat(salesChannelCRUDHandler.findByLabelId(labelId)).isEmpty();
    }

    @Test
    void deleteReturnsFalseForNonExistentSalesChannel() {
        boolean deleted = salesChannelCRUDHandler.delete(999L);

        assertThat(deleted).isFalse();
    }
}
