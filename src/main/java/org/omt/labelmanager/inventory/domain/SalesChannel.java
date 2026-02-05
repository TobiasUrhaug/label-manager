package org.omt.labelmanager.inventory.domain;

import org.omt.labelmanager.inventory.infrastructure.persistence.SalesChannelEntity;

public record SalesChannel(Long id, Long labelId, String name, ChannelType channelType) {

    public static SalesChannel fromEntity(SalesChannelEntity entity) {
        return new SalesChannel(
                entity.getId(),
                entity.getLabelId(),
                entity.getName(),
                entity.getChannelType()
        );
    }
}
