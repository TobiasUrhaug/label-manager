package org.omt.labelmanager.distribution.distributor.api;

import java.util.List;
import java.util.Optional;

public interface DistributorQueryApi {

    List<Distributor> findByLabelId(Long labelId);

    /** Find a distributor by its ID. */
    Optional<Distributor> findById(Long distributorId);

    /**
     * Find a distributor by label and channel type. Commonly used to find a label's DIRECT
     * distributor.
     */
    Optional<Distributor> findByLabelIdAndChannelType(Long labelId, ChannelType channelType);

    /**
     * Reports whether the distributor exists and belongs to this label.
     *
     * @param distributorId the distributor id
     * @param labelId the label the caller believes owns it
     * @return true only if the distributor exists and its labelId matches
     */
    boolean belongsToLabel(Long distributorId, Long labelId);
}
