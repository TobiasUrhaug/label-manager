package org.omt.labelmanager.release;

import jakarta.persistence.*;
import org.omt.labelmanager.label.persistence.LabelEntity;

import java.time.LocalDate;

@Entity
@Table(name = "release")
public class Release {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private LocalDate releaseDate;

    @ManyToOne
    @JoinColumn(name = "label_id")
    private LabelEntity labelEntity;

    public Release() {}

    public Release(Long id, String name, LocalDate releaseDate, LabelEntity labelEntity) {
        this.id = id;
        this.name = name;
        this.releaseDate = releaseDate;
        this.labelEntity = labelEntity;
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
        return labelEntity;
    }

    public void setLabel(LabelEntity labelEntity) {
        this.labelEntity = labelEntity;
    }
}
