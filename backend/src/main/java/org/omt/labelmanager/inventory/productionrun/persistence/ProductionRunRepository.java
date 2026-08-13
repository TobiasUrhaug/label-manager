package org.omt.labelmanager.inventory.productionrun.persistence;

import java.util.List;
import java.util.Optional;
import org.omt.labelmanager.shared.Format;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionRunRepository extends JpaRepository<ProductionRunEntity, Long> {

    List<ProductionRunEntity> findByReleaseId(Long releaseId);

    Optional<ProductionRunEntity> findTopByReleaseIdAndFormatOrderByManufacturingDateDesc(
            Long releaseId, Format format);
}
