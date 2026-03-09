package org.omt.labelmanager.sales.distributor_return.application;

import jakarta.persistence.EntityNotFoundException;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.sales.distributor_return.domain.ReturnLineItemInput;
import org.omt.labelmanager.sales.distributor_return.infrastructure.DistributorReturnEntity;
import org.omt.labelmanager.sales.distributor_return.infrastructure.ReturnLineItemEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Validates a return line item against current inventory and adds it to the return entity.
 * Shared by {@link RegisterReturnUseCase} and {@link UpdateReturnUseCase} to avoid
 * duplicating inventory and release validation logic.
 */
@Service
class ReturnLineItemProcessor {

    private static final Logger log = LoggerFactory.getLogger(ReturnLineItemProcessor.class);

    private final ReleaseQueryApi releaseQueryApi;
    private final ProductionRunQueryApi productionRunQueryApi;
    private final InventoryMovementQueryApi inventoryMovementQueryApi;

    ReturnLineItemProcessor(
            ReleaseQueryApi releaseQueryApi,
            ProductionRunQueryApi productionRunQueryApi,
            InventoryMovementQueryApi inventoryMovementQueryApi
    ) {
        this.releaseQueryApi = releaseQueryApi;
        this.productionRunQueryApi = productionRunQueryApi;
        this.inventoryMovementQueryApi = inventoryMovementQueryApi;
    }

    /**
     * Validates that the distributor holds enough current inventory for the line item,
     * then adds it to the return entity.
     *
     * @param lineItemInput  the line item data from the form
     * @param labelId        the label the return belongs to (for release ownership check)
     * @param distributorId  the distributor returning the inventory
     * @param returnEntity   the return entity to add the line item to
     * @return the production run ID (needed by the caller to record inventory movements)
     */
    Long validateAndAdd(
            ReturnLineItemInput lineItemInput,
            Long labelId,
            Long distributorId,
            DistributorReturnEntity returnEntity
    ) {
        var release = releaseQueryApi.findById(lineItemInput.releaseId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Release not found: " + lineItemInput.releaseId()
                ));

        if (!release.labelId().equals(labelId)) {
            throw new IllegalArgumentException(
                    "Release " + lineItemInput.releaseId()
                            + " does not belong to label " + labelId
            );
        }

        var productionRun = productionRunQueryApi
                .findMostRecent(lineItemInput.releaseId(), lineItemInput.format())
                .orElseThrow(() -> new IllegalStateException(
                        "No production run found for release '" + release.name()
                                + "' (" + lineItemInput.format() + "). "
                                + "Please create a production run for this release and format "
                                + "before registering returns."
                ));

        int available = inventoryMovementQueryApi.getCurrentInventory(
                productionRun.id(), distributorId
        );
        if (available < lineItemInput.quantity()) {
            throw new InsufficientInventoryException(lineItemInput.quantity(), available);
        }

        returnEntity.addLineItem(new ReturnLineItemEntity(
                lineItemInput.releaseId(),
                lineItemInput.format(),
                lineItemInput.quantity()
        ));

        log.debug("Processed return line item: release={}, format={}, quantity={}",
                lineItemInput.releaseId(),
                lineItemInput.format(),
                lineItemInput.quantity());

        return productionRun.id();
    }
}
