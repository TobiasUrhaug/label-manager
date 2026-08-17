package org.omt.labelmanager.inventory.inventorymovement.api;

import org.omt.labelmanager.inventory.InventoryLocation;

/**
 * How much of one production run a single location holds, summed from the ledger.
 *
 * @param productionRunId the run
 * @param location where the units are
 * @param onHand units in − units out; zero-balance locations are not reported
 */
public record LocationBalance(Long productionRunId, InventoryLocation location, int onHand) {

    /**
     * Whether this balance is the one for {@code location} — the whole location, id included.
     *
     * <p>Matching on the type alone would fold a hypothetical {@code WAREHOUSE} row carrying an id
     * into the real warehouse balance, and callers that did it differently would report different
     * stock for the same run.
     */
    public boolean isAt(InventoryLocation location) {
        return this.location.equals(location);
    }
}
