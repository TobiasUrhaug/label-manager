package org.omt.labelmanager.catalog.release;

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
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "release")
class ReleaseEntity {

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

    protected ReleaseEntity() {}

    ReleaseEntity(
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

    LocalDate getReleaseDate() {
        return releaseDate;
    }

    void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    Long getLabelId() {
        return labelId;
    }

    void setLabelId(Long labelId) {
        this.labelId = labelId;
    }

    Set<ReleaseFormat> getFormats() {
        return formats;
    }

    void setFormats(Set<ReleaseFormat> formats) {
        this.formats = formats;
    }
}
