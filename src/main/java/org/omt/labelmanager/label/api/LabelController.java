package org.omt.labelmanager.label.api;

import java.util.List;
import org.omt.labelmanager.common.Address;
import org.omt.labelmanager.common.Person;
import org.omt.labelmanager.label.Label;
import org.omt.labelmanager.label.LabelCRUDHandler;
import org.omt.labelmanager.release.Release;
import org.omt.labelmanager.release.ReleaseCRUDHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;


@Controller
@RequestMapping("/labels")
public class LabelController {

    private static final Logger log = LoggerFactory.getLogger(LabelController.class);

    private final LabelCRUDHandler labelCRUDHandler;
    private final ReleaseCRUDHandler releaseCRUDHandler;

    public LabelController(
            LabelCRUDHandler labelCRUDHandler,
            ReleaseCRUDHandler releaseCRUDHandler
    ) {
        this.labelCRUDHandler = labelCRUDHandler;
        this.releaseCRUDHandler = releaseCRUDHandler;
    }

    @GetMapping("/{id}")
    public String labelView(@PathVariable Long id, Model model) {
        Label label =
                labelCRUDHandler
                        .findById(id)
                        .orElseThrow(() -> {
                            log.warn("Label with id {} not found", id);
                            return new ResponseStatusException(HttpStatus.NOT_FOUND);
                        });

        List<Release> releases = releaseCRUDHandler.getReleasesForLabel(id);

        model.addAttribute("name", label.name());
        model.addAttribute("id", id);
        model.addAttribute("email", label.email());
        model.addAttribute("website", label.website());
        model.addAttribute("address", label.address());
        model.addAttribute("owner", label.owner());
        model.addAttribute("releases", releases);

        return "labels/label";
    }

    @PostMapping
    public String createLabel(
            @RequestParam String labelName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String website,
            @RequestParam(required = false) String street,
            @RequestParam(required = false) String street2,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String ownerName
    ) {
        Address address = null;
        if (street != null && !street.isBlank()) {
            address = new Address(street, street2, city, postalCode, country);
        }
        Person owner = null;
        if (ownerName != null && !ownerName.isBlank()) {
            owner = new Person(ownerName);
        }
        labelCRUDHandler.createLabel(labelName, email, website, address, owner);
        return "redirect:/dashboard";
    }

    @DeleteMapping("/{id}")
    public String deleteLabel(@PathVariable Long id) {
        labelCRUDHandler.delete(id);
        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/address")
    public String updateAddress(
            @PathVariable Long id,
            @RequestParam String street,
            @RequestParam(required = false) String street2,
            @RequestParam String city,
            @RequestParam(required = false) String postalCode,
            @RequestParam String country
    ) {
        var address = new Address(street, street2, city, postalCode, country);
        labelCRUDHandler.updateAddress(id, address);
        return "redirect:/labels/" + id;
    }

}
