package org.omt.labelmanager.cost.persistence;

import java.util.List;
import org.omt.labelmanager.cost.CostOwnerType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CostRepository extends JpaRepository<CostEntity, Long> {

    List<CostEntity> findByOwnerOwnerTypeAndOwnerOwnerId(CostOwnerType ownerType, Long ownerId);
}
