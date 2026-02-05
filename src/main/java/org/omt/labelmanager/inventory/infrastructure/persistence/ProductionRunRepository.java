package org.omt.labelmanager.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductionRunRepository extends JpaRepository<ProductionRunEntity, Long> {

    List<ProductionRunEntity> findByReleaseId(Long releaseId);
}
