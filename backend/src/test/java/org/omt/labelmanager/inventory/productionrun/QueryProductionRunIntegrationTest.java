package org.omt.labelmanager.inventory.productionrun;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.inventory.InventoryLocation;
import org.omt.labelmanager.inventory.domain.RunDraw;
import org.omt.labelmanager.inventory.domain.RunStock;
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
    void ledgerAt_reportsEachPressingsWarehouseStockOldestFirst() {
        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity("Test Release", label.id());

        var repress =
                commandApi.createProductionRun(
                        releaseId,
                        Format.VINYL,
                        "Second pressing",
                        "Manufacturer A",
                        LocalDate.of(2026, 1, 1),
                        200);
        var firstPressing =
                commandApi.createProductionRun(
                        releaseId,
                        Format.VINYL,
                        "First pressing",
                        "Manufacturer A",
                        LocalDate.of(2025, 1, 1),
                        500);

        var ledger = queryApi.ledgerAt(releaseId, Format.VINYL, InventoryLocation.warehouse());

        assertThat(ledger.onHand()).isEqualTo(700);
        assertThat(ledger.drawFifo(600))
                .containsExactly(
                        new RunDraw(firstPressing.id(), 500), new RunDraw(repress.id(), 100));
    }

    @Test
    void ledgerAt_countsOnlyTheRequestedFormat() {
        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity("Test Release", label.id());

        commandApi.createProductionRun(
                releaseId, Format.VINYL, "Vinyl", "Manufacturer A", LocalDate.of(2025, 1, 1), 500);
        commandApi.createProductionRun(
                releaseId, Format.CD, "CD", "Manufacturer B", LocalDate.of(2025, 2, 1), 300);

        assertThat(queryApi.ledgerAt(releaseId, Format.CD, InventoryLocation.warehouse()).onHand())
                .isEqualTo(300);
    }

    @Test
    void ledgerAt_isEmptyWhenTheReleaseHasNoPressingInThatFormat() {
        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity("Test Release", label.id());

        var ledger = queryApi.ledgerAt(releaseId, Format.VINYL, InventoryLocation.warehouse());

        assertThat(ledger.runs()).isEmpty();
        assertThat(ledger.onHand()).isZero();
    }

    /** A pressing that has never reached a location is still in the ledger, holding nothing. */
    @Test
    void ledgerAt_includesPressingsWithNoStockAtThatLocation() {
        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity("Test Release", label.id());

        var run =
                commandApi.createProductionRun(
                        releaseId,
                        Format.VINYL,
                        "First pressing",
                        "Manufacturer A",
                        LocalDate.of(2025, 1, 1),
                        500);

        var ledger = queryApi.ledgerAt(releaseId, Format.VINYL, InventoryLocation.distributor(7L));

        assertThat(ledger.runs()).extracting(RunStock::productionRunId).containsExactly(run.id());
        assertThat(ledger.onHand()).isZero();
    }
}
