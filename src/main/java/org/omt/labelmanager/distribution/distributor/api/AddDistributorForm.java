package org.omt.labelmanager.distribution.distributor.api;

import org.omt.labelmanager.distribution.distributor.domain.ChannelType;

public class AddDistributorForm {

    private String name;
    private ChannelType channelType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public void setChannelType(ChannelType channelType) {
        this.channelType = channelType;
    }
}
