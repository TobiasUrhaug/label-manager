package org.omt.labelmanager.distribution.distributor.api;

public interface DistributorCommandApi {

    Distributor createDistributor(Long labelId, String name, ChannelType channelType);

    boolean delete(Long id);
}
