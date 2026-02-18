package org.omt.labelmanager.sales.distributor_return.application;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.omt.labelmanager.inventory.domain.LocationType;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementCommandApi;
import org.omt.labelmanager.sales.distributor_return.domain.ReturnLineItemInput;
import org.omt.labelmanager.sales.distributor_return.infrastructure.DistributorReturnRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UpdateReturnUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateReturnUseCase.class);

    private final DistributorReturnRepository returnRepository;
    private final InventoryMovementCommandApi inventoryMovementCommandApi;
    private final ReturnLineItemProcessor lineItemProcessor;

    UpdateReturnUseCase(
            DistributorReturnRepository returnRepository,
            InventoryMovementCommandApi inventoryMovementCommandApi,
            ReturnLineItemProcessor lineItemProcessor
    ) {
        this.returnRepository = returnRepository;
        this.inventoryMovementCommandApi = inventoryMovementCommandApi;
        this.lineItemProcessor = lineItemProcessor;
    }

    /**
     * Updates a return's date, notes, and line items. The distributor is immutable
     * after registration. Old RETURN movements are deleted and new ones are recorded.
     */
    @Transactional
    public void execute(
            Long returnId,
            LocalDate returnDate,
            String notes,
            List<ReturnLineItemInput> lineItems
    ) {
        if (lineItems == null || lineItems.isEmpty()) {
            throw new IllegalArgumentException("Return must contain at least one line item");
        }

        log.info("Updating return {} with {} line items", returnId, lineItems.size());

        var returnEntity = returnRepository.findById(returnId)
                .orElseThrow(() -> new EntityNotFoundException("Return not found: " + returnId));

        // 1. Reverse old inventory movements (restores inventory to distributor)
        inventoryMovementCommandApi.deleteMovementsByReference(MovementType.RETURN, returnId);

        // 2. Replace old line items on entity
        returnEntity.clearLineItems();
        returnEntity.setReturnDate(returnDate);
        returnEntity.setNotes(notes);

        // 3. Validate each new line item and add to entity, caching production run IDs
        Long distributorId = returnEntity.getDistributorId();
        Map<ReturnLineItemInput, Long> productionRunIds = new LinkedHashMap<>();
        for (var lineItemInput : lineItems) {
            Long productionRunId = lineItemProcessor.validateAndAdd(
                    lineItemInput,
                    returnEntity.getLabelId(),
                    distributorId,
                    returnEntity
            );
            productionRunIds.put(lineItemInput, productionRunId);
        }

        // 4. Save updated entity
        returnRepository.save(returnEntity);

        // 5. Record new RETURN movements (after save ensures referenceId is available)
        for (var entry : productionRunIds.entrySet()) {
            inventoryMovementCommandApi.recordMovement(
                    entry.getValue(),
                    LocationType.DISTRIBUTOR,
                    distributorId,
                    LocationType.WAREHOUSE,
                    null,
                    entry.getKey().quantity(),
                    MovementType.RETURN,
                    returnId
            );
        }

        log.info("Return {} updated successfully", returnId);
    }
}
