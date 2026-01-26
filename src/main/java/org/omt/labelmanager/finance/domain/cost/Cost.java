package org.omt.labelmanager.finance.domain.cost;

import java.time.LocalDate;
import org.omt.labelmanager.finance.domain.shared.Money;
import org.omt.labelmanager.finance.cost.persistence.CostEntity;

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

    public static Cost fromEntity(CostEntity entity) {
        String currency = entity.getCurrency();
        return new Cost(
                entity.getId(),
                new Money(entity.getNetAmount(), currency),
                new VatAmount(
                        new Money(entity.getVatAmount(), currency),
                        entity.getVatRate()
                ),
                new Money(entity.getGrossAmount(), currency),
                entity.getCostType(),
                entity.getIncurredOn(),
                entity.getDescription(),
                entity.getOwner().toCostOwner(),
                entity.getDocumentReference()
        );
    }
}
