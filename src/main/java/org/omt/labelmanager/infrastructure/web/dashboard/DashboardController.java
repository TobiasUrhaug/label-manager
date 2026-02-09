package org.omt.labelmanager.infrastructure.web.dashboard;

import org.omt.labelmanager.catalog.application.ArtistCRUDHandler;
import org.omt.labelmanager.catalog.label.api.LabelQueryFacade;
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

    private final LabelQueryFacade labelQueryFacade;
    private final ArtistCRUDHandler artistCRUDHandler;

    public DashboardController(
            LabelQueryFacade labelQueryFacade,
            ArtistCRUDHandler artistCRUDHandler
    ) {
        this.labelQueryFacade = labelQueryFacade;
        this.artistCRUDHandler = artistCRUDHandler;
    }

    @GetMapping("/dashboard")
    public String overview(@AuthenticationPrincipal AppUserDetails user, Model model) {
        log.debug("Loading dashboard for user {}", user.getId());
        var labels = labelQueryFacade.getLabelsForUser(user.getId());
        var artists = artistCRUDHandler.getArtistsForUser(user.getId());

        model.addAttribute("labels", labels);
        model.addAttribute("artists", artists);

        return "dashboard";
    }

}
