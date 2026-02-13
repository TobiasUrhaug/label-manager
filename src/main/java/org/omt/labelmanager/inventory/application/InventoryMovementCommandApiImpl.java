package org.omt.labelmanager.inventory.application;

import org.omt.labelmanager.inventory.api.InventoryMovementCommandApi;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.infrastructure.persistence.InventoryMovementEntity;
import org.omt.labelmanager.inventory.infrastructure.persistence.InventoryMovementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
class InventoryMovementCommandApiImpl implements InventoryMovementCommandApi {

    private static final Logger log =
            LoggerFactory.getLogger(InventoryMovementCommandApiImpl.class);

    private final InventoryMovementRepository repository;

    InventoryMovementCommandApiImpl(InventoryMovementRepository repository) {
        this.repository = repository;
    }

    @Override
    public void recordMovement(
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
        log.debug("Movement record created for production run {} and distributor {}", productionRunId, distributorId);
    }
}
