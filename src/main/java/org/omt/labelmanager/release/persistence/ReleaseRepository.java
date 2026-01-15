package org.omt.labelmanager.release.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReleaseRepository extends JpaRepository<ReleaseEntity, Long> {

    List<ReleaseEntity> findByLabelId(Long labelId);

    Optional<ReleaseEntity> findByName(String name);

}
