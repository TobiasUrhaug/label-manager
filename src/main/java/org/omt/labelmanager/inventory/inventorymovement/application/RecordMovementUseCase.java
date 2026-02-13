package org.omt.labelmanager.inventory.inventorymovement.application;

import java.time.Instant;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.infrastructure.InventoryMovementEntity;
import org.omt.labelmanager.inventory.inventorymovement.infrastructure.InventoryMovementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RecordMovementUseCase {

    private static final Logger log = LoggerFactory.getLogger(RecordMovementUseCase.class);

    private final InventoryMovementRepository repository;

    RecordMovementUseCase(InventoryMovementRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(
            Long productionRunId,
            Long distributorId,
            int quantityDelta,
            MovementType movementType,
            Long referenceId
    ) {
        var movement = new InventoryMovementEntity(
                productionRunId,
                distributorId,
                quantityDelta,
                movementType,
                Instant.now(),
                referenceId
        );
        repository.save(movement);
        log.debug(
                "Movement record created for production run {} and distributor {}",
                productionRunId,
                distributorId
        );
    }
}
