package org.omt.labelmanager.catalog.release.api;

import java.time.LocalDate;
import org.omt.labelmanager.finance.domain.shared.Money;

/**
 * View model representing a single sale attributed to this release,
 * enriched with the distributor name for display on the release detail page.
 *
 * <p>{@code totalRevenue} may be {@code null} when no unit price has been recorded.</p>
 */
public record ReleaseSaleView(
        Long saleId,
        LocalDate saleDate,
        String distributorName,
        int totalUnits,
        Money totalRevenue
) {
}
