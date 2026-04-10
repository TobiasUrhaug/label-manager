package org.omt.labelmanager.catalog.label.api;

import org.omt.labelmanager.catalog.artist.api.ArtistQueryApi;
import org.omt.labelmanager.catalog.artist.domain.Artist;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.catalog.label.domain.Label;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.catalog.release.domain.Release;
import org.omt.labelmanager.distribution.distributor.Distributor;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/labels")
public class LabelController {

    private static final Logger log = LoggerFactory.getLogger(LabelController.class);

    private final LabelCommandApi labelCommandHandler;
    private final LabelQueryApi labelQueryFacade;
    private final ReleaseQueryApi releaseQueryFacade;
    private final ArtistQueryApi artistQueryApi;
    private final DistributorQueryApi distributorQueryApi;

    public LabelController(
            LabelCommandApi labelCommandHandler,
            LabelQueryApi labelQueryFacade,
            ReleaseQueryApi releaseQueryFacade,
            ArtistQueryApi artistQueryApi,
            DistributorQueryApi distributorQueryApi
    ) {
        this.labelCommandHandler = labelCommandHandler;
        this.labelQueryFacade = labelQueryFacade;
        this.releaseQueryFacade = releaseQueryFacade;
        this.artistQueryApi = artistQueryApi;
        this.distributorQueryApi = distributorQueryApi;
    }

    record LabelDetailResponse(
            Long id,
            String name,
            String email,
            String website,
            Address address,
            Person owner,
            List<Release> releases,
            List<Artist> artists,
            List<Distributor> distributors
    ) {}

    record CreateLabelRequest(
            String labelName,
            String email,
            String website,
            String ownerName,
            String street,
            String street2,
            String city,
            String postalCode,
            String country
    ) {
        Person toOwner() {
            if (ownerName == null || ownerName.isBlank()) return null;
            return new Person(ownerName);
        }

        Address toAddress() {
            if (street == null || street.isBlank()) return null;
            return new Address(street, street2, city, postalCode, country);
        }
    }

    record UpdateLabelRequest(
            String labelName,
            String email,
            String website,
            String ownerName,
            String street,
            String street2,
            String city,
            String postalCode,
            String country
    ) {
        Person toOwner() {
            if (ownerName == null || ownerName.isBlank()) return null;
            return new Person(ownerName);
        }

        Address toAddress() {
            if (street == null || street.isBlank()) return null;
            return new Address(street, street2, city, postalCode, country);
        }
    }

    @GetMapping("/{id}")
    public LabelDetailResponse label(
            @AuthenticationPrincipal AppUserDetails user,
            @PathVariable Long id
    ) {
        Label label = labelQueryFacade
                .findById(id)
                .orElseThrow(() -> {
                    log.warn("Label with id {} not found", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND);
                });

        List<Release> releases = releaseQueryFacade.getReleasesForLabel(id);
        List<Artist> artists = artistQueryApi.getArtistsForUser(user.getId());
        List<Distributor> distributors = distributorQueryApi.findByLabelId(id);

        return new LabelDetailResponse(
                label.id(), label.name(), label.email(), label.website(),
                label.address(), label.owner(), releases, artists, distributors
        );
    }

    @PostMapping
    public ResponseEntity<Void> createLabel(
            @AuthenticationPrincipal AppUserDetails user,
            @RequestBody CreateLabelRequest request
    ) {
        labelCommandHandler.createLabel(
                request.labelName(),
                request.email(),
                request.website(),
                request.toAddress(),
                request.toOwner(),
                user.getId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateLabel(
            @PathVariable Long id,
            @RequestBody UpdateLabelRequest request
    ) {
        labelCommandHandler.updateLabel(
                id,
                request.labelName(),
                request.email(),
                request.website(),
                request.toAddress(),
                request.toOwner()
        );
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLabel(@PathVariable Long id) {
        labelCommandHandler.delete(id);
        return ResponseEntity.noContent().build();
    }
}
