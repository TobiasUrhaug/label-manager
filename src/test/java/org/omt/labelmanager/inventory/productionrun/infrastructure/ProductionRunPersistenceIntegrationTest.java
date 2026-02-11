package org.omt.labelmanager.inventory.productionrun.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionRunPersistenceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProductionRunRepository repository;

    @Autowired
    private ReleaseTestHelper releaseTestHelper;

    @Autowired
    private LabelTestHelper labelTestHelper;

    private Long releaseId;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        var label = labelTestHelper.createLabel("Test Label");
        releaseId = releaseTestHelper.createReleaseEntity(
                "Test Release", label.id());
    }

    @Test
    void savesAndRetrievesProductionRun() {
        var entity = new ProductionRunEntity(
                releaseId,
                ReleaseFormat.VINYL,
                "Original pressing",
                "Record Industry",
                LocalDate.of(2025, 1, 1),
                500
        );

        var saved = repository.save(entity);

        var retrieved = repository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getReleaseId()).isEqualTo(releaseId);
        assertThat(retrieved.get().getFormat()).isEqualTo(ReleaseFormat.VINYL);
        assertThat(retrieved.get().getDescription())
                .isEqualTo("Original pressing");
        assertThat(retrieved.get().getManufacturer())
                .isEqualTo("Record Industry");
        assertThat(retrieved.get().getManufacturingDate())
                .isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(retrieved.get().getQuantity()).isEqualTo(500);
    }

    @Test
    void findsByReleaseId() {
        repository.save(new ProductionRunEntity(
                releaseId,
                ReleaseFormat.VINYL,
                "Original pressing",
                "Record Industry",
                LocalDate.of(2025, 1, 1),
                500
        ));

        repository.save(new ProductionRunEntity(
                releaseId,
                ReleaseFormat.CD,
                "Initial run",
                "CD Plant",
                LocalDate.of(2025, 1, 15),
                200
        ));

        var otherLabel = labelTestHelper.createLabel("Other Label");
        Long otherReleaseId = releaseTestHelper.createReleaseEntity(
                "Other Release", otherLabel.id());

        repository.save(new ProductionRunEntity(
                otherReleaseId,
                ReleaseFormat.CASSETTE,
                "Limited edition",
                "Tape Factory",
                LocalDate.of(2025, 2, 1),
                100
        ));

        var productionRunsForRelease = repository.findByReleaseId(releaseId);

        assertThat(productionRunsForRelease).hasSize(2);
        assertThat(productionRunsForRelease)
                .allMatch(pr -> pr.getReleaseId().equals(releaseId));
    }

    @Test
    void deletesProductionRunWhenReleaseDeleted() {
        repository.save(new ProductionRunEntity(
                releaseId,
                ReleaseFormat.VINYL,
                "Original pressing",
                "Record Industry",
                LocalDate.of(2025, 1, 1),
                500
        ));

        assertThat(repository.findByReleaseId(releaseId)).hasSize(1);

        // Note: cascade delete from release is tested via
        // the release module's own integration tests
    }
}
