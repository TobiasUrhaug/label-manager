package org.omt.labelmanager.distribution.distributor;

import org.omt.labelmanager.distribution.distributor.persistence.DistributorEntity;

public record Distributor(
        Long id,
        Long labelId,
        String name,
        ChannelType channelType
) {

    public static Distributor fromEntity(DistributorEntity entity) {
        return new Distributor(
                entity.getId(),
                entity.getLabelId(),
                entity.getName(),
                entity.getChannelType()
        );
    }
}
