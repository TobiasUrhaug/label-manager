package org.omt.labelmanager.catalog.label.web;

import jakarta.persistence.EntityNotFoundException;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.catalog.label.api.LabelCommandApi;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.label.domain.Label;
import org.omt.labelmanager.identity.api.user.AppUserDetails;
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

@RestController
@RequestMapping("/api/labels")
public class LabelController {

    private static final Logger log = LoggerFactory.getLogger(LabelController.class);

    private final LabelCommandApi labelCommandHandler;
    private final LabelQueryApi labelQueryFacade;

    public LabelController(LabelCommandApi labelCommandHandler, LabelQueryApi labelQueryFacade) {
        this.labelCommandHandler = labelCommandHandler;
        this.labelQueryFacade = labelQueryFacade;
    }

    record CreateLabelRequest(
            String labelName,
            String email,
            String website,
            String ownerName,
            String street,
            String street2,
            String city,
            String postalCode,
            String country) {
        Person toOwner() {
            if (ownerName == null || ownerName.isBlank()) {
                return null;
            }
            return new Person(ownerName);
        }

        Address toAddress() {
            if (street == null || street.isBlank()) {
                return null;
            }
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
            String country) {
        Person toOwner() {
            if (ownerName == null || ownerName.isBlank()) {
                return null;
            }
            return new Person(ownerName);
        }

        Address toAddress() {
            if (street == null || street.isBlank()) {
                return null;
            }
            return new Address(street, street2, city, postalCode, country);
        }
    }

    /**
     * The label itself.
     *
     * <p>Its releases, distributors and the caller's artists are separate collections — {@code
     * /api/labels/{id}/releases}, {@code /api/labels/{id}/distributors} and {@code /api/artists}.
     * Bundling them here was a page model for a screen that no longer exists, and the reason
     * catalog depended on distribution.
     */
    @GetMapping("/{id}")
    public Label label(@PathVariable Long id) {
        return labelQueryFacade
                .findById(id)
                .orElseThrow(
                        () -> {
                            log.warn("Label with id {} not found", id);
                            return new EntityNotFoundException("Label not found: " + id);
                        });
    }

    @PostMapping
    public ResponseEntity<Void> createLabel(
            @AuthenticationPrincipal AppUserDetails user, @RequestBody CreateLabelRequest request) {
        labelCommandHandler.createLabel(
                request.labelName(),
                request.email(),
                request.website(),
                request.toAddress(),
                request.toOwner(),
                user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateLabel(
            @PathVariable Long id, @RequestBody UpdateLabelRequest request) {
        labelCommandHandler.updateLabel(
                id,
                request.labelName(),
                request.email(),
                request.website(),
                request.toAddress(),
                request.toOwner());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLabel(@PathVariable Long id) {
        labelCommandHandler.delete(id);
        return ResponseEntity.noContent().build();
    }
}
