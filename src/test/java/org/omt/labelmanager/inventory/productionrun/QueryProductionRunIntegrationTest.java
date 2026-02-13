package org.omt.labelmanager.inventory.productionrun;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunCommandApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class QueryProductionRunIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProductionRunQueryApi queryApi;

    @Autowired
    private ProductionRunCommandApi commandApi;

    @Autowired
    private ReleaseTestHelper releaseTestHelper;

    @Autowired
    private LabelTestHelper labelTestHelper;

    @Test
    void findByReleaseId_returnsAllProductionRunsForRelease() {
        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity(
                "Test Release", label.id());

        commandApi.createProductionRun(
                releaseId,
                ReleaseFormat.VINYL,
                "First pressing",
                "Manufacturer A",
                LocalDate.of(2025, 1, 1),
                500
        );

        commandApi.createProductionRun(
                releaseId,
                ReleaseFormat.CD,
                "CD run",
                "Manufacturer B",
                LocalDate.of(2025, 2, 1),
                300
        );

        commandApi.createProductionRun(
                releaseId,
                ReleaseFormat.VINYL,
                "Second pressing",
                "Manufacturer A",
                LocalDate.of(2025, 3, 1),
                400
        );

        var runs = queryApi.findByReleaseId(releaseId);

        assertThat(runs).hasSize(3);
        assertThat(runs)
                .extracting("format")
                .containsExactlyInAnyOrder(
                        ReleaseFormat.VINYL,
                        ReleaseFormat.CD,
                        ReleaseFormat.VINYL
                );
    }

    @Test
    void findByReleaseId_returnsEmptyListWhenNoProductionRuns() {
        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity(
                "Test Release", label.id());

        var runs = queryApi.findByReleaseId(releaseId);

        assertThat(runs).isEmpty();
    }

    @Test
    void findMostRecent_returnsMostRecentProductionRunForFormat() {
        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity(
                "Test Release", label.id());

        commandApi.createProductionRun(
                releaseId,
                ReleaseFormat.VINYL,
                "First pressing",
                "Manufacturer A",
                LocalDate.of(2025, 1, 1),
                500
        );

        var secondPressing = commandApi.createProductionRun(
                releaseId,
                ReleaseFormat.VINYL,
                "Second pressing",
                "Manufacturer A",
                LocalDate.of(2025, 3, 1),
                400
        );

        commandApi.createProductionRun(
                releaseId,
                ReleaseFormat.VINYL,
                "Third pressing",
                "Manufacturer A",
                LocalDate.of(2025, 2, 15),
                300
        );

        var mostRecent = queryApi.findMostRecent(releaseId, ReleaseFormat.VINYL);

        assertThat(mostRecent).isPresent();
        assertThat(mostRecent.get().id()).isEqualTo(secondPressing.id());
        assertThat(mostRecent.get().description()).isEqualTo("Second pressing");
        assertThat(mostRecent.get().manufacturingDate())
                .isEqualTo(LocalDate.of(2025, 3, 1));
    }

    @Test
    void findMostRecent_returnsEmptyWhenNoMatchingFormat() {
        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity(
                "Test Release", label.id());

        commandApi.createProductionRun(
                releaseId,
                ReleaseFormat.VINYL,
                "Vinyl pressing",
                "Manufacturer A",
                LocalDate.of(2025, 1, 1),
                500
        );

        var mostRecent = queryApi.findMostRecent(releaseId, ReleaseFormat.CD);

        assertThat(mostRecent).isEmpty();
    }

    @Test
    void findMostRecent_returnsEmptyWhenNoProductionRuns() {
        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity(
                "Test Release", label.id());

        var mostRecent = queryApi.findMostRecent(releaseId, ReleaseFormat.VINYL);

        assertThat(mostRecent).isEmpty();
    }

    @Test
    void findMostRecent_distinguishesBetweenFormats() {
        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity(
                "Test Release", label.id());

        var vinylRun = commandApi.createProductionRun(
                releaseId,
                ReleaseFormat.VINYL,
                "Vinyl pressing",
                "Manufacturer A",
                LocalDate.of(2025, 3, 1),
                500
        );

        var cdRun = commandApi.createProductionRun(
                releaseId,
                ReleaseFormat.CD,
                "CD pressing",
                "Manufacturer B",
                LocalDate.of(2025, 2, 1),
                300
        );

        var mostRecentVinyl = queryApi.findMostRecent(
                releaseId, ReleaseFormat.VINYL);
        var mostRecentCd = queryApi.findMostRecent(releaseId, ReleaseFormat.CD);

        assertThat(mostRecentVinyl).isPresent();
        assertThat(mostRecentVinyl.get().id()).isEqualTo(vinylRun.id());

        assertThat(mostRecentCd).isPresent();
        assertThat(mostRecentCd.get().id()).isEqualTo(cdRun.id());
    }

    @Test
    void getManufacturedQuantity_returnsQuantityForExistingProductionRun() {
        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity(
                "Test Release", label.id());

        var productionRun = commandApi.createProductionRun(
                releaseId,
                ReleaseFormat.VINYL,
                "Pressing",
                "Manufacturer A",
                LocalDate.of(2025, 1, 1),
                500
        );

        int quantity = queryApi.getManufacturedQuantity(productionRun.id());

        assertThat(quantity).isEqualTo(500);
    }

    @Test
    void getManufacturedQuantity_returnsZeroForNonExistentProductionRun() {
        int quantity = queryApi.getManufacturedQuantity(999L);

        assertThat(quantity).isZero();
    }
}
