package org.omt.labelmanager.catalog.release.api;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.omt.labelmanager.catalog.artist.api.ArtistQueryApi;
import org.omt.labelmanager.catalog.artist.domain.Artist;
import org.omt.labelmanager.catalog.release.domain.Release;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.catalog.release.domain.Track;
import org.omt.labelmanager.catalog.release.domain.TrackDuration;
import org.omt.labelmanager.catalog.release.domain.TrackInput;
import org.omt.labelmanager.distribution.distributor.Distributor;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.finance.cost.api.CostQueryApi;
import org.omt.labelmanager.finance.cost.domain.Cost;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.omt.labelmanager.inventory.LocationType;
import org.omt.labelmanager.inventory.api.DistributorInventoryView;
import org.omt.labelmanager.inventory.api.MovementHistoryView;
import org.omt.labelmanager.inventory.api.ProductionRunWithAllocation;
import org.omt.labelmanager.inventory.inventorymovement.InventoryMovement;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;
import org.omt.labelmanager.sales.sale.api.SaleQueryApi;
import org.omt.labelmanager.sales.sale.domain.Sale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/labels/{labelId}/releases")
public class ReleaseController {

    private static final Logger log = LoggerFactory.getLogger(ReleaseController.class);

    private final ReleaseCommandApi releaseCommandApi;
    private final ReleaseQueryApi releaseQueryApi;
    private final ArtistQueryApi artistQueryApi;
    private final CostQueryApi costQueryFacade;
    private final ProductionRunQueryApi productionRunQueryApi;
    private final DistributorQueryApi distributorQueryApi;
    private final InventoryMovementQueryApi inventoryMovementQueryApi;
    private final SaleQueryApi saleQueryApi;

    public ReleaseController(
            ReleaseCommandApi releaseCommandApi,
            ReleaseQueryApi releaseQueryApi,
            ArtistQueryApi artistQueryApi,
            CostQueryApi costQueryFacade,
            ProductionRunQueryApi productionRunQueryApi,
            DistributorQueryApi distributorQueryApi,
            InventoryMovementQueryApi inventoryMovementQueryApi,
            SaleQueryApi saleQueryApi
    ) {
        this.releaseCommandApi = releaseCommandApi;
        this.releaseQueryApi = releaseQueryApi;
        this.artistQueryApi = artistQueryApi;
        this.costQueryFacade = costQueryFacade;
        this.productionRunQueryApi = productionRunQueryApi;
        this.distributorQueryApi = distributorQueryApi;
        this.inventoryMovementQueryApi = inventoryMovementQueryApi;
        this.saleQueryApi = saleQueryApi;
    }

    record TrackRequest(
            List<Long> artistIds,
            String name,
            String duration,
            List<Long> remixerIds
    ) {
        TrackInput toTrackInput(int position) {
            return new TrackInput(
                    artistIds != null ? artistIds : List.of(),
                    name,
                    TrackDuration.parse(duration),
                    position,
                    remixerIds != null ? remixerIds : List.of()
            );
        }
    }

    record CreateReleaseRequest(
            String releaseName,
            String releaseDate,
            List<Long> artistIds,
            List<TrackRequest> tracks,
            List<String> formats
    ) {
        List<TrackInput> toTrackInputs() {
            if (tracks == null) return List.of();
            return IntStream.range(0, tracks.size())
                    .mapToObj(i -> tracks.get(i).toTrackInput(i + 1))
                    .toList();
        }

        Set<ReleaseFormat> toReleaseFormats() {
            if (formats == null) return Set.of();
            return formats.stream().map(ReleaseFormat::valueOf).collect(Collectors.toSet());
        }
    }

    record UpdateReleaseRequest(
            String releaseName,
            String releaseDate,
            List<Long> artistIds,
            List<TrackRequest> tracks,
            List<String> formats
    ) {
        List<TrackInput> toTrackInputs() {
            if (tracks == null) return List.of();
            return IntStream.range(0, tracks.size())
                    .mapToObj(i -> tracks.get(i).toTrackInput(i + 1))
                    .toList();
        }

        Set<ReleaseFormat> toReleaseFormats() {
            if (formats == null) return Set.of();
            return formats.stream().map(ReleaseFormat::valueOf).collect(Collectors.toSet());
        }
    }

    record ReleaseDetailResponse(
            Long releaseId,
            Long labelId,
            String name,
            LocalDate releaseDate,
            List<Artist> artists,
            List<TrackView> tracks,
            Set<ReleaseFormat> formats,
            List<Cost> costs,
            List<ProductionRunWithAllocation> productionRuns,
            List<Distributor> distributors,
            List<ReleaseSaleView> releaseSales,
            int totalUnitsSold
    ) {}

    @GetMapping("/{releaseId}")
    public ReleaseDetailResponse release(
            @AuthenticationPrincipal AppUserDetails user,
            @PathVariable Long labelId,
            @PathVariable Long releaseId
    ) {
        Release release = releaseQueryApi
                .findById(releaseId)
                .orElseThrow(() -> {
                    log.warn("Release with id {} not found", releaseId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND);
                });

        List<Artist> allArtists = artistQueryApi.getArtistsForUser(user.getId());
        Map<Long, Artist> artistMap = allArtists.stream()
                .collect(Collectors.toMap(Artist::id, Function.identity()));
        List<Cost> costs = costQueryFacade.getCostsForRelease(releaseId);
        List<ProductionRun> productionRuns = productionRunQueryApi.findByReleaseId(releaseId);
        List<Distributor> distributors = distributorQueryApi.findByLabelId(labelId);
        List<ProductionRunWithAllocation> productionRunsWithAllocation = productionRuns.stream()
                .map(run -> buildProductionRunWithAllocation(run, distributors))
                .toList();
        List<ReleaseSaleView> releaseSales = buildReleaseSales(productionRuns, distributors);
        int totalUnitsSold = releaseSales.stream().mapToInt(ReleaseSaleView::totalUnits).sum();

        return new ReleaseDetailResponse(
                releaseId,
                labelId,
                release.name(),
                release.releaseDate(),
                resolveArtists(release.artistIds(), artistMap),
                resolveTrackArtists(release.tracks(), artistMap),
                release.formats(),
                costs,
                productionRunsWithAllocation,
                distributors,
                releaseSales,
                totalUnitsSold
        );
    }

