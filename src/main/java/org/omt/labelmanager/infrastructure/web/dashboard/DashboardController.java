package org.omt.labelmanager.infrastructure.web.dashboard;

import org.omt.labelmanager.catalog.application.ArtistCRUDHandler;
import org.omt.labelmanager.catalog.application.LabelCRUDHandler;
import org.omt.labelmanager.identity.user.AppUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final LabelCRUDHandler labelCRUDHandler;
    private final ArtistCRUDHandler artistCRUDHandler;

    public DashboardController(LabelCRUDHandler labelCRUDHandler,
                               ArtistCRUDHandler artistCRUDHandler) {
        this.labelCRUDHandler = labelCRUDHandler;
        this.artistCRUDHandler = artistCRUDHandler;
    }

    @GetMapping("/dashboard")
    public String overview(@AuthenticationPrincipal AppUserDetails user, Model model) {
        log.debug("Loading dashboard for user {}", user.getId());
        var labels = labelCRUDHandler.getLabelsForUser(user.getId());
        var artists = artistCRUDHandler.getArtistsForUser(user.getId());

        model.addAttribute("labels", labels);
        model.addAttribute("artists", artists);

        return "dashboard";
    }

}
