package org.omt.labelmanager.release.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.omt.labelmanager.label.persistence.LabelEntity;
import org.omt.labelmanager.track.persistence.TrackEntity;

@Entity
@Table(name = "release")
public class ReleaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private LocalDate releaseDate;

    @ManyToOne
    @JoinColumn(name = "label_id")
    private LabelEntity label;

    @OneToMany(mappedBy = "release", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<TrackEntity> tracks = new ArrayList<>();

    public ReleaseEntity() {}

    public ReleaseEntity(Long id, String name, LocalDate releaseDate, LabelEntity label) {
        this.id = id;
        this.name = name;
        this.releaseDate = releaseDate;
        this.label = label;
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

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public LabelEntity getLabel() {
        return label;
    }

    public void setLabel(LabelEntity label) {
        this.label = label;
    }

    public List<TrackEntity> getTracks() {
        return tracks;
    }

    public void setTracks(List<TrackEntity> tracks) {
        this.tracks = tracks;
    }
}
