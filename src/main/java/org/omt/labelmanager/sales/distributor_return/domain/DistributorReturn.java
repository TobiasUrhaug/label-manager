package org.omt.labelmanager.sales.distributor_return.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Represents a return of physical inventory from a distributor back to the label's warehouse.
 * Each return belongs to a label and is attributed to a specific distributor.
 */
public record DistributorReturn(
        Long id,
        Long labelId,
        Long distributorId,
        LocalDate returnDate,
        String notes,
        List<ReturnLineItem> lineItems,
        Instant createdAt
) {
}
