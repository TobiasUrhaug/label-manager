package org.omt.labelmanager.distribution.distributor.domain;

public record Distributor(
        Long id,
        Long labelId,
        String name,
        ChannelType channelType
) {
}
