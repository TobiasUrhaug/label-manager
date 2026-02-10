package org.omt.labelmanager.dashboard;

import org.omt.labelmanager.catalog.artist.api.ArtistQueryApi;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final LabelQueryApi labelQueryFacade;
    private final ArtistQueryApi artistQueryApi;

    public DashboardController(
            LabelQueryApi labelQueryFacade,
            ArtistQueryApi artistQueryApi
    ) {
        this.labelQueryFacade = labelQueryFacade;
        this.artistQueryApi = artistQueryApi;
    }

    @GetMapping("/dashboard")
    public String overview(@AuthenticationPrincipal AppUserDetails user, Model model) {
        log.debug("Loading dashboard for user {}", user.getId());
        var labels = labelQueryFacade.getLabelsForUser(user.getId());
        var artists = artistQueryApi.getArtistsForUser(user.getId());

        model.addAttribute("labels", labels);
        model.addAttribute("artists", artists);

        return "dashboard";
    }

}
