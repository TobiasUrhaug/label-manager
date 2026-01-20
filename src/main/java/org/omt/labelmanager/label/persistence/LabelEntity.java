package org.omt.labelmanager.label.persistence;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import org.omt.labelmanager.common.persistence.AddressEmbeddable;
import org.omt.labelmanager.common.persistence.PersonEmbeddable;

@Entity
@Table(name = "label")
public class LabelEntity {

    private String name;
    private String email;
    private String website;

    @Embedded
    private AddressEmbeddable address;

    @Embedded
    @AttributeOverride(name = "name", column = @Column(name = "owner_name"))
    private PersonEmbeddable owner;

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

    public AddressEmbeddable getAddress() {
        return address;
    }

    public void setAddress(AddressEmbeddable address) {
        this.address = address;
    }

    public PersonEmbeddable getOwner() {
        return owner;
    }

    public void setOwner(PersonEmbeddable owner) {
        this.owner = owner;
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
