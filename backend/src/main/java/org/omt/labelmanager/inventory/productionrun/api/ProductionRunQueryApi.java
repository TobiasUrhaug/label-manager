package org.omt.labelmanager.inventory.productionrun.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.omt.labelmanager.inventory.InventoryLocation;
import org.omt.labelmanager.inventory.domain.StockLedger;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;
import org.omt.labelmanager.shared.Format;

public interface ProductionRunQueryApi {

    Optional<ProductionRun> findById(Long productionRunId);

    List<ProductionRun> findByReleaseId(Long releaseId);

    /**
     * Every production run for any of these releases, in one query.
     *
     * @param releaseIds the release ids; an empty collection returns an empty list
     * @return the matching production runs
     */
    List<ProductionRun> findByReleaseIds(Collection<Long> releaseIds);

    /**
     * What a location holds of a release in a given format, pressing by pressing, ready to draw
     * from — with those pressings locked until the caller's transaction ends.
     *
     * <p>This is how callers decide which pressing stock comes out of. It replaces picking the most
     * recent run and hoping it covers the quantity, which ignored every earlier pressing that still
     * had stock.
     *
     * <p>Locking is not optional, which is why there is no unlocked variant to reach for. Checking
     * stock and recording the movement that consumes it are two statements, so an unlocked reader
     * that then writes lets two concurrent sales of the last units both succeed. A caller that only
     * wants to display stock wants {@code InventoryMovementQueryApi.balancesFor}, which does not
     * pretend to be drawable-from.
     *
     * @param releaseId the release
     * @param format the format — a release's vinyl and CD pressings are separate stock
     * @param location where the stock is being taken from
     * @return the ledger; empty if the release has no pressings in that format
     * @throws org.springframework.transaction.IllegalTransactionStateException if called outside a
     *     transaction, since a lock released on return is no lock at all
     */
    StockLedger lockedLedgerAt(Long releaseId, Format format, InventoryLocation location);
}
