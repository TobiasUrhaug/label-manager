package org.omt.labelmanager.sales.distributor_return.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistributorReturnRepository
        extends JpaRepository<DistributorReturnEntity, Long> {

    List<DistributorReturnEntity> findByLabelIdOrderByReturnDateDesc(Long labelId);

    List<DistributorReturnEntity> findByDistributorIdOrderByReturnDateDesc(Long distributorId);
}
