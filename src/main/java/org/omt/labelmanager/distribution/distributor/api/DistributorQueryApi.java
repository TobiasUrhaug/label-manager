package org.omt.labelmanager.distribution.distributor.api;

import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.distribution.distributor.domain.Distributor;

import java.util.List;
import java.util.Optional;

public interface DistributorQueryApi {

    List<Distributor> findByLabelId(Long labelId);

    /**
     * Find a distributor by its ID.
     */
    Optional<Distributor> findById(Long distributorId);

    /**
     * Find a distributor by label and channel type.
     * Commonly used to find a label's DIRECT distributor.
     */
    Optional<Distributor> findByLabelIdAndChannelType(Long labelId, ChannelType channelType);
}
