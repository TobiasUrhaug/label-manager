package org.omt.labelmanager.finance.cost;

import org.omt.labelmanager.finance.cost.domain.Cost;
import org.omt.labelmanager.finance.cost.domain.VatAmount;
import org.omt.labelmanager.finance.cost.infrastructure.CostEntity;
import org.omt.labelmanager.finance.domain.shared.Money;

public class CostMapper {

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
