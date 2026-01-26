package org.omt.labelmanager.cost;

import java.time.LocalDate;
import org.omt.labelmanager.common.Money;

public record Cost(
        Long id,
        Money netAmount,
        VatAmount vat,
        Money grossAmount,
        CostType type,
        LocalDate incurredOn,
        String description,
        CostOwner owner,
        String documentReference
) {
}
