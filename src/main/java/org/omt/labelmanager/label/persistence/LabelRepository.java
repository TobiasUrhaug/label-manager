package org.omt.labelmanager.label.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LabelRepository extends JpaRepository<LabelEntity, Long> {

    Optional<LabelEntity> findByName(String name);

}

