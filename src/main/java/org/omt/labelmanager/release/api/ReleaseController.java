package org.omt.labelmanager.release.api;

import java.time.LocalDate;
import org.omt.labelmanager.release.Release;
import org.omt.labelmanager.release.ReleaseCRUDHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/labels/{labelId}/releases")
public class ReleaseController {

    private static final Logger log = LoggerFactory.getLogger(ReleaseController.class);

    private final ReleaseCRUDHandler releaseCRUDHandler;

    public ReleaseController(ReleaseCRUDHandler releaseCRUDHandler) {
        this.releaseCRUDHandler = releaseCRUDHandler;
    }

    @PostMapping
    public String createRelease(
            @PathVariable Long labelId,
            @ModelAttribute CreateReleaseForm form
    ) {
        LocalDate date = LocalDate.parse(form.getReleaseDate());

        releaseCRUDHandler.createRelease(
                form.getReleaseName(),
                date,
                labelId,
                form.getArtistIds(),
                form.toTrackInputs()
        );

        return "redirect:/labels/" + labelId;
    }

    @GetMapping("/{releaseId}")
    public String releaseView(
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            Model model) {
        Release release = releaseCRUDHandler
                .findById(releaseId)
                .orElseThrow(() -> {
                    log.warn("Release with id {} not found", releaseId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND);
                });

        model.addAttribute("name", release.name());
        model.addAttribute("labelId", labelId);
        model.addAttribute("releaseId", releaseId);
        model.addAttribute("releaseDate", release.releaseDate());
        model.addAttribute("artists", release.artists());
        model.addAttribute("tracks", release.tracks());

        return "/releases/release";
    }

    @DeleteMapping("/{releaseId}")
    public String deleteRelease(@PathVariable Long labelId, @PathVariable Long releaseId) {
        releaseCRUDHandler.delete(releaseId);
        return "redirect:/labels/" + labelId;
    }
}
