package org.omt.labelmanager.sales.sale.application;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.inventory.domain.LocationType;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementCommandApi;
import org.omt.labelmanager.sales.sale.domain.Sale;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;
import org.omt.labelmanager.sales.sale.infrastructure.SaleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UpdateSaleUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateSaleUseCase.class);

    private final SaleRepository saleRepository;
    private final DistributorQueryApi distributorQueryApi;
    private final InventoryMovementCommandApi inventoryMovementCommandApi;
    private final SaleLineItemProcessor lineItemProcessor;
    private final SaleConverter saleConverter;

    UpdateSaleUseCase(
            SaleRepository saleRepository,
            DistributorQueryApi distributorQueryApi,
            InventoryMovementCommandApi inventoryMovementCommandApi,
            SaleLineItemProcessor lineItemProcessor,
            SaleConverter saleConverter
    ) {
        this.saleRepository = saleRepository;
        this.distributorQueryApi = distributorQueryApi;
        this.inventoryMovementCommandApi = inventoryMovementCommandApi;
        this.lineItemProcessor = lineItemProcessor;
        this.saleConverter = saleConverter;
    }

    @Transactional
    public Sale execute(
            Long saleId,
            LocalDate saleDate,
            String notes,
            List<SaleLineItemInput> lineItems
    ) {
        if (lineItems == null || lineItems.isEmpty()) {
            throw new IllegalArgumentException("Sale must contain at least one line item");
        }

        log.info("Updating sale {} with {} line items", saleId, lineItems.size());

        var saleEntity = saleRepository.findById(saleId)
                .orElseThrow(() -> new EntityNotFoundException("Sale not found: " + saleId));

        // 1. Reverse old inventory movements (restores inventory to distributor)
        inventoryMovementCommandApi.deleteMovementsByReference(MovementType.SALE, saleId);

        // 2. Replace old line items on entity
        saleEntity.clearLineItems();
        saleEntity.setSaleDate(saleDate);
        saleEntity.setNotes(notes);

        // 3. Validate each new line item and add to entity, caching production run IDs
        Long distributorId = saleEntity.getDistributorId();
        String distributorName = resolveDistributorName(distributorId, saleEntity.getLabelId());
        Map<SaleLineItemInput, Long> productionRunIds = new LinkedHashMap<>();
        for (var lineItemInput : lineItems) {
            Long productionRunId = lineItemProcessor.validateAndAdd(
                    lineItemInput,
                    saleEntity.getLabelId(),
                    distributorId,
                    distributorName,
                    saleEntity
            );
            productionRunIds.put(lineItemInput, productionRunId);
        }

        // 4. Save updated entity
        var savedSale = saleRepository.save(saleEntity);

        // 5. Record new SALE movements (after save ensures referenceId is available)
        for (var entry : productionRunIds.entrySet()) {
            inventoryMovementCommandApi.recordMovement(
                    entry.getValue(),
                    LocationType.DISTRIBUTOR,
                    distributorId,
                    LocationType.EXTERNAL,
                    null,
                    entry.getKey().quantity(),
                    MovementType.SALE,
                    savedSale.getId()
            );
        }

        log.info("Sale {} updated successfully with total amount {}",
                savedSale.getId(), savedSale.getTotalAmount());

        return saleConverter.toSale(savedSale);
    }

    private String resolveDistributorName(Long distributorId, Long labelId) {
        return distributorQueryApi.findByLabelId(labelId)
                .stream()
                .filter(d -> d.id().equals(distributorId))
                .findFirst()
                .map(d -> d.name())
                .orElse("id=" + distributorId);
    }
}
