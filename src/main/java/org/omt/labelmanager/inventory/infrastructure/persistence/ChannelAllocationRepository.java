package org.omt.labelmanager.inventory.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChannelAllocationRepository extends JpaRepository<ChannelAllocationEntity, Long> {

    List<ChannelAllocationEntity> findByProductionRunId(Long productionRunId);

    @Query("SELECT COALESCE(SUM(a.quantity), 0) FROM ChannelAllocationEntity a "
            + "WHERE a.productionRunId = :productionRunId")
    int sumQuantityByProductionRunId(@Param("productionRunId") Long productionRunId);
}
