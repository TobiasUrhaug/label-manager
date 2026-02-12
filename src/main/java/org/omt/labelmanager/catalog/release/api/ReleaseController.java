package org.omt.labelmanager.catalog.release.api;

import org.omt.labelmanager.catalog.artist.api.ArtistQueryApi;
import org.omt.labelmanager.catalog.artist.domain.Artist;
import org.omt.labelmanager.catalog.release.domain.Release;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.catalog.release.domain.Track;
import org.omt.labelmanager.finance.cost.domain.Cost;
import org.omt.labelmanager.finance.cost.api.CostQueryApi;
import org.omt.labelmanager.finance.cost.domain.CostType;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.omt.labelmanager.inventory.allocation.AllocationQueryService;
import org.omt.labelmanager.inventory.allocation.ChannelAllocation;
import org.omt.labelmanager.inventory.api.AllocationView;
import org.omt.labelmanager.inventory.api.ProductionRunWithAllocation;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;
import org.omt.labelmanager.distribution.distributor.domain.Distributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/labels/{labelId}/releases")
public class ReleaseController {

    private static final Logger log =
            LoggerFactory.getLogger(ReleaseController.class);

    private final ReleaseCommandApi releaseCommandApi;
    private final ReleaseQueryApi releaseQueryApi;
    private final ArtistQueryApi artistQueryApi;
    private final CostQueryApi costQueryFacade;
    private final ProductionRunQueryApi productionRunQueryApi;
    private final AllocationQueryService allocationQueryService;
    private final DistributorQueryApi distributorQueryApi;

    public ReleaseController(
            ReleaseCommandApi releaseCommandApi,
            ReleaseQueryApi releaseQueryApi,
            ArtistQueryApi artistQueryApi,
            CostQueryApi costQueryFacade,
            ProductionRunQueryApi productionRunQueryApi,
            AllocationQueryService allocationQueryService,
            DistributorQueryApi distributorQueryApi
    ) {
        this.releaseCommandApi = releaseCommandApi;
        this.releaseQueryApi = releaseQueryApi;
        this.artistQueryApi = artistQueryApi;
        this.costQueryFacade = costQueryFacade;
        this.productionRunQueryApi = productionRunQueryApi;
        this.allocationQueryService = allocationQueryService;
        this.distributorQueryApi = distributorQueryApi;
    }

    @PostMapping
    public String createRelease(
            @PathVariable Long labelId,
            @ModelAttribute CreateReleaseForm form
    ) {
        LocalDate date = LocalDate.parse(form.getReleaseDate());

        releaseCommandApi.createRelease(
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
        Release release = releaseQueryApi
                .findById(releaseId)
                .orElseThrow(() -> {
                    log.warn(
                            "Release with id {} not found",
                            releaseId
                    );
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND
                    );
                });

        List<Artist> allArtists =
                artistQueryApi.getArtistsForUser(user.getId());
        Map<Long, Artist> artistMap = allArtists.stream()
                .collect(Collectors.toMap(
                        Artist::id, Function.identity()
                ));
        List<String> releaseArtistNames =
                resolveArtistNames(release.artistIds(), artistMap);
        List<Cost> costs =
                costQueryFacade.getCostsForRelease(releaseId);
        List<ProductionRun> productionRuns =
                productionRunQueryApi
                        .findByReleaseId(releaseId);
        List<Distributor> distributors =
                distributorQueryApi
                        .findByLabelId(labelId);
        List<ProductionRunWithAllocation>
                productionRunsWithAllocation =
                productionRuns.stream()
                        .map(run ->
                                buildProductionRunWithAllocation(
                                        run, distributors
                                ))
                        .toList();
        List<ReleaseFormat> physicalFormats =
                Arrays.stream(ReleaseFormat.values())
                        .filter(ReleaseFormat::isPhysical)
                        .toList();

        model.addAttribute("name", release.name());
        model.addAttribute("labelId", labelId);
        model.addAttribute("releaseId", releaseId);
        model.addAttribute("releaseDate", release.releaseDate());
        model.addAttribute(
                "artists", resolveArtists(
                        release.artistIds(), artistMap
                )
        );
        model.addAttribute(
                "tracks", resolveTrackArtists(
                        release.tracks(), artistMap
                )
        );
        model.addAttribute("formats", release.formats());
        model.addAttribute("allArtists", allArtists);
        model.addAttribute(
                "allFormats", ReleaseFormat.values()
        );
        model.addAttribute("costs", costs);
        model.addAttribute(
                "allCostTypes", CostType.values()
        );
        model.addAttribute(
                "productionRuns", productionRunsWithAllocation
        );
        model.addAttribute("physicalFormats", physicalFormats);
        model.addAttribute("distributors", distributors);

        return "/releases/release";
    }

    @PutMapping("/{releaseId}")
    public String updateRelease(
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            @ModelAttribute UpdateReleaseForm form
    ) {
        LocalDate date = LocalDate.parse(form.getReleaseDate());

        releaseCommandApi.updateRelease(
                releaseId,
                form.getReleaseName(),
                date,
                form.getArtistIds(),
                form.toTrackInputs(),
                form.toReleaseFormats()
        );

        return "redirect:/labels/" + labelId
                + "/releases/" + releaseId;
    }

    @DeleteMapping("/{releaseId}")
    public String deleteRelease(
            @PathVariable Long labelId,
            @PathVariable Long releaseId
    ) {
        releaseCommandApi.delete(releaseId);
        return "redirect:/labels/" + labelId;
    }

    private List<Artist> resolveArtists(
            List<Long> artistIds,
            Map<Long, Artist> artistMap
    ) {
        return artistIds.stream()
                .map(artistMap::get)
                .filter(a -> a != null)
                .toList();
    }

    private List<String> resolveArtistNames(
            List<Long> artistIds,
            Map<Long, Artist> artistMap
    ) {
        return artistIds.stream()
                .map(id -> {
                    Artist a = artistMap.get(id);
                    return a != null ? a.artistName() : "Unknown";
                })
                .toList();
    }

    private List<TrackView> resolveTrackArtists(
            List<Track> tracks,
            Map<Long, Artist> artistMap
    ) {
        return tracks.stream()
                .map(track -> new TrackView(
                        track.id(),
                        resolveArtists(
                                track.artistIds(), artistMap
                        ),
                        track.name(),
                        track.duration(),
                        track.position(),
                        resolveArtists(
                                track.remixerIds(), artistMap
                        )
                ))
                .toList();
    }

    private ProductionRunWithAllocation
            buildProductionRunWithAllocation(
                    ProductionRun run,
                    List<Distributor> distributors
    ) {
        List<ChannelAllocation> allocations =
                allocationQueryService
                        .getAllocationsForProductionRun(run.id());
        List<AllocationView> allocationViews =
                allocations.stream()
                        .map(alloc -> new AllocationView(
                                alloc.id(),
                                findChannelName(
                                        alloc.distributorId(),
                                        distributors
                                ),
                                alloc.quantity(),
                                alloc.unitsSold(),
                                alloc.unitsRemaining(),
                                alloc.allocatedAt()
                        ))
                        .toList();
        return new ProductionRunWithAllocation(
                run,
                allocationQueryService.getTotalAllocated(
                        run.id()
                ),
                allocationQueryService.getUnallocatedQuantity(
                        run.id()
                ),
                allocationViews
        );
    }

    private String findChannelName(
            Long channelId,
            List<Distributor> distributors
    ) {
        return distributors.stream()
                .filter(ch -> ch.id().equals(channelId))
                .findFirst()
                .map(Distributor::name)
                .orElse("Unknown Channel");
    }
}
