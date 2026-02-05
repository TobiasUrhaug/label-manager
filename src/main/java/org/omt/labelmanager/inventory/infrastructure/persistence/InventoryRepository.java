package org.omt.labelmanager.inventory.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<InventoryEntity, Long> {

    List<InventoryEntity> findByReleaseId(Long releaseId);
}
