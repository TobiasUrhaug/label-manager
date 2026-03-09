package org.omt.labelmanager.sales.sale.application;

import jakarta.persistence.EntityNotFoundException;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.inventory.allocation.api.AllocationQueryApi;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;
import org.omt.labelmanager.sales.sale.infrastructure.SaleEntity;
import org.omt.labelmanager.sales.sale.infrastructure.SaleLineItemEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Validates a sale line item against business rules and adds it to the sale entity.
 * Shared by {@link RegisterSaleUseCase} and {@link UpdateSaleUseCase} to avoid
 * duplicating inventory and release validation logic.
 */
@Service
class SaleLineItemProcessor {

    private static final Logger log = LoggerFactory.getLogger(SaleLineItemProcessor.class);

    private final ReleaseQueryApi releaseQueryApi;
    private final ProductionRunQueryApi productionRunQueryApi;
    private final AllocationQueryApi allocationQueryApi;
    private final InventoryMovementQueryApi inventoryMovementQueryApi;

    SaleLineItemProcessor(
            ReleaseQueryApi releaseQueryApi,
            ProductionRunQueryApi productionRunQueryApi,
            AllocationQueryApi allocationQueryApi,
            InventoryMovementQueryApi inventoryMovementQueryApi
    ) {
        this.releaseQueryApi = releaseQueryApi;
        this.productionRunQueryApi = productionRunQueryApi;
        this.allocationQueryApi = allocationQueryApi;
        this.inventoryMovementQueryApi = inventoryMovementQueryApi;
    }

    /**
     * Validates the line item against inventory and allocation rules, then adds it to
     * the sale entity.
     *
     * @param lineItemInput the line item data from the form
     * @param labelId the label the sale belongs to (for release ownership check)
     * @param distributorId the distributor the sale is attributed to
     * @param distributorName the distributor name (used in error messages)
     * @param saleEntity the sale entity to add the line item to
     * @return the production run ID (needed by the caller to record inventory movements)
     */
    Long validateAndAdd(
            SaleLineItemInput lineItemInput,
            Long labelId,
            Long distributorId,
            String distributorName,
            SaleEntity saleEntity
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
                                + "before registering sales."
                ));

        boolean hasAllocation = allocationQueryApi
                .getAllocationsForProductionRun(productionRun.id())
                .stream()
                .anyMatch(a -> a.distributorId().equals(distributorId));

        if (!hasAllocation) {
            throw new IllegalStateException(
                    "No inventory allocated for release '" + release.name()
                            + "' (" + lineItemInput.format() + ") "
                            + "to distributor '" + distributorName + "'. "
                            + "Please allocate inventory from the production run "
                            + "before registering sales."
            );
        }

        int available = inventoryMovementQueryApi.getCurrentInventory(
                productionRun.id(), distributorId
        );
        if (available < lineItemInput.quantity()) {
            throw new InsufficientInventoryException(lineItemInput.quantity(), available);
        }

        saleEntity.addLineItem(new SaleLineItemEntity(
                lineItemInput.releaseId(),
                lineItemInput.format(),
                lineItemInput.quantity(),
                lineItemInput.unitPrice().amount(),
                lineItemInput.unitPrice().currency()
        ));

        log.debug("Processed line item: release={}, format={}, quantity={}",
                lineItemInput.releaseId(),
                lineItemInput.format(),
                lineItemInput.quantity());

        return productionRun.id();
    }
}
