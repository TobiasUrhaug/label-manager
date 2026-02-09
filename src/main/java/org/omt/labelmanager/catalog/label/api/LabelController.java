package org.omt.labelmanager.catalog.label.api;

import org.omt.labelmanager.catalog.application.ArtistCRUDHandler;
import org.omt.labelmanager.catalog.application.ReleaseCRUDHandler;
import org.omt.labelmanager.catalog.domain.artist.Artist;
import org.omt.labelmanager.catalog.domain.release.Release;
import org.omt.labelmanager.catalog.domain.release.ReleaseFormat;
import org.omt.labelmanager.catalog.label.Label;
import org.omt.labelmanager.catalog.label.LabelCRUDHandler;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.omt.labelmanager.inventory.application.SalesChannelQueryService;
import org.omt.labelmanager.inventory.domain.ChannelType;
import org.omt.labelmanager.inventory.domain.SalesChannel;
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

    private final LabelCRUDHandler labelCRUDHandler;
    private final LabelQueryFacade labelQueryFacade;
    private final ReleaseCRUDHandler releaseCRUDHandler;
    private final ArtistCRUDHandler artistCRUDHandler;
    private final SalesChannelQueryService salesChannelQueryService;

    public LabelController(
            LabelCRUDHandler labelCRUDHandler, LabelQueryFacade labelQueryFacade,
            ReleaseCRUDHandler releaseCRUDHandler,
            ArtistCRUDHandler artistCRUDHandler,
            SalesChannelQueryService salesChannelQueryService
    ) {
        this.labelCRUDHandler = labelCRUDHandler;
        this.labelQueryFacade = labelQueryFacade;
        this.releaseCRUDHandler = releaseCRUDHandler;
        this.artistCRUDHandler = artistCRUDHandler;
        this.salesChannelQueryService = salesChannelQueryService;
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

        List<Release> releases = releaseCRUDHandler.getReleasesForLabel(id);
        List<Artist> artists = artistCRUDHandler.getArtistsForUser(user.getId());
        List<SalesChannel> salesChannels = salesChannelQueryService.getSalesChannelsForLabel(id);

        model.addAttribute("name", label.name());
        model.addAttribute("id", id);
        model.addAttribute("email", label.email());
        model.addAttribute("website", label.website());
        model.addAttribute("address", label.address());
        model.addAttribute("owner", label.owner());
        model.addAttribute("releases", releases);
        model.addAttribute("artists", artists);
        model.addAttribute("allFormats", ReleaseFormat.values());
        model.addAttribute("salesChannels", salesChannels);
        model.addAttribute("allChannelTypes", ChannelType.values());

        return "labels/label";
    }

    @PostMapping
    public String createLabel(
            @AuthenticationPrincipal AppUserDetails user,
            CreateLabelForm form
    ) {
        labelCRUDHandler.createLabel(
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
        labelCRUDHandler.delete(id);
        return "redirect:/dashboard";
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
