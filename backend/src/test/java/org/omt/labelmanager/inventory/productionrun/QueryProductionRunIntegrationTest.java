package org.omt.labelmanager.inventory.productionrun;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunCommandApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.shared.Format;
import org.springframework.beans.factory.annotation.Autowired;

class QueryProductionRunIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ProductionRunQueryApi queryApi;

    @Autowired private ProductionRunCommandApi commandApi;

    @Autowired private ReleaseTestHelper releaseTestHelper;

    @Autowired private LabelTestHelper labelTestHelper;

    @Test
    void findByReleaseId_returnsAllProductionRunsForRelease() {
        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity("Test Release", label.id());

        commandApi.createProductionRun(
                releaseId,
                Format.VINYL,
                "First pressing",
                "Manufacturer A",
                LocalDate.of(2025, 1, 1),
                500);

        commandApi.createProductionRun(
                releaseId, Format.CD, "CD run", "Manufacturer B", LocalDate.of(2025, 2, 1), 300);

        commandApi.createProductionRun(
                releaseId,
                Format.VINYL,
                "Second pressing",
                "Manufacturer A",
                LocalDate.of(2025, 3, 1),
                400);

        var runs = queryApi.findByReleaseId(releaseId);

        assertThat(runs).hasSize(3);
        assertThat(runs)
                .extracting("format")
                .containsExactlyInAnyOrder(Format.VINYL, Format.CD, Format.VINYL);
    }

    @Test
    void findByReleaseId_returnsEmptyListWhenNoProductionRuns() {
        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity("Test Release", label.id());

        var runs = queryApi.findByReleaseId(releaseId);

        assertThat(runs).isEmpty();
    }

    @Test
    void findMostRecent_returnsMostRecentProductionRunForFormat() {
        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity("Test Release", label.id());

        commandApi.createProductionRun(
                releaseId,
                Format.VINYL,
                "First pressing",
                "Manufacturer A",
                LocalDate.of(2025, 1, 1),
                500);

        var secondPressing =
                commandApi.createProductionRun(
                        releaseId,
                        Format.VINYL,
                        "Second pressing",
                        "Manufacturer A",
                        LocalDate.of(2025, 3, 1),
                        400);

        commandApi.createProductionRun(
                releaseId,
                Format.VINYL,
                "Third pressing",
                "Manufacturer A",
                LocalDate.of(2025, 2, 15),
                300);

        var mostRecent = queryApi.findMostRecent(releaseId, Format.VINYL);

        assertThat(mostRecent).isPresent();
        assertThat(mostRecent.get().id()).isEqualTo(secondPressing.id());
        assertThat(mostRecent.get().description()).isEqualTo("Second pressing");
        assertThat(mostRecent.get().manufacturingDate()).isEqualTo(LocalDate.of(2025, 3, 1));
    }

    @Test
    void findMostRecent_returnsEmptyWhenNoMatchingFormat() {
        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity("Test Release", label.id());

        commandApi.createProductionRun(
                releaseId,
                Format.VINYL,
                "Vinyl pressing",
                "Manufacturer A",
                LocalDate.of(2025, 1, 1),
                500);

        var mostRecent = queryApi.findMostRecent(releaseId, Format.CD);

        assertThat(mostRecent).isEmpty();
    }

    @Test
    void findMostRecent_returnsEmptyWhenNoProductionRuns() {
        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity("Test Release", label.id());

        var mostRecent = queryApi.findMostRecent(releaseId, Format.VINYL);

        assertThat(mostRecent).isEmpty();
    }

    @Test
    void findMostRecent_distinguishesBetweenFormats() {
        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity("Test Release", label.id());

        var vinylRun =
                commandApi.createProductionRun(
                        releaseId,
                        Format.VINYL,
                        "Vinyl pressing",
                        "Manufacturer A",
                        LocalDate.of(2025, 3, 1),
                        500);

        var cdRun =
                commandApi.createProductionRun(
                        releaseId,
                        Format.CD,
                        "CD pressing",
                        "Manufacturer B",
                        LocalDate.of(2025, 2, 1),
                        300);

        var mostRecentVinyl = queryApi.findMostRecent(releaseId, Format.VINYL);
        var mostRecentCd = queryApi.findMostRecent(releaseId, Format.CD);

        assertThat(mostRecentVinyl).isPresent();
        assertThat(mostRecentVinyl.get().id()).isEqualTo(vinylRun.id());

        assertThat(mostRecentCd).isPresent();
        assertThat(mostRecentCd.get().id()).isEqualTo(cdRun.id());
    }

    @Test
    void getManufacturedQuantity_returnsQuantityForExistingProductionRun() {
        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity("Test Release", label.id());

        var productionRun =
                commandApi.createProductionRun(
                        releaseId,
                        Format.VINYL,
                        "Pressing",
                        "Manufacturer A",
                        LocalDate.of(2025, 1, 1),
                        500);

        int quantity = queryApi.getManufacturedQuantity(productionRun.id());

        assertThat(quantity).isEqualTo(500);
    }

    @Test
    void getManufacturedQuantity_returnsZeroForNonExistentProductionRun() {
        int quantity = queryApi.getManufacturedQuantity(999L);

        assertThat(quantity).isZero();
    }
}
