package org.omt.labelmanager.catalog.infrastructure.persistence.track;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.omt.labelmanager.catalog.infrastructure.persistence.artist.ArtistEntity;
import org.omt.labelmanager.catalog.infrastructure.persistence.release.ReleaseEntity;

@Entity
@Table(name = "track")
public class TrackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany
    @JoinTable(
            name = "track_artist",
            joinColumns = @JoinColumn(name = "track_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    private List<ArtistEntity> artists = new ArrayList<>();

    private String name;

    private Integer durationSeconds;

    private Integer position;

    @ManyToOne
    @JoinColumn(name = "release_id")
    private ReleaseEntity release;

    public TrackEntity() {}

    public TrackEntity(
            Long id,
            List<ArtistEntity> artists,
            String name,
            Integer durationSeconds,
            Integer position,
            ReleaseEntity release
    ) {
        this.id = id;
        this.artists = artists != null ? artists : new ArrayList<>();
        this.name = name;
        this.durationSeconds = durationSeconds;
        this.position = position;
        this.release = release;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<ArtistEntity> getArtists() {
        return artists;
    }

    public void setArtists(List<ArtistEntity> artists) {
        this.artists = artists;
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

    public ReleaseEntity getRelease() {
        return release;
    }

    public void setRelease(ReleaseEntity release) {
        this.release = release;
    }
}
