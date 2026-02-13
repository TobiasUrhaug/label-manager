package org.omt.labelmanager.inventory.productionrun.domain;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionRunTest {

    @Test
    void canAllocate_returnsTrueWhenSufficientQuantityAvailable() {
        var productionRun = new ProductionRun(
                1L,
                100L,
                ReleaseFormat.VINYL,
                "Test pressing",
                "Plant A",
                LocalDate.of(2025, 1, 1),
                500
        );

        assertThat(productionRun.canAllocate(100, 200)).isTrue();
    }

    @Test
    void canAllocate_returnsTrueWhenRequestedEqualsAvailable() {
        var productionRun = new ProductionRun(
                1L,
                100L,
                ReleaseFormat.VINYL,
                "Test pressing",
                "Plant A",
                LocalDate.of(2025, 1, 1),
                500
        );

        assertThat(productionRun.canAllocate(200, 300)).isTrue();
    }

    @Test
    void canAllocate_returnsFalseWhenInsufficientQuantityAvailable() {
        var productionRun = new ProductionRun(
                1L,
                100L,
                ReleaseFormat.VINYL,
                "Test pressing",
                "Plant A",
                LocalDate.of(2025, 1, 1),
                500
        );

        assertThat(productionRun.canAllocate(300, 300)).isFalse();
    }

    @Test
    void canAllocate_returnsFalseWhenEverythingAlreadyAllocated() {
        var productionRun = new ProductionRun(
                1L,
                100L,
                ReleaseFormat.VINYL,
                "Test pressing",
                "Plant A",
                LocalDate.of(2025, 1, 1),
                500
        );

        assertThat(productionRun.canAllocate(1, 500)).isFalse();
    }

    @Test
    void getAvailableQuantity_returnsCorrectAmount() {
        var productionRun = new ProductionRun(
                1L,
                100L,
                ReleaseFormat.VINYL,
                "Test pressing",
                "Plant A",
                LocalDate.of(2025, 1, 1),
                500
        );

        assertThat(productionRun.getAvailableQuantity(200)).isEqualTo(300);
    }

    @Test
    void getAvailableQuantity_returnsZeroWhenFullyAllocated() {
        var productionRun = new ProductionRun(
                1L,
                100L,
                ReleaseFormat.VINYL,
                "Test pressing",
                "Plant A",
                LocalDate.of(2025, 1, 1),
                500
        );

        assertThat(productionRun.getAvailableQuantity(500)).isZero();
    }

    @Test
    void getAvailableQuantity_returnsFullQuantityWhenNothingAllocated() {
        var productionRun = new ProductionRun(
                1L,
                100L,
                ReleaseFormat.VINYL,
                "Test pressing",
                "Plant A",
                LocalDate.of(2025, 1, 1),
                500
        );

        assertThat(productionRun.getAvailableQuantity(0)).isEqualTo(500);
    }
}
