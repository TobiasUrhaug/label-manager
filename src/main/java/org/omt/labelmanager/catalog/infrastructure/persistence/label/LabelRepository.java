package org.omt.labelmanager.catalog.infrastructure.persistence.label;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRepository extends JpaRepository<LabelEntity, Long> {

    Optional<LabelEntity> findByName(String name);

    List<LabelEntity> findByUserId(Long userId);

}

