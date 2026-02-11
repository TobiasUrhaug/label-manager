package org.omt.labelmanager.distribution.distributor.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;

@Entity
@Table(name = "distributor")
public class DistributorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "label_id", nullable = false)
    private Long labelId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false)
    private ChannelType channelType;

    protected DistributorEntity() {
    }

    public DistributorEntity(Long labelId, String name, ChannelType channelType) {
        this.labelId = labelId;
        this.name = name;
        this.channelType = channelType;
    }

    public Long getId() {
        return id;
    }

    public Long getLabelId() {
        return labelId;
    }

    public String getName() {
        return name;
    }

    public ChannelType getChannelType() {
        return channelType;
    }
}
