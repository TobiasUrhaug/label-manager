package org.omt.labelmanager.distribution.distributor.application;

import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.distribution.distributor.domain.Distributor;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class DistributorQueryApiImpl implements DistributorQueryApi {

    private final DistributorRepository repository;

    DistributorQueryApiImpl(DistributorRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Distributor> findByLabelId(Long labelId) {
        return repository.findByLabelId(labelId).stream()
                .map(entity -> new Distributor(
                        entity.getId(),
                        entity.getLabelId(),
                        entity.getName(),
                        entity.getChannelType()
                ))
                .toList();
    }
}
