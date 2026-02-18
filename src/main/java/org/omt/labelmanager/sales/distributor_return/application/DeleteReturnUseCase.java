package org.omt.labelmanager.sales.distributor_return.application;

import jakarta.persistence.EntityNotFoundException;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementCommandApi;
import org.omt.labelmanager.sales.distributor_return.infrastructure.DistributorReturnRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DeleteReturnUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteReturnUseCase.class);

    private final DistributorReturnRepository returnRepository;
    private final InventoryMovementCommandApi inventoryMovementCommandApi;

    DeleteReturnUseCase(
            DistributorReturnRepository returnRepository,
            InventoryMovementCommandApi inventoryMovementCommandApi
    ) {
        this.returnRepository = returnRepository;
        this.inventoryMovementCommandApi = inventoryMovementCommandApi;
    }

    @Transactional
    public void execute(Long returnId) {
        log.info("Deleting return {}", returnId);

        if (!returnRepository.existsById(returnId)) {
            throw new EntityNotFoundException("Return not found: " + returnId);
        }

        // 1. Reverse inventory movements (restores inventory to distributor)
        inventoryMovementCommandApi.deleteMovementsByReference(MovementType.RETURN, returnId);

        // 2. Delete the return entity (cascades to line items)
        returnRepository.deleteById(returnId);

        log.info("Return {} deleted successfully", returnId);
    }
}
