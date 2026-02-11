package org.omt.labelmanager.inventory.productionrun.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunCommandApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionRunCRUDIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProductionRunCommandApi commandApi;

    @Autowired
    private ProductionRunQueryApi queryApi;

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
    void createsProductionRun() {
        var productionRun = commandApi.createProductionRun(
                releaseId,
                ReleaseFormat.VINYL,
                "Original pressing",
                "Record Industry",
                LocalDate.of(2025, 1, 1),
                500
        );

        assertThat(productionRun.id()).isNotNull();
        assertThat(productionRun.releaseId()).isEqualTo(releaseId);
        assertThat(productionRun.format()).isEqualTo(ReleaseFormat.VINYL);
        assertThat(productionRun.description()).isEqualTo("Original pressing");
        assertThat(productionRun.manufacturer()).isEqualTo("Record Industry");
        assertThat(productionRun.manufacturingDate())
                .isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(productionRun.quantity()).isEqualTo(500);
    }

    @Test
    void findsProductionRunsByReleaseId() {
        commandApi.createProductionRun(
                releaseId,
                ReleaseFormat.VINYL,
                "Original pressing",
                "Record Industry",
                LocalDate.of(2025, 1, 1),
                500
        );

        commandApi.createProductionRun(
                releaseId,
                ReleaseFormat.CD,
                "Initial run",
                "CD Plant",
                LocalDate.of(2025, 1, 15),
                200
        );

        var productionRuns = queryApi.findByReleaseId(releaseId);

        assertThat(productionRuns).hasSize(2);
    }

    @Test
    void deletesProductionRun() {
        var productionRun = commandApi.createProductionRun(
                releaseId,
                ReleaseFormat.VINYL,
                "Original pressing",
                "Record Industry",
                LocalDate.of(2025, 1, 1),
                500
        );

        boolean deleted = commandApi.delete(productionRun.id());

        assertThat(deleted).isTrue();
        assertThat(queryApi.findByReleaseId(releaseId)).isEmpty();
    }

    @Test
    void deleteReturnsFalseForNonExistentProductionRun() {
        boolean deleted = commandApi.delete(999L);

        assertThat(deleted).isFalse();
    }
}
