package org.omt.labelmanager.distribution.distributor.api;

import org.omt.labelmanager.distribution.distributor.ChannelType;
import org.omt.labelmanager.distribution.distributor.Distributor;

public interface DistributorCommandApi {

    Distributor createDistributor(Long labelId, String name, ChannelType channelType);

    boolean delete(Long id);
}
