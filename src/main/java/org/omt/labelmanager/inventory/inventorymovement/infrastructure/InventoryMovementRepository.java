package org.omt.labelmanager.inventory.inventorymovement.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovementEntity, Long> {

    List<InventoryMovementEntity> findByProductionRunId(Long productionRunId);

    List<InventoryMovementEntity> findByDistributorId(Long distributorId);
}
