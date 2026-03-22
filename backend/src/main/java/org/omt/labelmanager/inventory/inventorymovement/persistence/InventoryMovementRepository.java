package org.omt.labelmanager.inventory.inventorymovement.persistence;

import java.util.List;
import org.omt.labelmanager.inventory.MovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryMovementRepository
        extends JpaRepository<InventoryMovementEntity, Long> {

    List<InventoryMovementEntity> findByProductionRunIdOrderByOccurredAtDesc(Long productionRunId);

    void deleteByMovementTypeAndReferenceId(MovementType movementType, Long referenceId);

    @Query("""
            SELECT DISTINCT m.productionRunId
            FROM InventoryMovementEntity m
            WHERE m.toLocationType = 'DISTRIBUTOR'
              AND m.toLocationId = :distributorId
              AND m.movementType = 'ALLOCATION'
            """)
    List<Long> findDistinctProductionRunIdsAllocatedToDistributor(
            @Param("distributorId") Long distributorId);
}
