package org.omt.labelmanager.catalog.label.api;

import org.omt.labelmanager.catalog.artist.api.ArtistQueryApi;
import org.omt.labelmanager.catalog.artist.domain.Artist;
import org.omt.labelmanager.catalog.release.domain.Release;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.catalog.label.domain.Label;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.distribution.distributor.domain.Distributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


@Controller
@RequestMapping("/labels")
public class LabelController {

    private static final Logger log = LoggerFactory.getLogger(LabelController.class);

    private final LabelCommandApi labelCommandHandler;
    private final LabelQueryApi labelQueryFacade;
    private final ReleaseQueryApi releaseQueryFacade;
    private final ArtistQueryApi artistQueryApi;
    private final DistributorQueryApi distributorQueryApi;

    public LabelController(
            LabelCommandApi labelCommandHandler, LabelQueryApi labelQueryFacade,
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

    @GetMapping("/{id}")
    public String labelView(
            @AuthenticationPrincipal AppUserDetails user,
            @PathVariable Long id,
            Model model
    ) {
        Label label =
                labelQueryFacade
                        .findById(id)
                        .orElseThrow(() -> {
                            log.warn("Label with id {} not found", id);
                            return new ResponseStatusException(HttpStatus.NOT_FOUND);
                        });

        List<Release> releases = releaseQueryFacade.getReleasesForLabel(id);
        List<Artist> artists = artistQueryApi.getArtistsForUser(user.getId());
        List<Distributor> distributors = distributorQueryApi.findByLabelId(id);

        model.addAttribute("name", label.name());
        model.addAttribute("id", id);
        model.addAttribute("email", label.email());
        model.addAttribute("website", label.website());
        model.addAttribute("address", label.address());
        model.addAttribute("owner", label.owner());
        model.addAttribute("releases", releases);
        model.addAttribute("artists", artists);
        model.addAttribute("allFormats", ReleaseFormat.values());
        model.addAttribute("distributors", distributors);
        model.addAttribute("allChannelTypes", ChannelType.values());

        return "labels/label";
    }

    @PostMapping
    public String createLabel(
            @AuthenticationPrincipal AppUserDetails user,
            CreateLabelForm form
    ) {
        labelCommandHandler.createLabel(
                form.getLabelName(),
                form.getEmail(),
                form.getWebsite(),
                form.toAddress(),
                form.toOwner(),
                user.getId()
        );
        return "redirect:/dashboard";
    }

    @DeleteMapping("/{id}")
    public String deleteLabel(@PathVariable Long id) {
        labelCommandHandler.delete(id);
        return "redirect:/dashboard";
    }

    @PutMapping("/{id}")
    public String updateLabel(@PathVariable Long id, UpdateLabelForm form) {
        labelCommandHandler.updateLabel(
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
