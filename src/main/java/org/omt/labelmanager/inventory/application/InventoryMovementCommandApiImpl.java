package org.omt.labelmanager.inventory.application;

import org.omt.labelmanager.inventory.api.InventoryMovementCommandApi;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.infrastructure.persistence.InventoryMovementEntity;
import org.omt.labelmanager.inventory.infrastructure.persistence.InventoryMovementRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
class InventoryMovementCommandApiImpl implements InventoryMovementCommandApi {

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
    }
}
