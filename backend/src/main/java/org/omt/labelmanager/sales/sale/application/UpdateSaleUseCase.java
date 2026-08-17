package org.omt.labelmanager.sales.sale.application;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.omt.labelmanager.inventory.InventoryLocation;
import org.omt.labelmanager.inventory.MovementType;
import org.omt.labelmanager.inventory.domain.RunDraw;
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
    private final InventoryMovementCommandApi inventoryMovementCommandApi;
    private final SaleLineItemProcessor lineItemProcessor;
    private final SaleConverter saleConverter;

    UpdateSaleUseCase(
            SaleRepository saleRepository,
            InventoryMovementCommandApi inventoryMovementCommandApi,
            SaleLineItemProcessor lineItemProcessor,
            SaleConverter saleConverter) {
        this.saleRepository = saleRepository;
        this.inventoryMovementCommandApi = inventoryMovementCommandApi;
        this.lineItemProcessor = lineItemProcessor;
        this.saleConverter = saleConverter;
    }

    /**
     * Updates a sale's date, notes, and line items. The distributor and channel type are immutable
     * after registration — they are read from the persisted entity. If the wrong distributor was
     * used, delete the sale and register a new one.
     */
    @Transactional
    public Sale execute(
            Long saleId, LocalDate saleDate, String notes, List<SaleLineItemInput> lineItems) {
        if (lineItems == null || lineItems.isEmpty()) {
            throw new IllegalArgumentException("Sale must contain at least one line item");
        }

        log.info("Updating sale {} with {} line items", saleId, lineItems.size());

        var saleEntity =
                saleRepository
                        .findById(saleId)
                        .orElseThrow(
                                () -> new EntityNotFoundException("Sale not found: " + saleId));

        // 1. Reverse old inventory movements (restores inventory to distributor)
        inventoryMovementCommandApi.deleteMovementsByReference(MovementType.SALE, saleId);

        // 2. Replace old line items on entity
        saleEntity.clearLineItems();
        saleEntity.setSaleDate(saleDate);
        saleEntity.setNotes(notes);

        // 3. Validate the new line items and work out which pressings each draws from
        InventoryLocation from = InventoryLocation.distributor(saleEntity.getDistributorId());
        List<RunDraw> draws =
                lineItemProcessor.validateAndAdd(
                        lineItems, saleEntity.getLabelId(), from, saleEntity);

        // 4. Save updated entity
        var savedSale = saleRepository.save(saleEntity);

        // 5. Record new SALE movements (after save ensures referenceId is available), one per
        // pressing drawn from
        for (RunDraw draw : draws) {
            inventoryMovementCommandApi.recordMovement(
                    draw.productionRunId(),
                    from,
                    InventoryLocation.external(),
                    draw.quantity(),
                    MovementType.SALE,
                    savedSale.getId());
        }

        log.info(
                "Sale {} updated successfully with total amount {}",
                savedSale.getId(),
                savedSale.getTotalAmount());

        return saleConverter.toSale(savedSale);
    }
}
