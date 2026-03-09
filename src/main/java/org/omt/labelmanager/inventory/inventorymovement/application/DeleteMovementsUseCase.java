package org.omt.labelmanager.inventory.inventorymovement.application;

import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.infrastructure.InventoryMovementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DeleteMovementsUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteMovementsUseCase.class);

    private final InventoryMovementRepository repository;

    DeleteMovementsUseCase(InventoryMovementRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(MovementType movementType, Long referenceId) {
        repository.deleteByMovementTypeAndReferenceId(movementType, referenceId);
        log.debug(
                "Deleted all {} movements with referenceId={}",
                movementType, referenceId
        );
    }
}
