package org.omt.labelmanager.catalog.label;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface LabelRepository extends JpaRepository<LabelEntity, Long> {

    Optional<LabelEntity> findByName(String name);

    List<LabelEntity> findByUserId(Long userId);

}

