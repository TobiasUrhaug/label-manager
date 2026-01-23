package org.omt.labelmanager.release.api;

import java.time.LocalDate;
import java.util.List;
import org.omt.labelmanager.artist.Artist;
import org.omt.labelmanager.artist.ArtistCRUDHandler;
import org.omt.labelmanager.release.Release;
import org.omt.labelmanager.release.ReleaseCRUDHandler;
import org.omt.labelmanager.user.AppUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/labels/{labelId}/releases")
public class ReleaseController {

    private static final Logger log = LoggerFactory.getLogger(ReleaseController.class);

    private final ReleaseCRUDHandler releaseCRUDHandler;
    private final ArtistCRUDHandler artistCRUDHandler;

    public ReleaseController(
            ReleaseCRUDHandler releaseCRUDHandler,
            ArtistCRUDHandler artistCRUDHandler
    ) {
        this.releaseCRUDHandler = releaseCRUDHandler;
        this.artistCRUDHandler = artistCRUDHandler;
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
            @AuthenticationPrincipal AppUserDetails user,
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            Model model
    ) {
        Release release = releaseCRUDHandler
                .findById(releaseId)
                .orElseThrow(() -> {
                    log.warn("Release with id {} not found", releaseId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND);
                });

        List<Artist> allArtists = artistCRUDHandler.getArtistsForUser(user.getId());

        model.addAttribute("name", release.name());
        model.addAttribute("labelId", labelId);
        model.addAttribute("releaseId", releaseId);
        model.addAttribute("releaseDate", release.releaseDate());
        model.addAttribute("artists", release.artists());
        model.addAttribute("tracks", release.tracks());
        model.addAttribute("allArtists", allArtists);

        return "/releases/release";
    }

    @PutMapping("/{releaseId}")
    public String updateRelease(
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            @ModelAttribute UpdateReleaseForm form
    ) {
        LocalDate date = LocalDate.parse(form.getReleaseDate());

        releaseCRUDHandler.updateRelease(
                releaseId,
                form.getReleaseName(),
                date,
                form.getArtistIds(),
                form.toTrackInputs()
        );

        return "redirect:/labels/" + labelId + "/releases/" + releaseId;
    }

    @DeleteMapping("/{releaseId}")
    public String deleteRelease(@PathVariable Long labelId, @PathVariable Long releaseId) {
        releaseCRUDHandler.delete(releaseId);
        return "redirect:/labels/" + labelId;
    }
}
