package org.omt.labelmanager.sales.distributorreturn.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.omt.labelmanager.sales.distributorreturn.domain.ReturnLineItemInput;
import org.omt.labelmanager.sales.distributorreturn.infrastructure.DistributorReturnEntity;
import org.omt.labelmanager.shared.Format;

@ExtendWith(MockitoExtension.class)
class ReturnLineItemProcessorTest {

    private static final long LABEL_ID = 1L;
    private static final long RELEASE_ID = 10L;
    private static final long FIRST_PRESSING = 100L;
    private static final long REPRESS = 101L;
    private static final long DISTRIBUTOR_ID = 200L;
    private static final InventoryLocation FROM = InventoryLocation.distributor(DISTRIBUTOR_ID);

    @Mock private ReleaseQueryApi releaseQueryApi;

    @Mock private ProductionRunQueryApi productionRunQueryApi;

    @Mock private DistributorReturnEntity returnEntity;

    private ReturnLineItemProcessor subject;

    @BeforeEach
    void setUp() {
        subject = new ReturnLineItemProcessor(releaseQueryApi, productionRunQueryApi);
    }

    @Test
    void validateAndAdd_refusesMoreThanTheDistributorHolds() {
        givenRelease();
        givenLedger(pressing(FIRST_PRESSING, "2024-01-01", 10));

        assertThatThrownBy(
                        () ->
                                subject.validateAndAdd(
                                        List.of(lineItem(11)), LABEL_ID, FROM, returnEntity))
                .isInstanceOf(InsufficientInventoryException.class);
    }

    @Test
    void validateAndAdd_addsLineItemAndReportsWhichPressingComesBack() {
        givenRelease();
        givenLedger(pressing(FIRST_PRESSING, "2024-01-01", 40));

        var draws = subject.validateAndAdd(List.of(lineItem(10)), LABEL_ID, FROM, returnEntity);

        assertThat(draws).containsExactly(new RunDraw(FIRST_PRESSING, 10));
        verify(returnEntity).addLineItem(org.mockito.ArgumentMatchers.any());
    }

    /**
     * A return credits the oldest pressing the distributor still holds, as a sale draws from it.
     */
    @Test
    void validateAndAdd_splitsAcrossPressingsOldestFirst() {
        givenRelease();
        givenLedger(
                pressing(REPRESS, "2026-01-01", 100), pressing(FIRST_PRESSING, "2024-01-01", 15));

        var draws = subject.validateAndAdd(List.of(lineItem(40)), LABEL_ID, FROM, returnEntity);

        assertThat(draws)
                .containsExactly(new RunDraw(FIRST_PRESSING, 15), new RunDraw(REPRESS, 25));
    }

    @Test
    void validateAndAdd_takesEarlierLineItemsOffTheStockLaterOnesSee() {
        givenRelease();
        givenLedger(
                pressing(FIRST_PRESSING, "2024-01-01", 40), pressing(REPRESS, "2026-01-01", 60));

        var draws =
                subject.validateAndAdd(
                        List.of(lineItem(30), lineItem(30)), LABEL_ID, FROM, returnEntity);

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
                                        returnEntity))
                .isInstanceOf(InsufficientInventoryException.class);
    }

    /** Vinyl and CD are separate stock, so one must not be drawn against the other's ledger. */
    @Test
    void validateAndAdd_keepsFormatsApart() {
        givenRelease();
        givenLedger(pressing(FIRST_PRESSING, "2024-01-01", 20));
        when(productionRunQueryApi.ledgerAt(RELEASE_ID, Format.CD, FROM))
                .thenReturn(StockLedger.of(List.of(pressing(REPRESS, "2025-01-01", 20))));

        var draws =
                subject.validateAndAdd(
                        List.of(lineItem(20), cdLineItem(20)), LABEL_ID, FROM, returnEntity);

        assertThat(draws)
                .containsExactly(new RunDraw(FIRST_PRESSING, 20), new RunDraw(REPRESS, 20));
    }

    @Test
    void validateAndAdd_refusesAReleaseThatHasNeverBeenPressed() {
        givenRelease();
        givenLedger();

        assertThatThrownBy(
                        () ->
                                subject.validateAndAdd(
                                        List.of(lineItem(1)), LABEL_ID, FROM, returnEntity))
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

    private void givenLedger(RunStock... pressings) {
        when(productionRunQueryApi.ledgerAt(RELEASE_ID, Format.VINYL, FROM))
                .thenReturn(StockLedger.of(List.of(pressings)));
    }

    private RunStock pressing(long runId, String manufacturedOn, int onHand) {
        return new RunStock(runId, LocalDate.parse(manufacturedOn), onHand);
    }

    private ReturnLineItemInput lineItem(int quantity) {
        return new ReturnLineItemInput(RELEASE_ID, Format.VINYL, quantity);
    }

    private ReturnLineItemInput cdLineItem(int quantity) {
        return new ReturnLineItemInput(RELEASE_ID, Format.CD, quantity);
    }
}
