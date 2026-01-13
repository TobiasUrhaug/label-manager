package org.omt.labelmanager.label.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRepository extends JpaRepository<LabelEntity, Long> {
}

