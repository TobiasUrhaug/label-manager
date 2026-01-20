package org.omt.labelmanager.label.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "label")
public class LabelEntity {

    private String name;
    private String email;
    private String website;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    protected LabelEntity() {
    }

    public LabelEntity(String name, String email, String website) {
        this.name = name;
        this.email = email;
        this.website = website;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getWebsite() {
        return website;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LabelEntity labelEntity = (LabelEntity) o;
        return Objects.equals(name, labelEntity.name) && Objects.equals(id, labelEntity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id);
    }

}
