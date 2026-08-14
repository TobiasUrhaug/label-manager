package org.omt.labelmanager.sales.sale;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.distribution.distributor.api.ChannelType;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.inventory.InventoryLocation;
import org.omt.labelmanager.inventory.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementCommandApi;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.productionrun.ProductionRunTestHelper;
import org.omt.labelmanager.sales.sale.api.SaleCommandApi;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;
import org.omt.labelmanager.sales.sale.infrastructure.SaleRepository;
import org.omt.labelmanager.shared.Format;
import org.omt.labelmanager.shared.Money;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Two sales of the same stock, racing.
 *
 * <p>Checking stock and recording the movement that consumes it are separate statements, so without
 * a lock both transactions read the same balance and both commit — the classic oversell. The sale
 * path takes a pessimistic lock on the pressings it is about to draw from, which serialises the
 * two.
 */
class ConcurrentSaleIntegrationTest extends AbstractIntegrationTest {

    @Autowired private SaleCommandApi saleCommandApi;

    @Autowired private SaleRepository saleRepository;

    @Autowired private InventoryMovementCommandApi inventoryMovementCommandApi;

    @Autowired private InventoryMovementQueryApi inventoryMovementQueryApi;

    @Autowired private DistributorQueryApi distributorQueryApi;

    @Autowired private ProductionRunTestHelper productionRunTestHelper;

    @Autowired private ReleaseTestHelper releaseTestHelper;

    @Autowired private LabelTestHelper labelTestHelper;

    private Long labelId;
    private Long releaseId;
    private Long distributorId;
    private Long productionRunId;

    @BeforeEach
    void setUp() {
        var label = labelTestHelper.createLabelWithDirectDistributor("Concurrency Test Label");
        labelId = label.id();
        distributorId =
                distributorQueryApi
                        .findByLabelIdAndChannelType(labelId, ChannelType.DIRECT)
                        .orElseThrow()
                        .id();

        releaseId = releaseTestHelper.createReleaseEntity("Concurrency Test Release", labelId);

        var run =
                productionRunTestHelper.createProductionRun(
                        releaseId,
                        Format.VINYL,
                        "First pressing",
                        "Plant A",
                        LocalDate.of(2025, 1, 1),
                        500);
        productionRunId = run.id();

        // The distributor holds exactly 10 — enough for either sale, not both.
        inventoryMovementCommandApi.recordMovement(
                productionRunId,
                InventoryLocation.warehouse(),
                InventoryLocation.distributor(distributorId),
                10,
                MovementType.ALLOCATION,
                null);
    }

    @Test
    void twoSalesOfTheLastUnitsCannotBothSucceed() throws Exception {
        var bothReady = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Outcome> sellEverything =
                    () -> {
                        bothReady.await(10, TimeUnit.SECONDS);
                        try {
                            saleCommandApi.registerSale(
                                    labelId,
                                    LocalDate.of(2026, 2, 1),
                                    ChannelType.DIRECT,
                                    null,
                                    null,
                                    List.of(
                                            new SaleLineItemInput(
                                                    releaseId,
                                                    Format.VINYL,
                                                    10,
                                                    Money.of(new BigDecimal("15.00")))));
                            return Outcome.SOLD;
                        } catch (RuntimeException e) {
                            return Outcome.REJECTED;
                        }
                    };

            Future<Outcome> first = executor.submit(sellEverything);
            Future<Outcome> second = executor.submit(sellEverything);

            List<Outcome> outcomes =
                    List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));

            assertThat(outcomes).containsExactlyInAnyOrder(Outcome.SOLD, Outcome.REJECTED);
            assertThat(
                            inventoryMovementQueryApi.getCurrentInventory(
                                    productionRunId, distributorId))
                    .isZero();
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * The sales this test commits outlive its transaction — it deliberately runs two of its own —
     * and a sale row keeps its distributor alive, which breaks the {@code deleteAll()} setup other
     * classes use. Cleaned up here rather than left for whichever class runs next.
     */
    @AfterEach
    void removeCommittedSales() {
        saleRepository.deleteAll();
    }

    private enum Outcome {
        SOLD,
        REJECTED
    }
}
