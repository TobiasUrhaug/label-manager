package org.omt.labelmanager.catalog.release.infrastructure;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "release")
public class ReleaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private LocalDate releaseDate;

    @Column(name = "label_id", nullable = false)
    private Long labelId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "release_format",
            joinColumns = @JoinColumn(name = "release_id")
    )
    @Column(name = "format")
    @Enumerated(EnumType.STRING)
    private Set<ReleaseFormat> formats = new HashSet<>();

    public ReleaseEntity() {}

    public ReleaseEntity(
            Long id,
            String name,
            LocalDate releaseDate,
            Long labelId
    ) {
        this.id = id;
        this.name = name;
        this.releaseDate = releaseDate;
        this.labelId = labelId;
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

    public Long getLabelId() {
        return labelId;
    }

    public void setLabelId(Long labelId) {
        this.labelId = labelId;
    }

    public Set<ReleaseFormat> getFormats() {
        return formats;
    }

    public void setFormats(Set<ReleaseFormat> formats) {
        this.formats = formats;
    }
}
