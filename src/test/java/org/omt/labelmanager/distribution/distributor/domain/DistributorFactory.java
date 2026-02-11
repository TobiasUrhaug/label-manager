package org.omt.labelmanager.distribution.distributor.domain;

import java.util.concurrent.atomic.AtomicLong;

public final class DistributorFactory {

    private static final AtomicLong counter = new AtomicLong(1);

    private DistributorFactory() {
    }

    public static Builder aDistributor() {
        return new Builder();
    }

    public static Distributor createDefault() {
        return aDistributor().build();
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

        public Distributor build() {
            return new Distributor(id, labelId, name, channelType);
        }
    }
}
