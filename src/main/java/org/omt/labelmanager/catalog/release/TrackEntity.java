package org.omt.labelmanager.catalog.release;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "track")
class TrackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Integer durationSeconds;

    private Integer position;

    @Column(name = "release_id", nullable = false)
    private Long releaseId;

    protected TrackEntity() {}

    TrackEntity(
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

    Long getId() {
        return id;
    }

    void setId(Long id) {
        this.id = id;
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    Integer getDurationSeconds() {
        return durationSeconds;
    }

    void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    Integer getPosition() {
        return position;
    }

    void setPosition(Integer position) {
        this.position = position;
    }

    Long getReleaseId() {
        return releaseId;
    }

    void setReleaseId(Long releaseId) {
        this.releaseId = releaseId;
    }
}
