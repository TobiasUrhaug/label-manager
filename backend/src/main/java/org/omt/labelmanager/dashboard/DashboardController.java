package org.omt.labelmanager.dashboard;

import java.util.List;
import org.omt.labelmanager.catalog.artist.api.ArtistQueryApi;
import org.omt.labelmanager.catalog.artist.domain.Artist;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.label.domain.Label;
import org.omt.labelmanager.identity.api.user.AppUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final LabelQueryApi labelQueryFacade;
    private final ArtistQueryApi artistQueryApi;

    public DashboardController(LabelQueryApi labelQueryFacade, ArtistQueryApi artistQueryApi) {
        this.labelQueryFacade = labelQueryFacade;
        this.artistQueryApi = artistQueryApi;
    }

    record DashboardResponse(List<Label> labels, List<Artist> artists) {}

    @GetMapping
    public DashboardResponse overview(@AuthenticationPrincipal AppUserDetails user) {
        log.debug("Loading dashboard for user {}", user.getId());
        var labels = labelQueryFacade.getLabelsForUser(user.getId());
        var artists = artistQueryApi.getArtistsForUser(user.getId());
        return new DashboardResponse(labels, artists);
    }
}
