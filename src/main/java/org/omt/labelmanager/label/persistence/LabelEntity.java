package org.omt.labelmanager.label.persistence;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "label")
public class LabelEntity {

    private String name;

    protected LabelEntity() {
    }

    public LabelEntity(String name) {
        this.name = name;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LabelEntity labelEntity = (LabelEntity) o;
        return Objects.equals(name, labelEntity.name) && Objects.equals(id, labelEntity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id);
    }

}
