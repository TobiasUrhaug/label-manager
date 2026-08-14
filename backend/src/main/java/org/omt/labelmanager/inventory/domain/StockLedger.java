package org.omt.labelmanager.inventory.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.omt.labelmanager.inventory.InsufficientInventoryException;

/**
 * What one location holds of a release, pressing by pressing, and the rule for taking stock out of
 * it.
 *
 * <p>This is the one place that answers "how much is there" and "which pressing does this come
 * from". Both questions used to be answered at the call site: stock as {@code run.quantity() +} a
 * movement sum, and the pressing as {@code findMostRecent}, which silently ignored every earlier
 * run that still had stock.
 *
 * <p>Balances are supplied by the caller, already summed from the ledger. Nothing here touches a
 * database, so the rule is testable without one.
 */
public record StockLedger(List<RunStock> runs) {

    /** Oldest pressing first — "first in" is the manufacturing date, with the id as tie-break. */
    private static final Comparator<RunStock> FIRST_IN =
            Comparator.comparing(RunStock::manufacturedOn).thenComparing(RunStock::productionRunId);

    /**
     * Sorts on the way in, so there is no way to build a ledger that draws in the wrong order —
     * including through the canonical constructor, which a record makes public whether or not a
     * factory exists.
     */
    public StockLedger {
        if (runs == null) {
            throw new IllegalArgumentException(
                    "A ledger needs a list of pressings, even an empty one");
        }
        long distinctRuns = runs.stream().map(RunStock::productionRunId).distinct().count();
        if (distinctRuns != runs.size()) {
            throw new IllegalArgumentException(
                    "A production run may appear once in a ledger; balances are already summed");
        }
        runs = runs.stream().sorted(FIRST_IN).toList();
    }

    public static StockLedger of(List<RunStock> runs) {
        return new StockLedger(runs);
    }

    /** Everything this location holds of the release, across every pressing. */
    public int onHand() {
        return runs.stream().mapToInt(RunStock::onHand).sum();
    }

    /**
     * Takes {@code quantity} units out of the oldest pressings first, splitting the draw when one
     * pressing cannot cover it.
     *
     * @return the split, one entry per production run drawn from, oldest first
     * @throws InsufficientInventoryException if the location does not hold that many units
     */
    public List<RunDraw> drawFifo(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("A draw must be for at least one unit: " + quantity);
        }

        int onHand = onHand();
        if (quantity > onHand) {
            throw new InsufficientInventoryException(quantity, onHand);
        }

        List<RunDraw> draws = new ArrayList<>();
        int outstanding = quantity;
        for (RunStock run : runs) {
            if (outstanding == 0) {
                break;
            }
            int taken = Math.min(run.onHand(), outstanding);
            if (taken > 0) {
                draws.add(new RunDraw(run.productionRunId(), taken));
                outstanding -= taken;
            }
        }
        return List.copyOf(draws);
    }

    /**
     * The ledger as it stands once {@code draws} have been taken out of it.
     *
     * <p>Needed because a sale is validated line item by line item before any movement is recorded:
     * two line items for the same release and format would otherwise each draw against the same
     * starting balances and together take more than exists. Each draw is subtracted from the ledger
     * the next line item sees.
     */
    public StockLedger minus(List<RunDraw> draws) {
        Map<Long, Integer> drawnByRun = new HashMap<>();
        for (RunDraw draw : draws) {
            drawnByRun.merge(draw.productionRunId(), draw.quantity(), Integer::sum);
        }

        List<RunStock> remaining = new ArrayList<>();
        for (RunStock run : runs) {
            Integer drawn = drawnByRun.remove(run.productionRunId());
            int taken = drawn == null ? 0 : drawn;
            if (taken > run.onHand()) {
                throw new IllegalArgumentException(
                        "Draw of "
                                + taken
                                + " exceeds the "
                                + run.onHand()
                                + " this ledger holds of production run "
                                + run.productionRunId());
            }
            remaining.add(
                    new RunStock(
                            run.productionRunId(), run.manufacturedOn(), run.onHand() - taken));
        }
        if (!drawnByRun.isEmpty()) {
            throw new IllegalArgumentException(
                    "Draw names production runs this ledger does not hold: " + drawnByRun.keySet());
        }
        return new StockLedger(remaining);
    }
}
