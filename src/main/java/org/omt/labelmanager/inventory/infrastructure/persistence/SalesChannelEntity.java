package org.omt.labelmanager.inventory.infrastructure.persistence;

import jakarta.persistence.*;
import org.omt.labelmanager.inventory.domain.ChannelType;

@Entity
@Table(name = "sales_channel")
public class SalesChannelEntity {

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

    protected SalesChannelEntity() {
    }

    public SalesChannelEntity(Long labelId, String name, ChannelType channelType) {
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
