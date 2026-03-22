package org.omt.labelmanager.sales.sale.application;

import java.util.List;
import org.omt.labelmanager.finance.domain.shared.Money;
import org.omt.labelmanager.sales.sale.domain.Sale;
import org.omt.labelmanager.sales.sale.domain.SaleLineItem;
import org.omt.labelmanager.sales.sale.infrastructure.SaleEntity;
import org.springframework.stereotype.Service;

/**
 * Converts a {@link SaleEntity} to the public {@link Sale} domain record.
 * Shared by use cases to avoid duplicating the mapping logic.
 */
@Service
class SaleConverter {

    Sale toSale(SaleEntity entity) {
        List<SaleLineItem> lineItems = entity.getLineItems().stream()
                .map(item -> new SaleLineItem(
                        item.getId(),
                        item.getReleaseId(),
                        item.getFormat(),
                        item.getQuantity(),
                        new Money(item.getUnitPrice(), item.getCurrency()),
                        new Money(item.getLineTotal(), item.getCurrency())
                ))
                .toList();

        return new Sale(
                entity.getId(),
                entity.getLabelId(),
                entity.getDistributorId(),
                entity.getSaleDate(),
                entity.getChannel(),
                entity.getNotes(),
                lineItems,
                new Money(entity.getTotalAmount(), entity.getCurrency())
        );
    }
}
