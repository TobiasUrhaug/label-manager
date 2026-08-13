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

class DeleteProductionRunIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ProductionRunCommandApi commandApi;

    @Autowired private ProductionRunQueryApi queryApi;

    @Autowired private ReleaseTestHelper releaseTestHelper;

    @Autowired private LabelTestHelper labelTestHelper;

    @Test
    void deletesProductionRun() {
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
