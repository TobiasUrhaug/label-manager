package org.omt.labelmanager.sales.sale.application;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.inventory.allocation.api.AllocationQueryApi;
import org.omt.labelmanager.inventory.domain.LocationType;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementCommandApi;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
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

@Service
class RegisterSaleUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(RegisterSaleUseCase.class);

    private final SaleRepository saleRepository;
    private final LabelQueryApi labelQueryApi;
    private final ReleaseQueryApi releaseQueryApi;
    private final DistributorQueryApi distributorQueryApi;
    private final ProductionRunQueryApi productionRunQueryApi;
    private final AllocationQueryApi allocationQueryApi;
    private final InventoryMovementQueryApi inventoryMovementQueryApi;
    private final InventoryMovementCommandApi inventoryMovementCommandApi;

    RegisterSaleUseCase(
            SaleRepository saleRepository,
            LabelQueryApi labelQueryApi,
            ReleaseQueryApi releaseQueryApi,
            DistributorQueryApi distributorQueryApi,
            ProductionRunQueryApi productionRunQueryApi,
            AllocationQueryApi allocationQueryApi,
            InventoryMovementQueryApi inventoryMovementQueryApi,
            InventoryMovementCommandApi inventoryMovementCommandApi
    ) {
        this.saleRepository = saleRepository;
        this.labelQueryApi = labelQueryApi;
        this.releaseQueryApi = releaseQueryApi;
        this.distributorQueryApi = distributorQueryApi;
        this.productionRunQueryApi = productionRunQueryApi;
        this.allocationQueryApi = allocationQueryApi;
        this.inventoryMovementQueryApi = inventoryMovementQueryApi;
        this.inventoryMovementCommandApi = inventoryMovementCommandApi;
    }

    @Transactional
    public Sale execute(
            Long labelId,
            LocalDate saleDate,
            ChannelType channel,
            String notes,
            Long distributorId,
            List<SaleLineItemInput> lineItems
    ) {
        if (lineItems == null || lineItems.isEmpty()) {
            throw new IllegalArgumentException("Sale must contain at least one line item");
        }

        log.info("Registering sale for label {} with {} line items",
                labelId, lineItems.size());

        // 1. Validate label exists
        if (!labelQueryApi.exists(labelId)) {
            throw new EntityNotFoundException("Label not found: " + labelId);
        }

        // 2. Determine which distributor to use
        Long targetDistributorId = determineDistributor(labelId, channel, distributorId);

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
            validateAndAddLineItem(lineItemInput, labelId, targetDistributorId, saleEntity);
        }

        // 5. Save sale
        var savedSale = saleRepository.save(saleEntity);

        // 6. Record SALE movements (after save so saleId is available as referenceId)
        for (var lineItemInput : lineItems) {
            var productionRun = productionRunQueryApi
                    .findMostRecent(lineItemInput.releaseId(), lineItemInput.format())
                    .orElseThrow();
            inventoryMovementCommandApi.recordMovement(
                    productionRun.id(),
                    LocationType.DISTRIBUTOR,
                    targetDistributorId,
                    LocationType.EXTERNAL,
                    null,
                    lineItemInput.quantity(),
                    MovementType.SALE,
                    savedSale.getId()
            );
        }

        log.info("Sale registered successfully with ID {} and total amount {}",
                savedSale.getId(), savedSale.getTotalAmount());

        return convertToSale(savedSale);
    }

    private Long determineDistributor(
            Long labelId,
            ChannelType channel,
            Long distributorId
    ) {
        if (channel == ChannelType.DIRECT) {
            return distributorQueryApi
                    .findByLabelIdAndChannelType(labelId, ChannelType.DIRECT)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "DIRECT distributor not found for label: " + labelId
                    ))
                    .id();
        }

        if (distributorId == null) {
            throw new IllegalArgumentException(
                    "Distributor must be specified for " + channel + " sales"
            );
        }

        var distributor = distributorQueryApi
                .findByLabelId(labelId)
                .stream()
                .filter(d -> d.id().equals(distributorId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Distributor not found: " + distributorId
                ));

        if (distributor.channelType() != channel) {
            throw new IllegalArgumentException(
                    "Distributor '" + distributor.name()
                            + "' (type: " + distributor.channelType()
                            + ") does not match channel type: " + channel
            );
        }

        return distributorId;
    }

    private void validateAndAddLineItem(
            SaleLineItemInput lineItemInput,
            Long labelId,
            Long targetDistributorId,
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

        var distributor = distributorQueryApi
                .findByLabelId(labelId)
                .stream()
                .filter(d -> d.id().equals(targetDistributorId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Distributor not found: " + targetDistributorId
                ));

        boolean hasAllocation = allocationQueryApi
                .getAllocationsForProductionRun(productionRun.id())
                .stream()
                .anyMatch(a -> a.distributorId().equals(targetDistributorId));

        if (!hasAllocation) {
            throw new IllegalStateException(
                    "No inventory allocated for release '" + release.name()
                            + "' (" + lineItemInput.format() + ") "
                            + "to distributor '" + distributor.name() + "'. "
                            + "Please allocate inventory from the production run "
                            + "before registering sales."
            );
        }

        int available = inventoryMovementQueryApi.getCurrentInventory(
                productionRun.id(), targetDistributorId
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
