package org.omt.labelmanager.catalog.infrastructure.persistence.track;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "track")
public class TrackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Integer durationSeconds;

    private Integer position;

    @Column(name = "release_id", nullable = false)
    private Long releaseId;

    public TrackEntity() {}

    public TrackEntity(
            Long id,
            String name,
            Integer durationSeconds,
            Integer position,
            Long releaseId
    ) {
        this.id = id;
        this.name = name;
        this.durationSeconds = durationSeconds;
        this.position = position;
        this.releaseId = releaseId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Long getReleaseId() {
        return releaseId;
    }

    public void setReleaseId(Long releaseId) {
        this.releaseId = releaseId;
    }
}
