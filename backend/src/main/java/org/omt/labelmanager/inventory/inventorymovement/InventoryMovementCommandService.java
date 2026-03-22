package org.omt.labelmanager.inventory.inventorymovement;

import java.time.Instant;
import org.omt.labelmanager.inventory.InventoryLocation;
import org.omt.labelmanager.inventory.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementCommandApi;
import org.omt.labelmanager.inventory.inventorymovement.persistence.InventoryMovementEntity;
import org.omt.labelmanager.inventory.inventorymovement.persistence.InventoryMovementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class InventoryMovementCommandService implements InventoryMovementCommandApi {

    private static final Logger log =
            LoggerFactory.getLogger(InventoryMovementCommandService.class);

    private final InventoryMovementRepository repository;

    InventoryMovementCommandService(InventoryMovementRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void recordMovement(
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

    @Override
    @Transactional
    public void deleteMovementsByReference(
            MovementType movementType, Long referenceId
    ) {
        repository.deleteByMovementTypeAndReferenceId(movementType, referenceId);
        log.debug(
                "Deleted all {} movements with referenceId={}",
                movementType, referenceId
        );
    }
}
