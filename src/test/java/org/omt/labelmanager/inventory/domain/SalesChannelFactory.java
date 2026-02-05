package org.omt.labelmanager.inventory.domain;

import java.util.concurrent.atomic.AtomicLong;

public final class SalesChannelFactory {

    private static final AtomicLong counter = new AtomicLong(1);

    private SalesChannelFactory() {
    }

    public static Builder aSalesChannel() {
        return new Builder();
    }

    public static SalesChannel createDefault() {
        return aSalesChannel().build();
    }

    public static final class Builder {

        private Long id = counter.getAndIncrement();
        private Long labelId = 1L;
        private String name = "Direct Sales";
        private ChannelType channelType = ChannelType.DIRECT;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder labelId(Long labelId) {
            this.labelId = labelId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder channelType(ChannelType channelType) {
            this.channelType = channelType;
            return this;
        }

        public SalesChannel build() {
            return new SalesChannel(id, labelId, name, channelType);
        }
    }
}
