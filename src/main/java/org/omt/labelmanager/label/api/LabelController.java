package org.omt.labelmanager.label.api;

import java.util.List;
import org.omt.labelmanager.artist.Artist;
import org.omt.labelmanager.artist.ArtistCRUDHandler;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;


@Controller
@RequestMapping("/labels")
public class LabelController {

    private static final Logger log = LoggerFactory.getLogger(LabelController.class);

    private final LabelCRUDHandler labelCRUDHandler;
    private final ReleaseCRUDHandler releaseCRUDHandler;
    private final ArtistCRUDHandler artistCRUDHandler;

    public LabelController(
            LabelCRUDHandler labelCRUDHandler,
            ReleaseCRUDHandler releaseCRUDHandler,
            ArtistCRUDHandler artistCRUDHandler
    ) {
        this.labelCRUDHandler = labelCRUDHandler;
        this.releaseCRUDHandler = releaseCRUDHandler;
        this.artistCRUDHandler = artistCRUDHandler;
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
        List<Artist> artists = artistCRUDHandler.getAllArtists();

        model.addAttribute("name", label.name());
        model.addAttribute("id", id);
        model.addAttribute("email", label.email());
        model.addAttribute("website", label.website());
        model.addAttribute("address", label.address());
        model.addAttribute("owner", label.owner());
        model.addAttribute("releases", releases);
        model.addAttribute("artists", artists);

        return "labels/label";
    }

    @PostMapping
    public String createLabel(CreateLabelForm form) {
        labelCRUDHandler.createLabel(
                form.getLabelName(),
                form.getEmail(),
                form.getWebsite(),
                form.toAddress(),
                form.toOwner(),
                null  // userId will be set from authenticated user in a future update
        );
        return "redirect:/dashboard";
    }

    @DeleteMapping("/{id}")
    public String deleteLabel(@PathVariable Long id) {
        labelCRUDHandler.delete(id);
        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/address")
    public String updateAddress(@PathVariable Long id, UpdateAddressForm form) {
        labelCRUDHandler.updateAddress(id, form.toAddress());
        return "redirect:/labels/" + id;
    }

    @PutMapping("/{id}")
    public String updateLabel(@PathVariable Long id, UpdateLabelForm form) {
        labelCRUDHandler.updateLabel(
                id,
                form.getLabelName(),
                form.getEmail(),
                form.getWebsite(),
                form.toAddress(),
                form.toOwner()
        );
        return "redirect:/labels/" + id;
    }

}
