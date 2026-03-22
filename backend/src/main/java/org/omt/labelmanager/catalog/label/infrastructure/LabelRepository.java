package org.omt.labelmanager.catalog.label.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LabelRepository extends JpaRepository<LabelEntity, Long> {

    Optional<LabelEntity> findByName(String name);

    List<LabelEntity> findByUserId(Long userId);

}

