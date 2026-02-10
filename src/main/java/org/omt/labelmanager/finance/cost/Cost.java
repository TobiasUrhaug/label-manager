package org.omt.labelmanager.finance.cost;

import org.omt.labelmanager.finance.cost.persistence.CostEntity;
import org.omt.labelmanager.finance.domain.shared.Money;

import java.time.LocalDate;

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
        String documentStorageKey
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
                entity.getDocumentReference(),
                entity.getDocumentStorageKey()
        );
    }
}
