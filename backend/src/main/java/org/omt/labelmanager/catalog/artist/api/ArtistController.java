package org.omt.labelmanager.catalog.artist.api;

import org.omt.labelmanager.catalog.artist.domain.Artist;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/artists")
public class ArtistController {

    private static final Logger log = LoggerFactory.getLogger(ArtistController.class);

    private final ArtistCommandApi artistCommandApi;
    private final ArtistQueryApi artistQueryApi;

    public ArtistController(
            ArtistCommandApi artistCommandApi,
            ArtistQueryApi artistQueryApi
    ) {
        this.artistCommandApi = artistCommandApi;
        this.artistQueryApi = artistQueryApi;
    }

    record CreateArtistRequest(
            String artistName,
            String realName,
            String email,
            String street,
            String street2,
            String city,
            String postalCode,
            String country
    ) {
        Person toRealName() {
            if (realName == null || realName.isBlank()) return null;
            return new Person(realName);
        }

        Address toAddress() {
            if (street == null || street.isBlank()) return null;
            return new Address(street, street2, city, postalCode, country);
        }
    }

    record UpdateArtistRequest(
            String artistName,
            String realName,
            String email,
            String street,
            String street2,
            String city,
            String postalCode,
            String country
    ) {
        Person toRealName() {
            if (realName == null || realName.isBlank()) return null;
            return new Person(realName);
        }

        Address toAddress() {
            if (street == null || street.isBlank()) return null;
            return new Address(street, street2, city, postalCode, country);
        }
    }

    @GetMapping("/{id}")
    public Artist artist(@PathVariable Long id) {
        return artistQueryApi
                .findById(id)
                .orElseThrow(() -> {
                    log.warn("Artist with id {} not found", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND);
                });
    }

    @PostMapping
    public ResponseEntity<Void> createArtist(
            @AuthenticationPrincipal AppUserDetails user,
            @RequestBody CreateArtistRequest request
    ) {
        artistCommandApi.createArtist(
                request.artistName(),
                request.toRealName(),
                request.email(),
                request.toAddress(),
                user.getId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateArtist(
            @PathVariable Long id,
            @RequestBody UpdateArtistRequest request
    ) {
        artistCommandApi.updateArtist(
                id,
                request.artistName(),
                request.toRealName(),
                request.email(),
                request.toAddress()
        );
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArtist(@PathVariable Long id) {
        artistCommandApi.delete(id);
        return ResponseEntity.noContent().build();
    }
}
