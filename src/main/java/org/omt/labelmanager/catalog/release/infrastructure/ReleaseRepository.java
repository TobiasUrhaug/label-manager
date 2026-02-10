package org.omt.labelmanager.catalog.release.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseRepository extends JpaRepository<ReleaseEntity, Long> {

    List<ReleaseEntity> findByLabelId(Long labelId);

    Optional<ReleaseEntity> findByName(String name);

}
