package org.omt.labelmanager.distribution.distributor.application;

import org.omt.labelmanager.distribution.distributor.api.DistributorCommandApi;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.distribution.distributor.domain.Distributor;
import org.springframework.stereotype.Service;

@Service
class DistributorCommandApiImpl implements DistributorCommandApi {

    private final CreateDistributorUseCase createDistributor;
    private final DeleteDistributorUseCase deleteDistributor;

    DistributorCommandApiImpl(
            CreateDistributorUseCase createDistributor,
            DeleteDistributorUseCase deleteDistributor
    ) {
        this.createDistributor = createDistributor;
        this.deleteDistributor = deleteDistributor;
    }

    @Override
    public Distributor createDistributor(
            Long labelId,
            String name,
            ChannelType channelType
    ) {
        return createDistributor.execute(labelId, name, channelType);
    }

    @Override
    public boolean delete(Long id) {
        return deleteDistributor.execute(id);
    }
}
