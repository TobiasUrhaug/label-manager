package org.omt.labelmanager.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalesChannelRepository extends JpaRepository<SalesChannelEntity, Long> {

    List<SalesChannelEntity> findByLabelId(Long labelId);
}
