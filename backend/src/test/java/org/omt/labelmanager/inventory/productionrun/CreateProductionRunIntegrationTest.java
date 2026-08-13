package org.omt.labelmanager.inventory.productionrun;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunCommandApi;
import org.omt.labelmanager.shared.Format;
import org.springframework.beans.factory.annotation.Autowired;

class CreateProductionRunIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ProductionRunCommandApi commandApi;

    @Autowired private ReleaseTestHelper releaseTestHelper;

    @Autowired private LabelTestHelper labelTestHelper;

    @Test
    void createsProductionRun() {
        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity("Test Release", label.id());

        var productionRun =
                commandApi.createProductionRun(
                        releaseId,
                        Format.VINYL,
                        "Original pressing",
                        "Record Industry",
                        LocalDate.of(2025, 1, 1),
                        500);

        assertThat(productionRun.id()).isNotNull();
        assertThat(productionRun.releaseId()).isEqualTo(releaseId);
        assertThat(productionRun.format()).isEqualTo(Format.VINYL);
        assertThat(productionRun.description()).isEqualTo("Original pressing");
        assertThat(productionRun.manufacturer()).isEqualTo("Record Industry");
        assertThat(productionRun.manufacturingDate()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(productionRun.quantity()).isEqualTo(500);
    }
}
