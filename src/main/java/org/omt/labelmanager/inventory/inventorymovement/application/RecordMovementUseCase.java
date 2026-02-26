package org.omt.labelmanager.inventory.inventorymovement.application;

import java.time.Instant;
import org.omt.labelmanager.inventory.domain.InventoryLocation;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.infrastructure.InventoryMovementEntity;
import org.omt.labelmanager.inventory.inventorymovement.infrastructure.InventoryMovementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RecordMovementUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(RecordMovementUseCase.class);

    private final InventoryMovementRepository repository;

    RecordMovementUseCase(InventoryMovementRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(
            Long productionRunId,
            InventoryLocation from,
            InventoryLocation to,
            int quantity,
            MovementType movementType,
            Long referenceId
    ) {
        var movement = new InventoryMovementEntity(
                productionRunId,
                from.type(),
                from.id(),
                to.type(),
                to.id(),
                quantity,
                movementType,
                Instant.now(),
                referenceId
        );
        repository.save(movement);
        log.debug(
                "Recorded {} movement of {} units for production"
                + " run {} ({} → {}), referenceId={}",
                movementType, quantity, productionRunId,
                from, to, referenceId
        );
    }
}
