package org.omt.labelmanager.inventory.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository
        extends JpaRepository<InventoryMovementEntity, Long> {

    List<InventoryMovementEntity> findByProductionRunId(Long productionRunId);

    List<InventoryMovementEntity> findBySalesChannelId(Long salesChannelId);
}
