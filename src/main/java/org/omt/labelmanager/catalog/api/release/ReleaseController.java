package org.omt.labelmanager.catalog.api.release;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.omt.labelmanager.catalog.application.ArtistCRUDHandler;
import org.omt.labelmanager.catalog.application.ReleaseCRUDHandler;
import org.omt.labelmanager.catalog.domain.artist.Artist;
import org.omt.labelmanager.catalog.domain.release.Release;
import org.omt.labelmanager.catalog.domain.release.ReleaseFormat;
import org.omt.labelmanager.finance.application.CostQueryService;
import org.omt.labelmanager.finance.domain.cost.Cost;
import org.omt.labelmanager.finance.domain.cost.CostType;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.omt.labelmanager.productionrun.application.ProductionRunQueryService;
import org.omt.labelmanager.productionrun.domain.ProductionRun;
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
    private final CostQueryService costQueryService;
    private final ProductionRunQueryService productionRunQueryService;

    public ReleaseController(
            ReleaseCRUDHandler releaseCRUDHandler,
            ArtistCRUDHandler artistCRUDHandler,
            CostQueryService costQueryService,
            ProductionRunQueryService productionRunQueryService
    ) {
        this.releaseCRUDHandler = releaseCRUDHandler;
        this.artistCRUDHandler = artistCRUDHandler;
        this.costQueryService = costQueryService;
        this.productionRunQueryService = productionRunQueryService;
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
                form.toTrackInputs(),
                form.toReleaseFormats()
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
        List<Cost> costs = costQueryService.getCostsForRelease(releaseId);
        List<ProductionRun> productionRuns =
                productionRunQueryService.getProductionRunsForRelease(releaseId);
        Map<ReleaseFormat, Integer> productionRunTotals =
                productionRunQueryService.getTotalsForRelease(releaseId);
        List<ReleaseFormat> physicalFormats = Arrays.stream(ReleaseFormat.values())
                .filter(ReleaseFormat::isPhysical)
                .toList();

        model.addAttribute("name", release.name());
        model.addAttribute("labelId", labelId);
        model.addAttribute("releaseId", releaseId);
        model.addAttribute("releaseDate", release.releaseDate());
        model.addAttribute("artists", release.artists());
        model.addAttribute("tracks", release.tracks());
        model.addAttribute("formats", release.formats());
        model.addAttribute("allArtists", allArtists);
        model.addAttribute("allFormats", ReleaseFormat.values());
        model.addAttribute("costs", costs);
        model.addAttribute("allCostTypes", CostType.values());
        model.addAttribute("productionRuns", productionRuns);
        model.addAttribute("productionRunTotals", productionRunTotals);
        model.addAttribute("physicalFormats", physicalFormats);

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
                form.toTrackInputs(),
                form.toReleaseFormats()
        );

        return "redirect:/labels/" + labelId + "/releases/" + releaseId;
    }

    @DeleteMapping("/{releaseId}")
    public String deleteRelease(@PathVariable Long labelId, @PathVariable Long releaseId) {
        releaseCRUDHandler.delete(releaseId);
        return "redirect:/labels/" + labelId;
    }
}
