package org.omt.labelmanager.distribution.distributor.application;

import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.distribution.distributor.domain.Distributor;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorEntity;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CreateDistributorUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(CreateDistributorUseCase.class);

    private final DistributorRepository repository;

    CreateDistributorUseCase(DistributorRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Distributor execute(Long labelId, String name, ChannelType channelType) {
        log.info(
                "Creating distributor '{}' ({}) for label {}",
                name,
                channelType,
                labelId
        );

        DistributorEntity entity =
                new DistributorEntity(labelId, name, channelType);
        entity = repository.save(entity);
        log.debug("Distributor created with id {}", entity.getId());

        return new Distributor(
                entity.getId(),
                entity.getLabelId(),
                entity.getName(),
                entity.getChannelType()
        );
    }
}
