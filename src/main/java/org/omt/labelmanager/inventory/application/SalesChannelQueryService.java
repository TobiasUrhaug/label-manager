package org.omt.labelmanager.inventory.application;

import org.omt.labelmanager.inventory.domain.SalesChannel;
import org.omt.labelmanager.inventory.infrastructure.persistence.SalesChannelRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalesChannelQueryService {

    private final SalesChannelRepository salesChannelRepository;

    public SalesChannelQueryService(SalesChannelRepository salesChannelRepository) {
        this.salesChannelRepository = salesChannelRepository;
    }

    public List<SalesChannel> getSalesChannelsForLabel(Long labelId) {
        return salesChannelRepository.findByLabelId(labelId).stream()
                .map(SalesChannel::fromEntity)
                .toList();
    }
}
