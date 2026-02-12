package org.omt.labelmanager.distribution.distributor.infrastructure;

import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DistributorRepository extends JpaRepository<DistributorEntity, Long> {

    List<DistributorEntity> findByLabelId(Long labelId);

    Optional<DistributorEntity> findByLabelIdAndChannelType(
            Long labelId,
            ChannelType channelType
    );
}
