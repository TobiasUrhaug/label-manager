package org.omt.labelmanager.sales.distributor_return.application;

import java.util.List;
import org.omt.labelmanager.sales.distributor_return.domain.DistributorReturn;
import org.omt.labelmanager.sales.distributor_return.domain.ReturnLineItem;
import org.omt.labelmanager.sales.distributor_return.infrastructure.DistributorReturnEntity;
import org.springframework.stereotype.Service;

/**
 * Converts a {@link DistributorReturnEntity} to the public {@link DistributorReturn} domain record.
 * Shared by use cases to avoid duplicating the mapping logic.
 */
@Service
class ReturnConverter {

    DistributorReturn toReturn(DistributorReturnEntity entity) {
        List<ReturnLineItem> lineItems = entity.getLineItems().stream()
                .map(item -> new ReturnLineItem(
                        item.getId(),
                        entity.getId(),
                        item.getReleaseId(),
                        item.getFormat(),
                        item.getQuantity()
                ))
                .toList();

        return new DistributorReturn(
                entity.getId(),
                entity.getLabelId(),
                entity.getDistributorId(),
                entity.getReturnDate(),
                entity.getNotes(),
                lineItems,
                entity.getCreatedAt()
        );
    }
}
