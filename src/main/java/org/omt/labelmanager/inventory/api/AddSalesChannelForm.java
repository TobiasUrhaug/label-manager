package org.omt.labelmanager.inventory.api;

import org.omt.labelmanager.inventory.domain.ChannelType;

public class AddSalesChannelForm {

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
