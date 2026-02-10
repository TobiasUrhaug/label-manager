package org.omt.labelmanager.finance.cost;

import org.omt.labelmanager.finance.cost.domain.CostOwnerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface CostRepository extends JpaRepository<CostEntity, Long> {

    List<CostEntity> findByOwnerOwnerTypeAndOwnerOwnerId(CostOwnerType ownerType, Long ownerId);
}