    @PostMapping
    public ResponseEntity<Void> createRelease(
            @PathVariable Long labelId,
            @RequestBody CreateReleaseRequest request
    ) {
        releaseCommandApi.createRelease(
                request.releaseName(),
                LocalDate.parse(request.releaseDate()),
                labelId,
                request.artistIds() != null ? request.artistIds() : List.of(),
                request.toTrackInputs(),
                request.toReleaseFormats()
        );
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{releaseId}")
    public ResponseEntity<Void> updateRelease(
            @PathVariable Long releaseId,
            @RequestBody UpdateReleaseRequest request
    ) {
        releaseCommandApi.updateRelease(
                releaseId,
                request.releaseName(),
                LocalDate.parse(request.releaseDate()),
                request.artistIds() != null ? request.artistIds() : List.of(),
                request.toTrackInputs(),
                request.toReleaseFormats()
        );
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{releaseId}")
    public ResponseEntity<Void> deleteRelease(@PathVariable Long releaseId) {
        releaseCommandApi.delete(releaseId);
        return ResponseEntity.noContent().build();
    }

    private List<Artist> resolveArtists(List<Long> artistIds, Map<Long, Artist> artistMap) {
        return artistIds.stream()
                .map(artistMap::get)
                .filter(a -> a != null)
                .toList();
    }

    private List<TrackView> resolveTrackArtists(List<Track> tracks, Map<Long, Artist> artistMap) {
        return tracks.stream()
                .map(track -> new TrackView(
                        track.id(),
                        resolveArtists(track.artistIds(), artistMap),
                        track.name(),
                        track.duration(),
                        track.position(),
                        resolveArtists(track.remixerIds(), artistMap)
                ))
                .toList();
    }

    private ProductionRunWithAllocation buildProductionRunWithAllocation(
            ProductionRun run,
            List<Distributor> distributors
    ) {
        int warehouseInventory = run.quantity() + inventoryMovementQueryApi.getWarehouseInventory(run.id());
        int bandcampInventory = inventoryMovementQueryApi.getBandcampInventory(run.id());
        Map<Long, Integer> currentByDistributor =
                inventoryMovementQueryApi.getCurrentInventoryByDistributor(run.id());
        List<InventoryMovement> movements =
                inventoryMovementQueryApi.getMovementsForProductionRun(run.id());

        return new ProductionRunWithAllocation(
                run,
                bandcampInventory,
                warehouseInventory,
                buildDistributorInventories(currentByDistributor, distributors),
                buildMovementHistory(movements, distributors)
        );
    }

    private List<DistributorInventoryView> buildDistributorInventories(
            Map<Long, Integer> currentByDistributor,
            List<Distributor> distributors
    ) {
        return currentByDistributor.entrySet().stream()
                .map(entry -> new DistributorInventoryView(
                        findDistributorName(entry.getKey(), distributors),
                        entry.getValue()
                ))
                .sorted(Comparator.comparing(DistributorInventoryView::name))
                .toList();
    }

    private List<MovementHistoryView> buildMovementHistory(
            List<InventoryMovement> movements,
            List<Distributor> distributors
    ) {
        return movements.stream()
                .map(m -> new MovementHistoryView(
                        m.occurredAt(),
                        m.movementType(),
                        formatLocation(m.fromLocationType(), m.fromLocationId(), distributors),
                        formatLocation(m.toLocationType(), m.toLocationId(), distributors),
                        m.quantity()
                ))
                .toList();
    }

    private String formatLocation(
            LocationType locationType,
            Long locationId,
            List<Distributor> distributors
    ) {
        return switch (locationType) {
            case WAREHOUSE -> "Warehouse";
            case EXTERNAL -> "External (sold)";
            case DISTRIBUTOR -> findDistributorName(locationId, distributors);
            case BANDCAMP -> "Bandcamp";
        };
    }

    private String findDistributorName(Long distributorId, List<Distributor> distributors) {
        return distributors.stream()
                .filter(d -> d.id().equals(distributorId))
                .findFirst()
                .map(Distributor::name)
                .orElse("Unknown");
    }

    private List<ReleaseSaleView> buildReleaseSales(
            List<ProductionRun> productionRuns,
            List<Distributor> distributors
    ) {
        return productionRuns.stream()
                .flatMap(run -> saleQueryApi.getSalesForProductionRun(run.id()).stream())
                .map(sale -> toReleaseSaleView(sale, distributors))
                .sorted(Comparator.comparing(ReleaseSaleView::saleDate).reversed())
                .toList();
    }

    private ReleaseSaleView toReleaseSaleView(Sale sale, List<Distributor> distributors) {
        int totalUnits = sale.lineItems().stream().mapToInt(item -> item.quantity()).sum();
        return new ReleaseSaleView(
                sale.id(),
                sale.saleDate(),
                findDistributorName(sale.distributorId(), distributors),
                totalUnits,
                sale.totalAmount()
        );
    }
}
