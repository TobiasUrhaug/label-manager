package org.omt.labelmanager.inventory.application;

import org.omt.labelmanager.inventory.domain.ChannelType;
import org.omt.labelmanager.inventory.domain.SalesChannel;
import org.omt.labelmanager.inventory.infrastructure.persistence.SalesChannelEntity;
import org.omt.labelmanager.inventory.infrastructure.persistence.SalesChannelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SalesChannelCRUDHandler {

    private static final Logger log = LoggerFactory.getLogger(SalesChannelCRUDHandler.class);

    private final SalesChannelRepository salesChannelRepository;

    public SalesChannelCRUDHandler(SalesChannelRepository salesChannelRepository) {
        this.salesChannelRepository = salesChannelRepository;
    }

    @Transactional
    public SalesChannel create(Long labelId, String name, ChannelType channelType) {
        log.info("Creating sales channel '{}' ({}) for label {}", name, channelType, labelId);

        SalesChannelEntity entity = new SalesChannelEntity(labelId, name, channelType);
        entity = salesChannelRepository.save(entity);
        log.debug("Sales channel created with id {}", entity.getId());

        return SalesChannel.fromEntity(entity);
    }

    public List<SalesChannel> findByLabelId(Long labelId) {
        return salesChannelRepository.findByLabelId(labelId).stream()
                .map(SalesChannel::fromEntity)
                .toList();
    }

    @Transactional
    public boolean delete(Long id) {
        if (!salesChannelRepository.existsById(id)) {
            log.warn("Sales channel with id {} not found for deletion", id);
            return false;
        }

        salesChannelRepository.deleteById(id);
        log.info("Deleted sales channel with id {}", id);
        return true;
    }
}
