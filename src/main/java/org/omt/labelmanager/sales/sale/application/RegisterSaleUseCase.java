package org.omt.labelmanager.sales.sale.application;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorRepository;
import org.omt.labelmanager.inventory.allocation.ChannelAllocationRepository;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.infrastructure.persistence.InventoryMovementEntity;
import org.omt.labelmanager.inventory.infrastructure.persistence.InventoryMovementRepository;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
import org.omt.labelmanager.sales.sale.domain.Sale;
import org.omt.labelmanager.sales.sale.domain.SaleLineItem;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;
import org.omt.labelmanager.sales.sale.infrastructure.SaleEntity;
import org.omt.labelmanager.sales.sale.infrastructure.SaleLineItemEntity;
import org.omt.labelmanager.sales.sale.infrastructure.SaleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
class RegisterSaleUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(RegisterSaleUseCase.class);

    private final SaleRepository saleRepository;
    private final LabelQueryApi labelQueryApi;
    private final ReleaseQueryApi releaseQueryApi;
    private final DistributorRepository distributorRepository;
    private final ProductionRunRepository productionRunRepository;
    private final ChannelAllocationRepository channelAllocationRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    RegisterSaleUseCase(
            SaleRepository saleRepository,
            LabelQueryApi labelQueryApi,
            ReleaseQueryApi releaseQueryApi,
            DistributorRepository distributorRepository,
            ProductionRunRepository productionRunRepository,
            ChannelAllocationRepository channelAllocationRepository,
            InventoryMovementRepository inventoryMovementRepository
    ) {
        this.saleRepository = saleRepository;
        this.labelQueryApi = labelQueryApi;
        this.releaseQueryApi = releaseQueryApi;
        this.distributorRepository = distributorRepository;
        this.productionRunRepository = productionRunRepository;
        this.channelAllocationRepository = channelAllocationRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
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
        var directDistributor = distributorRepository
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
                    directDistributor.getId(),
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
        var productionRun = productionRunRepository
                .findTopByReleaseIdAndFormatOrderByManufacturingDateDesc(
                        lineItemInput.releaseId(),
                        lineItemInput.format()
                )
                .orElseThrow(() -> new EntityNotFoundException(
                        "No production run found for release "
                                + lineItemInput.releaseId()
                                + " and format " + lineItemInput.format()
                ));

        // Find allocation for DIRECT distributor
        var allocation = channelAllocationRepository
                .findByProductionRunIdAndDistributorId(
                        productionRun.getId(),
                        directDistributorId
                )
                .orElseThrow(() -> new EntityNotFoundException(
                        "No allocation found for production run "
                                + productionRun.getId()
                                + " and DIRECT distributor"
                ));

        // Validate sufficient inventory
        if (allocation.getQuantity() < lineItemInput.quantity()) {
            throw new IllegalStateException(
                    "Insufficient inventory for release "
                            + lineItemInput.releaseId()
                            + " format " + lineItemInput.format()
                            + ": available=" + allocation.getQuantity()
                            + ", requested=" + lineItemInput.quantity()
            );
        }

        // Create line item entity
        var lineItemEntity = new SaleLineItemEntity(
                lineItemInput.releaseId(),
                lineItemInput.format(),
                lineItemInput.quantity(),
                lineItemInput.unitPrice().amount(),
                lineItemInput.unitPrice().currency()
        );
        saleEntity.addLineItem(lineItemEntity);

        // Reduce allocation
        allocation.reduceQuantity(lineItemInput.quantity());
        channelAllocationRepository.save(allocation);

        // Create inventory movement record
        var movement = new InventoryMovementEntity(
                productionRun.getId(),
                directDistributorId,
                -lineItemInput.quantity(),  // Negative for outbound
                MovementType.SALE,
                Instant.now(),
                null  // Reference ID will be set after sale is saved
        );
        inventoryMovementRepository.save(movement);

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
