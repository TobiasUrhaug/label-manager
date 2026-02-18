package org.omt.labelmanager.distribution.distributor;

import org.omt.labelmanager.distribution.distributor.api.DistributorCommandApi;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.distribution.distributor.domain.Distributor;
import org.springframework.stereotype.Component;

/**
 * Public helper for creating test distributor data.
 * Used by integration tests in other modules that need distributor fixtures,
 * without reaching into the distributor module's package-private infrastructure.
 */
@Component
public class DistributorTestHelper {

    private final DistributorCommandApi distributorCommandApi;

    public DistributorTestHelper(DistributorCommandApi distributorCommandApi) {
        this.distributorCommandApi = distributorCommandApi;
    }

    /**
     * Creates a distributor for the given label.
     *
     * @param labelId     the owning label
     * @param name        the distributor name
     * @param channelType the channel type (DIRECT, DISTRIBUTOR, etc.)
     * @return the created distributor
     */
    public Distributor createDistributor(Long labelId, String name, ChannelType channelType) {
        return distributorCommandApi.createDistributor(labelId, name, channelType);
    }
}
