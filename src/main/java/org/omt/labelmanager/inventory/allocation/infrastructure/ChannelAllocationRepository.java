package org.omt.labelmanager.inventory.allocation.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChannelAllocationRepository extends JpaRepository<ChannelAllocationEntity, Long> {

    List<ChannelAllocationEntity> findByProductionRunId(Long productionRunId);

    Optional<ChannelAllocationEntity> findByProductionRunIdAndDistributorId(
            Long productionRunId,
            Long distributorId
    );

    @Query("SELECT COALESCE(SUM(a.quantity), 0) FROM ChannelAllocationEntity a "
            + "WHERE a.productionRunId = :productionRunId")
    int sumQuantityByProductionRunId(@Param("productionRunId") Long productionRunId);
}
