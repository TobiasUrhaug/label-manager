package org.omt.labelmanager.finance.cost.domain;

import java.time.LocalDate;
import org.omt.labelmanager.finance.domain.shared.Money;

public record Cost(
        Long id,
        Money netAmount,
        VatAmount vat,
        Money grossAmount,
        CostType type,
        LocalDate incurredOn,
        String description,
        CostOwner owner,
        String documentReference,
        String documentStorageKey) {}
