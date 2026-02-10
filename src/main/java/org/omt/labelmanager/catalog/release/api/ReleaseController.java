package org.omt.labelmanager.catalog.release.api;

import org.omt.labelmanager.catalog.application.ArtistCRUDHandler;
import org.omt.labelmanager.catalog.domain.artist.Artist;
import org.omt.labelmanager.catalog.release.Release;
import org.omt.labelmanager.catalog.release.ReleaseFormat;
import org.omt.labelmanager.catalog.release.Track;
import org.omt.labelmanager.finance.cost.Cost;
import org.omt.labelmanager.finance.cost.api.CostQueryApi;
import org.omt.labelmanager.finance.cost.CostType;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.omt.labelmanager.inventory.allocation.AllocationQueryService;
import org.omt.labelmanager.inventory.allocation.ChannelAllocation;
import org.omt.labelmanager.inventory.api.AllocationView;
import org.omt.labelmanager.inventory.api.ProductionRunWithAllocation;
import org.omt.labelmanager.inventory.application.ProductionRunQueryService;
import org.omt.labelmanager.inventory.application.SalesChannelQueryService;
import org.omt.labelmanager.inventory.domain.ProductionRun;
import org.omt.labelmanager.inventory.domain.SalesChannel;
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

    private final ReleaseCommandFacade releaseCommandFacade;
    private final ReleaseQueryFacade releaseQueryFacade;
    private final ArtistCRUDHandler artistCRUDHandler;
    private final CostQueryApi costQueryFacade;
    private final ProductionRunQueryService productionRunQueryService;
    private final AllocationQueryService allocationQueryService;
    private final SalesChannelQueryService salesChannelQueryService;

    public ReleaseController(
            ReleaseCommandFacade releaseCommandFacade,
            ReleaseQueryFacade releaseQueryFacade,
            ArtistCRUDHandler artistCRUDHandler,
            CostQueryApi costQueryFacade,
            ProductionRunQueryService productionRunQueryService,
            AllocationQueryService allocationQueryService,
            SalesChannelQueryService salesChannelQueryService
    ) {
        this.releaseCommandFacade = releaseCommandFacade;
        this.releaseQueryFacade = releaseQueryFacade;
        this.artistCRUDHandler = artistCRUDHandler;
        this.costQueryFacade = costQueryFacade;
        this.productionRunQueryService = productionRunQueryService;
        this.allocationQueryService = allocationQueryService;
        this.salesChannelQueryService = salesChannelQueryService;
    }

    @PostMapping
    public String createRelease(
            @PathVariable Long labelId,
            @ModelAttribute CreateReleaseForm form
    ) {
        LocalDate date = LocalDate.parse(form.getReleaseDate());

        releaseCommandFacade.createRelease(
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
        Release release = releaseQueryFacade
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
                artistCRUDHandler.getArtistsForUser(user.getId());
        Map<Long, Artist> artistMap = allArtists.stream()
                .collect(Collectors.toMap(
                        Artist::id, Function.identity()
                ));
        List<String> releaseArtistNames =
                resolveArtistNames(release.artistIds(), artistMap);
        List<Cost> costs =
                costQueryFacade.getCostsForRelease(releaseId);
        List<ProductionRun> productionRuns =
                productionRunQueryService
                        .getProductionRunsForRelease(releaseId);
        List<SalesChannel> salesChannels =
                salesChannelQueryService
                        .getSalesChannelsForLabel(labelId);
        List<ProductionRunWithAllocation>
                productionRunsWithAllocation =
                productionRuns.stream()
                        .map(run ->
                                buildProductionRunWithAllocation(
                                        run, salesChannels
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
        model.addAttribute("salesChannels", salesChannels);

        return "/releases/release";
    }

    @PutMapping("/{releaseId}")
    public String updateRelease(
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            @ModelAttribute UpdateReleaseForm form
    ) {
        LocalDate date = LocalDate.parse(form.getReleaseDate());

        releaseCommandFacade.updateRelease(
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
        releaseCommandFacade.delete(releaseId);
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
                        track.position()
                ))
                .toList();
    }

    private ProductionRunWithAllocation
            buildProductionRunWithAllocation(
                    ProductionRun run,
                    List<SalesChannel> salesChannels
    ) {
        List<ChannelAllocation> allocations =
                allocationQueryService
                        .getAllocationsForProductionRun(run.id());
        List<AllocationView> allocationViews =
                allocations.stream()
                        .map(alloc -> new AllocationView(
                                alloc.id(),
                                findChannelName(
                                        alloc.salesChannelId(),
                                        salesChannels
                                ),
                                alloc.quantity(),
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
            List<SalesChannel> channels
    ) {
        return channels.stream()
                .filter(ch -> ch.id().equals(channelId))
                .findFirst()
                .map(SalesChannel::name)
                .orElse("Unknown Channel");
    }
}
