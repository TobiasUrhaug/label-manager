package org.omt.labelmanager.catalog.label;

import jakarta.persistence.*;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.AddressEmbeddable;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.PersonEmbeddable;

import java.util.Objects;

@Entity
@Table(name = "label")
class LabelEntity {

    private String name;
    private String email;
    private String website;

    @Embedded
    private AddressEmbeddable address;

    @Embedded
    @AttributeOverride(name = "name", column = @Column(name = "owner_name"))
    private PersonEmbeddable owner;

    @Column(name = "user_id")
    private Long userId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    protected LabelEntity() {
    }

    LabelEntity(String name, String email, String website) {
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

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setWebsite(String website) {
        this.website = website;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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
