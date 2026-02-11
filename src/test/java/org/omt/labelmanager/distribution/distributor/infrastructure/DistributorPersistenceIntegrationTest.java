package org.omt.labelmanager.distribution.distributor.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorRepository;
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
class DistributorPersistenceIntegrationTest {

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
    private DistributorRepository distributorRepository;

    @Autowired
    private LabelTestHelper labelTestHelper;

    private Long labelId;

    @BeforeEach
    void setUp() {
        distributorRepository.deleteAll();

        var label = labelTestHelper.createLabel("Test Label");
        labelId = label.id();
    }

    @Test
    void savesAndRetrievesDistributor() {
        var entity = new DistributorEntity(labelId, "Bandcamp", ChannelType.DIRECT);

        var saved = distributorRepository.save(entity);

        var retrieved = distributorRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getLabelId()).isEqualTo(labelId);
        assertThat(retrieved.get().getName()).isEqualTo("Bandcamp");
        assertThat(retrieved.get().getChannelType()).isEqualTo(ChannelType.DIRECT);
    }

    @Test
    void findsByLabelId() {
        distributorRepository.save(
                new DistributorEntity(labelId, "Direct Sales", ChannelType.DIRECT));
        distributorRepository.save(
                new DistributorEntity(labelId, "Cargo Records", ChannelType.DISTRIBUTOR));

        var otherLabel = labelTestHelper.createLabel("Other Label");
        distributorRepository.save(
                new DistributorEntity(otherLabel.id(), "Record Shop", ChannelType.RETAIL));

        var channelsForLabel = distributorRepository.findByLabelId(labelId);

        assertThat(channelsForLabel).hasSize(2);
        assertThat(channelsForLabel)
                .allMatch(channel -> channel.getLabelId().equals(labelId));
    }

    // Note: Cascade delete test removed as LabelRepository is package-private.
    // Cascade behavior from Label to Distributor is tested in the label package.
}
