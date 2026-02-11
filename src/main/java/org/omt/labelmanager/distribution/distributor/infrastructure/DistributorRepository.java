package org.omt.labelmanager.distribution.distributor.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DistributorRepository extends JpaRepository<DistributorEntity, Long> {

    List<DistributorEntity> findByLabelId(Long labelId);
}
