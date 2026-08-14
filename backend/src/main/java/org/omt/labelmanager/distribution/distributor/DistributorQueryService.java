package org.omt.labelmanager.distribution.distributor;

import java.util.List;
import java.util.Optional;
import org.omt.labelmanager.distribution.distributor.api.ChannelType;
import org.omt.labelmanager.distribution.distributor.api.Distributor;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.distribution.distributor.persistence.DistributorRepository;
import org.springframework.stereotype.Service;

@Service
class DistributorQueryService implements DistributorQueryApi {

    private final DistributorRepository repository;

    DistributorQueryService(DistributorRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Distributor> findByLabelId(Long labelId) {
        return repository.findByLabelId(labelId).stream().map(Distributor::fromEntity).toList();
    }

    @Override
    public Optional<Distributor> findById(Long distributorId) {
        return repository.findById(distributorId).map(Distributor::fromEntity);
    }

    @Override
    public Optional<Distributor> findByLabelIdAndChannelType(
            Long labelId, ChannelType channelType) {
        return repository
                .findByLabelIdAndChannelType(labelId, channelType)
                .map(Distributor::fromEntity);
    }

    @Override
    public boolean belongsToLabel(Long distributorId, Long labelId) {
        return repository
                .findById(distributorId)
                .map(distributor -> distributor.getLabelId().equals(labelId))
                .orElse(false);
    }
}
