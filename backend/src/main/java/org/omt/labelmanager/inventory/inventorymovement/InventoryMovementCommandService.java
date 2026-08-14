package org.omt.labelmanager.inventory.inventorymovement;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.omt.labelmanager.inventory.InventoryLocation;
import org.omt.labelmanager.inventory.LocationType;
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
    public void recordManufacture(Long productionRunId, int quantity, LocalDate manufacturedOn) {
        var movement =
                new InventoryMovementEntity(
                        productionRunId,
                        LocationType.EXTERNAL,
                        null,
                        LocationType.WAREHOUSE,
                        null,
                        quantity,
                        MovementType.PRODUCTION,
                        manufacturedOn.atStartOfDay(ZoneOffset.UTC).toInstant(),
                        null);
        repository.save(movement);
        log.debug(
                "Recorded manufacture of {} units for production run {} on {}",
                quantity,
                productionRunId,
                manufacturedOn);
    }

    @Override
    @Transactional
    public void recordMovement(
            Long productionRunId,
            InventoryLocation from,
            InventoryLocation to,
            int quantity,
            MovementType movementType,
            Long referenceId) {
        var movement =
                new InventoryMovementEntity(
                        productionRunId,
                        from.type(),
                        from.id(),
                        to.type(),
                        to.id(),
                        quantity,
                        movementType,
                        Instant.now(),
                        referenceId);
        repository.save(movement);
        log.debug(
                "Recorded {} movement of {} units for production"
                        + " run {} ({} → {}), referenceId={}",
                movementType,
                quantity,
                productionRunId,
                from,
                to,
                referenceId);
    }

    @Override
    @Transactional
    public void deleteMovementsByReference(MovementType movementType, Long referenceId) {
        repository.deleteByMovementTypeAndReferenceId(movementType, referenceId);
        // Flushed deliberately, not left to the provider. Callers reverse a sale's movements and
        // then re-read the balances to revalidate the new line items, and those balances are
        // computed by a native query — which JPA does not promise to flush pending deletes before.
        // Without this, an edit that reuses the stock it just released can be rejected as
        // insufficient.
        repository.flush();
        log.debug("Deleted all {} movements with referenceId={}", movementType, referenceId);
    }
}
