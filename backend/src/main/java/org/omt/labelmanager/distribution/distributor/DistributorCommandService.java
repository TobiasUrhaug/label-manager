package org.omt.labelmanager.distribution.distributor;

import org.omt.labelmanager.distribution.distributor.api.DistributorCommandApi;
import org.omt.labelmanager.distribution.distributor.persistence.DistributorEntity;
import org.omt.labelmanager.distribution.distributor.persistence.DistributorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DistributorCommandService implements DistributorCommandApi {

    private static final Logger log = LoggerFactory.getLogger(DistributorCommandService.class);

    private final DistributorRepository repository;

    DistributorCommandService(DistributorRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Distributor createDistributor(Long labelId, String name, ChannelType channelType) {
        log.info("Creating distributor '{}' ({}) for label {}", name, channelType, labelId);
        var entity = repository.save(new DistributorEntity(labelId, name, channelType));
        log.debug("Distributor created with id {}", entity.getId());
        return Distributor.fromEntity(entity);
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        if (!repository.existsById(id)) {
            log.warn("Distributor with id {} not found for deletion", id);
            return false;
        }
        repository.deleteById(id);
        log.info("Deleted distributor with id {}", id);
        return true;
    }
}
