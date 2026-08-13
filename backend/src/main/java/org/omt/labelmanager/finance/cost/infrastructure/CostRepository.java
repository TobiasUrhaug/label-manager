package org.omt.labelmanager.finance.cost.infrastructure;

import java.util.List;
import org.omt.labelmanager.finance.cost.domain.CostOwnerType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CostRepository extends JpaRepository<CostEntity, Long> {

    List<CostEntity> findByOwnerOwnerTypeAndOwnerOwnerId(CostOwnerType ownerType, Long ownerId);
}
