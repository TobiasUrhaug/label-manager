package org.omt.labelmanager.inventory.inventorymovement.infrastructure;

import java.util.List;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository
        extends JpaRepository<InventoryMovementEntity, Long> {

    List<InventoryMovementEntity> findByProductionRunIdOrderByOccurredAtDesc(Long productionRunId);

    void deleteByMovementTypeAndReferenceId(MovementType movementType, Long referenceId);
}
