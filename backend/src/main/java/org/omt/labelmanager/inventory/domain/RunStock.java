package org.omt.labelmanager.inventory.domain;

import java.time.LocalDate;

/**
 * How much of one production run is sitting at a location.
 *
 * @param productionRunId the run
 * @param manufacturedOn when it was pressed — what "first in" means for FIFO
 * @param onHand units at the location, never negative
 */
public record RunStock(Long productionRunId, LocalDate manufacturedOn, int onHand) {

    public RunStock {
        if (onHand < 0) {
            throw new IllegalArgumentException(
                    "A location cannot hold a negative quantity: run "
                            + productionRunId
                            + " has "
                            + onHand);
        }
    }
}
