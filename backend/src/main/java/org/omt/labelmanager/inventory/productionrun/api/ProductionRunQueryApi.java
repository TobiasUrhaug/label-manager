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
     * from.
     *
     * <p>This is how callers decide which pressing stock comes out of. It replaces picking the most
     * recent run and hoping it covers the quantity, which ignored every earlier pressing that still
     * had stock.
     *
     * @param releaseId the release
     * @param format the format — a release's vinyl and CD pressings are separate stock
     * @param location where the stock is being taken from
     * @return the ledger; empty if the release has no pressings in that format
     */
    StockLedger ledgerAt(Long releaseId, Format format, InventoryLocation location);
}
