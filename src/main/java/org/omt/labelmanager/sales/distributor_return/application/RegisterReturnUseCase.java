package org.omt.labelmanager.sales.distributor_return.application;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.inventory.domain.LocationType;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementCommandApi;
import org.omt.labelmanager.sales.distributor_return.domain.DistributorReturn;
import org.omt.labelmanager.sales.distributor_return.domain.ReturnLineItemInput;
import org.omt.labelmanager.sales.distributor_return.infrastructure.DistributorReturnEntity;
import org.omt.labelmanager.sales.distributor_return.infrastructure.DistributorReturnRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RegisterReturnUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterReturnUseCase.class);

    private final DistributorReturnRepository returnRepository;
    private final LabelQueryApi labelQueryApi;
    private final DistributorQueryApi distributorQueryApi;
    private final InventoryMovementCommandApi inventoryMovementCommandApi;
    private final ReturnLineItemProcessor lineItemProcessor;
    private final ReturnConverter returnConverter;

    RegisterReturnUseCase(
            DistributorReturnRepository returnRepository,
            LabelQueryApi labelQueryApi,
            DistributorQueryApi distributorQueryApi,
            InventoryMovementCommandApi inventoryMovementCommandApi,
            ReturnLineItemProcessor lineItemProcessor,
            ReturnConverter returnConverter
    ) {
        this.returnRepository = returnRepository;
        this.labelQueryApi = labelQueryApi;
        this.distributorQueryApi = distributorQueryApi;
        this.inventoryMovementCommandApi = inventoryMovementCommandApi;
        this.lineItemProcessor = lineItemProcessor;
        this.returnConverter = returnConverter;
    }

    @Transactional
    public DistributorReturn execute(
            Long labelId,
            Long distributorId,
            LocalDate returnDate,
            String notes,
            List<ReturnLineItemInput> lineItems
    ) {
        if (lineItems == null || lineItems.isEmpty()) {
            throw new IllegalArgumentException("Return must contain at least one line item");
        }

        log.info("Registering return for label {} from distributor {} with {} line items",
                labelId, distributorId, lineItems.size());

        // 1. Validate label exists
        if (!labelQueryApi.exists(labelId)) {
            throw new EntityNotFoundException("Label not found: " + labelId);
        }

        // 2. Validate distributor belongs to this label
        distributorQueryApi.findByLabelId(labelId)
                .stream()
                .filter(d -> d.id().equals(distributorId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Distributor " + distributorId + " not found for label " + labelId
                ));

        // 3. Create return entity
        var returnEntity = new DistributorReturnEntity(labelId, distributorId, returnDate, notes);

        // 4. Validate each line item and cache production run IDs for step 6
        Map<ReturnLineItemInput, Long> productionRunIds = new LinkedHashMap<>();
        for (var lineItemInput : lineItems) {
            Long productionRunId = lineItemProcessor.validateAndAdd(
                    lineItemInput, labelId, distributorId, returnEntity
            );
            productionRunIds.put(lineItemInput, productionRunId);
        }

        // 5. Save return entity (generates returnId)
        var savedReturn = returnRepository.save(returnEntity);

        // 6. Record RETURN movements (after save so returnId is available as referenceId)
        for (var entry : productionRunIds.entrySet()) {
            inventoryMovementCommandApi.recordMovement(
                    entry.getValue(),
                    LocationType.DISTRIBUTOR,
                    distributorId,
                    LocationType.WAREHOUSE,
                    null,
                    entry.getKey().quantity(),
                    MovementType.RETURN,
                    savedReturn.getId()
            );
        }

        log.info("Return registered successfully with ID {}", savedReturn.getId());

        return returnConverter.toReturn(savedReturn);
    }
}
