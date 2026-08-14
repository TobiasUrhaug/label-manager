package org.omt.labelmanager.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.inventory.InsufficientInventoryException;

class StockLedgerTest {

    @Test
    void onHandSumsEveryRun() {
        var ledger = ledgerOf(run(1L, "2024-01-01", 120), run(2L, "2025-01-01", 80));

        assertThat(ledger.onHand()).isEqualTo(200);
    }

    @Test
    void onHandIsZeroWhenNothingHasBeenPressed() {
        assertThat(ledgerOf().onHand()).isZero();
    }

    @Test
    void drawsFromTheOldestPressingFirst() {
        var ledger = ledgerOf(run(2L, "2025-06-01", 100), run(1L, "2024-01-01", 100));

        assertThat(ledger.drawFifo(60)).containsExactly(new RunDraw(1L, 60));
    }

    @Test
    void splitsAcrossPressingsWhenTheOldestIsShort() {
        var ledger =
                ledgerOf(
                        run(1L, "2024-01-01", 50),
                        run(2L, "2025-01-01", 30),
                        run(3L, "2026-01-01", 100));

        assertThat(ledger.drawFifo(95))
                .containsExactly(new RunDraw(1L, 50), new RunDraw(2L, 30), new RunDraw(3L, 15));
    }

    @Test
    void drainsEveryPressingWhenTheDrawIsTheWholeStock() {
        var ledger = ledgerOf(run(1L, "2024-01-01", 40), run(2L, "2025-01-01", 60));

        assertThat(ledger.drawFifo(100)).containsExactly(new RunDraw(1L, 40), new RunDraw(2L, 60));
    }

    @Test
    void skipsPressingsThatAreSoldOut() {
        var ledger =
                ledgerOf(
                        run(1L, "2024-01-01", 0),
                        run(2L, "2025-01-01", 70),
                        run(3L, "2026-01-01", 5));

        assertThat(ledger.drawFifo(70)).containsExactly(new RunDraw(2L, 70));
    }

    /** Two pressings on the same day still need a stable order, or the split is arbitrary. */
    @Test
    void ordersPressingsFromTheSameDayByRunId() {
        var ledger = ledgerOf(run(7L, "2024-01-01", 10), run(3L, "2024-01-01", 10));

        assertThat(ledger.drawFifo(15)).containsExactly(new RunDraw(3L, 10), new RunDraw(7L, 5));
    }

    @Test
    void refusesToDrawMoreThanIsOnHand() {
        var ledger = ledgerOf(run(1L, "2024-01-01", 40), run(2L, "2025-01-01", 30));

        assertThatThrownBy(() -> ledger.drawFifo(71))
                .isInstanceOfSatisfying(
                        InsufficientInventoryException.class,
                        e -> {
                            assertThat(e.getRequested()).isEqualTo(71);
                            assertThat(e.getAvailable()).isEqualTo(70);
                        });
    }

    @Test
    void refusesADrawOfNothing() {
        var ledger = ledgerOf(run(1L, "2024-01-01", 40));

        assertThatThrownBy(() -> ledger.drawFifo(0)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * A sale validates every line item before recording any movement, so two line items for the
     * same release must not both draw against the opening balances.
     */
    @Test
    void subtractingADrawLeavesTheStockTheNextDrawSees() {
        var ledger = ledgerOf(run(1L, "2024-01-01", 50), run(2L, "2025-01-01", 40));

        var remaining = ledger.minus(ledger.drawFifo(50));

        assertThat(remaining.onHand()).isEqualTo(40);
        assertThat(remaining.drawFifo(40)).containsExactly(new RunDraw(2L, 40));
    }

    @Test
    void subtractingEverythingLeavesNothingToDraw() {
        var ledger = ledgerOf(run(1L, "2024-01-01", 20), run(2L, "2025-01-01", 10));

        var remaining = ledger.minus(ledger.drawFifo(30));

        assertThat(remaining.onHand()).isZero();
        assertThatThrownBy(() -> remaining.drawFifo(1))
                .isInstanceOf(InsufficientInventoryException.class);
    }

    @Test
    void refusesToSubtractADrawFromARunItDoesNotHold() {
        var ledger = ledgerOf(run(1L, "2024-01-01", 20));

        assertThatThrownBy(() -> ledger.minus(List.of(new RunDraw(99L, 5))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    @Test
    void refusesToSubtractMoreThanARunHolds() {
        var ledger = ledgerOf(run(1L, "2024-01-01", 20));

        assertThatThrownBy(() -> ledger.minus(List.of(new RunDraw(1L, 21))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** The canonical constructor is public on a record, so it has to sort too. */
    @Test
    void sortsEvenWhenBuiltThroughTheCanonicalConstructor() {
        var ledger = new StockLedger(List.of(run(2L, "2025-01-01", 10), run(1L, "2024-01-01", 10)));

        assertThat(ledger.drawFifo(10)).containsExactly(new RunDraw(1L, 10));
    }

    @Test
    void refusesTwoEntriesForTheSameRun() {
        assertThatThrownBy(
                        () ->
                                StockLedger.of(
                                        List.of(
                                                run(1L, "2024-01-01", 10),
                                                run(1L, "2024-01-01", 5))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refusesAPressingWithNegativeStock() {
        assertThatThrownBy(() -> run(1L, "2024-01-01", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void returnsADrawTheCallerCannotModify() {
        var ledger = ledgerOf(run(1L, "2024-01-01", 10));

        var draws = ledger.drawFifo(5);

        assertThatThrownBy(() -> draws.add(new RunDraw(2L, 1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private StockLedger ledgerOf(RunStock... runs) {
        return StockLedger.of(List.of(runs));
    }

    private RunStock run(Long id, String manufacturedOn, int onHand) {
        return new RunStock(id, LocalDate.parse(manufacturedOn), onHand);
    }
}
