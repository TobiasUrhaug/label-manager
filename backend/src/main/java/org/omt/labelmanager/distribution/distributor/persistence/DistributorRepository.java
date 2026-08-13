package org.omt.labelmanager.distribution.distributor.persistence;

import java.util.List;
import java.util.Optional;
import org.omt.labelmanager.distribution.distributor.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistributorRepository extends JpaRepository<DistributorEntity, Long> {

    List<DistributorEntity> findByLabelId(Long labelId);

    Optional<DistributorEntity> findByLabelIdAndChannelType(Long labelId, ChannelType channelType);
}
