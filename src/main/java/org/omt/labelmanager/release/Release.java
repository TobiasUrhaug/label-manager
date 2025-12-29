package org.omt.labelmanager.release;

import jakarta.persistence.*;
import org.omt.labelmanager.label.Label;

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
    private Label label;

    public Release() {}

    public Release(Long id, String name, LocalDate releaseDate, Label label) {
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

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
    }
}
