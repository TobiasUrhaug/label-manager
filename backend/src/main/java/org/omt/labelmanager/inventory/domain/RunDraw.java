package org.omt.labelmanager.inventory.domain;

/**
 * Part of a draw, attributed to the production run it came out of.
 *
 * <p>A single sale line item can produce several of these when one pressing cannot cover it — which
 * is why callers get a list back rather than one run id.
 *
 * @param productionRunId the run drawn from
 * @param quantity units taken from that run, always positive
 */
public record RunDraw(Long productionRunId, int quantity) {

    public RunDraw {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "A draw of nothing is not a draw: run " + productionRunId + " got " + quantity);
        }
    }
}
