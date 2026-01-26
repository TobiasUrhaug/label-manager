package org.omt.labelmanager.catalog.artist.persistence;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import org.omt.labelmanager.catalog.shared.persistence.AddressEmbeddable;
import org.omt.labelmanager.catalog.shared.persistence.PersonEmbeddable;

@Entity
@Table(name = "artist")
public class ArtistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artist_name")
    private String artistName;

    @Embedded
    @AttributeOverride(name = "name", column = @Column(name = "real_name"))
    private PersonEmbeddable realName;

    private String email;

    @Embedded
    private AddressEmbeddable address;

    @Column(name = "user_id")
    private Long userId;

    protected ArtistEntity() {
    }

    public ArtistEntity(String artistName) {
        this.artistName = artistName;
    }

    public Long getId() {
        return id;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public PersonEmbeddable getRealName() {
        return realName;
    }

    public void setRealName(PersonEmbeddable realName) {
        this.realName = realName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public AddressEmbeddable getAddress() {
        return address;
    }

    public void setAddress(AddressEmbeddable address) {
        this.address = address;
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
        ArtistEntity that = (ArtistEntity) o;
        return Objects.equals(artistName, that.artistName) && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artistName, id);
    }
}
