package org.omt.labelmanager.sales.sale.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.catalog.release.domain.Release;
import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.inventory.InventoryLocation;
import org.omt.labelmanager.inventory.domain.RunDraw;
import org.omt.labelmanager.inventory.domain.RunStock;
import org.omt.labelmanager.inventory.domain.StockLedger;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;
import org.omt.labelmanager.sales.sale.infrastructure.SaleEntity;
import org.omt.labelmanager.shared.Format;
import org.omt.labelmanager.shared.Money;

@ExtendWith(MockitoExtension.class)
class SaleLineItemProcessorTest {

    private static final long LABEL_ID = 1L;
    private static final long RELEASE_ID = 10L;
    private static final long FIRST_PRESSING = 100L;
    private static final long REPRESS = 101L;
    private static final long OTHER_RELEASE_ID = 9L;
    private static final long DISTRIBUTOR_ID = 200L;
    private static final InventoryLocation FROM = InventoryLocation.distributor(DISTRIBUTOR_ID);

    @Mock private ReleaseQueryApi releaseQueryApi;

    @Mock private ProductionRunQueryApi productionRunQueryApi;

    @Mock private SaleEntity saleEntity;

    private SaleLineItemProcessor subject;

    @BeforeEach
    void setUp() {
        subject = new SaleLineItemProcessor(releaseQueryApi, productionRunQueryApi);
    }

    @Test
    void validateAndAdd_throwsInsufficientInventoryException_whenDistributorHasZeroStock() {
        givenRelease();
        givenLedger(pressing(FIRST_PRESSING, "2024-01-01", 0));

        assertThatThrownBy(
                        () ->
                                subject.validateAndAdd(
                                        List.of(lineItem(1)), LABEL_ID, FROM, saleEntity))
                .isInstanceOf(InsufficientInventoryException.class);
    }

    @Test
    void validateAndAdd_addsLineItemAndReportsWhereItCameFrom() {
        givenRelease();
        givenLedger(pressing(FIRST_PRESSING, "2024-01-01", 100));

        var draws = subject.validateAndAdd(List.of(lineItem(5)), LABEL_ID, FROM, saleEntity);

        assertThat(draws).containsExactly(new RunDraw(FIRST_PRESSING, 5));
        verify(saleEntity).addLineItem(org.mockito.ArgumentMatchers.any());
    }

    /** F4: the old code sold from the newest pressing and ignored stock sitting in older ones. */
    @Test
    void validateAndAdd_splitsALineItemAcrossPressingsOldestFirst() {
        givenRelease();
        givenLedger(
                pressing(REPRESS, "2026-01-01", 100), pressing(FIRST_PRESSING, "2024-01-01", 30));

        var draws = subject.validateAndAdd(List.of(lineItem(50)), LABEL_ID, FROM, saleEntity);

        assertThat(draws)
                .containsExactly(new RunDraw(FIRST_PRESSING, 30), new RunDraw(REPRESS, 20));
    }

    /**
     * Two line items for the same release share stock. Validating each against the opening balances
     * would let one sale take 60 units out of a pressing that only has 40.
     */
    @Test
    void validateAndAdd_takesEarlierLineItemsOffTheStockLaterOnesSee() {
        givenRelease();
        givenLedger(
                pressing(FIRST_PRESSING, "2024-01-01", 40), pressing(REPRESS, "2026-01-01", 60));

        var draws =
                subject.validateAndAdd(
                        List.of(lineItem(30), lineItem(30)), LABEL_ID, FROM, saleEntity);

        assertThat(draws)
                .containsExactly(
                        new RunDraw(FIRST_PRESSING, 30),
                        new RunDraw(FIRST_PRESSING, 10),
                        new RunDraw(REPRESS, 20));
    }

    @Test
    void validateAndAdd_refusesWhenTheLineItemsTogetherExceedTheStock() {
        givenRelease();
        givenLedger(pressing(FIRST_PRESSING, "2024-01-01", 50));

        assertThatThrownBy(
                        () ->
                                subject.validateAndAdd(
                                        List.of(lineItem(30), lineItem(30)),
                                        LABEL_ID,
                                        FROM,
                                        saleEntity))
                .isInstanceOf(InsufficientInventoryException.class);
    }

    /**
     * Locks go in a fixed order, not the order the line items happen to arrive in. Two sales
     * listing the same two releases in opposite orders would otherwise each hold what the other
     * waits for, and Postgres would kill one — a 500 rather than the 409 an out-of-stock sale gets.
     */
    @Test
    void validateAndAdd_locksLedgersInAFixedOrderWhateverOrderTheLineItemsCome() {
        givenRelease();
        givenReleaseWithId(OTHER_RELEASE_ID);
        givenLedger(pressing(FIRST_PRESSING, "2024-01-01", 100));
        when(productionRunQueryApi.lockedLedgerAt(OTHER_RELEASE_ID, Format.VINYL, FROM))
                .thenReturn(StockLedger.of(List.of(pressing(REPRESS, "2024-01-01", 100))));

        // Line items in descending release order; the locks must still be taken in ascending.
        subject.validateAndAdd(
                List.of(lineItem(1), lineItemFor(OTHER_RELEASE_ID, 1)), LABEL_ID, FROM, saleEntity);

        var inOrder = inOrder(productionRunQueryApi);
        inOrder.verify(productionRunQueryApi).lockedLedgerAt(OTHER_RELEASE_ID, Format.VINYL, FROM);
        inOrder.verify(productionRunQueryApi).lockedLedgerAt(RELEASE_ID, Format.VINYL, FROM);
    }

    @Test
    void validateAndAdd_refusesAReleaseThatHasNeverBeenPressed() {
        givenRelease();
        givenLedger();

        assertThatThrownBy(
                        () ->
                                subject.validateAndAdd(
                                        List.of(lineItem(1)), LABEL_ID, FROM, saleEntity))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No production run found");
    }

    private void givenRelease() {
        when(releaseQueryApi.findById(RELEASE_ID))
                .thenReturn(
                        Optional.of(
                                new Release(
                                        RELEASE_ID,
                                        "Test Album",
                                        LocalDate.now(),
                                        LABEL_ID,
                                        List.of(),
                                        List.of(),
                                        Set.of(Format.VINYL))));
    }

    private void givenReleaseWithId(long releaseId) {
        when(releaseQueryApi.findById(releaseId))
                .thenReturn(
                        Optional.of(
                                new Release(
                                        releaseId,
                                        "Other Album",
                                        LocalDate.now(),
                                        LABEL_ID,
                                        List.of(),
                                        List.of(),
                                        Set.of(Format.VINYL))));
    }

    private SaleLineItemInput lineItemFor(long releaseId, int quantity) {
        return new SaleLineItemInput(
                releaseId, Format.VINYL, quantity, new Money(new BigDecimal("15.00"), "EUR"));
    }

    private void givenLedger(RunStock... pressings) {
        when(productionRunQueryApi.lockedLedgerAt(RELEASE_ID, Format.VINYL, FROM))
                .thenReturn(StockLedger.of(List.of(pressings)));
    }

    private RunStock pressing(long runId, String manufacturedOn, int onHand) {
        return new RunStock(runId, LocalDate.parse(manufacturedOn), onHand);
    }

    private SaleLineItemInput lineItem(int quantity) {
        return new SaleLineItemInput(
                RELEASE_ID, Format.VINYL, quantity, new Money(new BigDecimal("15.00"), "EUR"));
    }
}
