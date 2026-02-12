package org.omt.labelmanager.sales.sale.application;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.inventory.allocation.api.AllocationCommandApi;
import org.omt.labelmanager.inventory.api.InventoryMovementCommandApi;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.sales.sale.domain.Sale;
import org.omt.labelmanager.sales.sale.domain.SaleLineItem;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;
import org.omt.labelmanager.sales.sale.infrastructure.SaleEntity;
import org.omt.labelmanager.sales.sale.infrastructure.SaleLineItemEntity;
import org.omt.labelmanager.sales.sale.infrastructure.SaleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
class RegisterSaleUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(RegisterSaleUseCase.class);

    private final SaleRepository saleRepository;
    private final LabelQueryApi labelQueryApi;
    private final ReleaseQueryApi releaseQueryApi;
    private final DistributorQueryApi distributorQueryApi;
    private final ProductionRunQueryApi productionRunQueryApi;
    private final AllocationCommandApi allocationCommandApi;
    private final InventoryMovementCommandApi inventoryMovementCommandApi;

    RegisterSaleUseCase(
            SaleRepository saleRepository,
            LabelQueryApi labelQueryApi,
            ReleaseQueryApi releaseQueryApi,
            DistributorQueryApi distributorQueryApi,
            ProductionRunQueryApi productionRunQueryApi,
            AllocationCommandApi allocationCommandApi,
            InventoryMovementCommandApi inventoryMovementCommandApi
    ) {
        this.saleRepository = saleRepository;
        this.labelQueryApi = labelQueryApi;
        this.releaseQueryApi = releaseQueryApi;
        this.distributorQueryApi = distributorQueryApi;
        this.productionRunQueryApi = productionRunQueryApi;
        this.allocationCommandApi = allocationCommandApi;
        this.inventoryMovementCommandApi = inventoryMovementCommandApi;
    }

    @Transactional
    public Sale execute(

            Long labelId,
            LocalDate saleDate,
            ChannelType channel,
            String notes,
            List<SaleLineItemInput> lineItems
    ) {
        log.info("Registering sale for label {} with {} line items",
                labelId, lineItems.size());

        // 1. Validate label exists
        if (!labelQueryApi.exists(labelId)) {
            throw new EntityNotFoundException("Label not found: " + labelId);
        }

        // 2. Validate releases and find DIRECT distributor
        var directDistributor = distributorQueryApi
                .findByLabelIdAndChannelType(labelId, ChannelType.DIRECT)
                .orElseThrow(() -> new EntityNotFoundException(
                        "DIRECT distributor not found for label: " + labelId
                ));

        // 3. Create sale entity
        var saleEntity = new SaleEntity(
                labelId,
                saleDate,
                channel,
                notes,
                lineItems.getFirst().unitPrice().currency()
        );

        // 4. Process each line item
        for (var lineItemInput : lineItems) {
            validateAndProcessLineItem(
                    lineItemInput,
                    labelId,
                    directDistributor.id(),
                    saleEntity
            );
        }

        // 5. Save sale
        var savedSale = saleRepository.save(saleEntity);

        log.info("Sale registered successfully with ID {} and total amount {}",
                savedSale.getId(), savedSale.getTotalAmount());

        return convertToSale(savedSale);
    }

    private void validateAndProcessLineItem(
            SaleLineItemInput lineItemInput,
            Long labelId,
            Long directDistributorId,
            SaleEntity saleEntity
    ) {
        // Validate release exists and belongs to label
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

        // Find most recent production run for this release/format
        var productionRun = productionRunQueryApi
                .findMostRecent(lineItemInput.releaseId(), lineItemInput.format())
                .orElseThrow(() -> new IllegalStateException(
                        "No production run found for release '" + release.name()
                                + "' (" + lineItemInput.format() + "). "
                                + "Please create a production run for this release and format "
                                + "before registering sales."
                ));

        // Create line item entity
        var lineItemEntity = new SaleLineItemEntity(
                lineItemInput.releaseId(),
                lineItemInput.format(),
                lineItemInput.quantity(),
                lineItemInput.unitPrice().amount(),
                lineItemInput.unitPrice().currency()
        );
        saleEntity.addLineItem(lineItemEntity);

        // Reduce allocation (validates sufficient inventory)
        try {
            allocationCommandApi.reduceAllocation(
                    productionRun.id(),
                    directDistributorId,
                    lineItemInput.quantity()
            );
        } catch (EntityNotFoundException e) {
            throw new IllegalStateException(
                    "No inventory allocated for release '" + release.name()
                            + "' (" + lineItemInput.format() + "). "
                            + "Please allocate inventory from the production run to your "
                            + "DIRECT sales channel before registering sales."
            );
        }

        // Create inventory movement record
        inventoryMovementCommandApi.recordMovement(
                productionRun.id(),
                directDistributorId,
                -lineItemInput.quantity(),  // Negative for outbound
                MovementType.SALE,
                null  // Reference ID could be set to sale ID if needed
        );

        log.debug("Processed line item: release={}, format={}, quantity={}",
                lineItemInput.releaseId(),
                lineItemInput.format(),
                lineItemInput.quantity());
    }

    private Sale convertToSale(SaleEntity entity) {
        List<SaleLineItem> lineItems = entity.getLineItems().stream()
                .map(item -> new SaleLineItem(
                        item.getId(),
                        item.getReleaseId(),
                        item.getFormat(),
                        item.getQuantity(),
                        new org.omt.labelmanager.finance.domain.shared.Money(
                                item.getUnitPrice(),
                                item.getCurrency()
                        ),
                        new org.omt.labelmanager.finance.domain.shared.Money(
                                item.getLineTotal(),
                                item.getCurrency()
                        )
                ))
                .toList();

        return new Sale(
                entity.getId(),
                entity.getLabelId(),
                entity.getSaleDate(),
                entity.getChannel(),
                entity.getNotes(),
                lineItems,
                new org.omt.labelmanager.finance.domain.shared.Money(
                        entity.getTotalAmount(),
                        entity.getCurrency()
                )
        );
    }
}
