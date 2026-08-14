package org.omt.labelmanager.web.inventory;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.omt.labelmanager.distribution.distributor.api.Distributor;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.inventory.LocationType;
import org.omt.labelmanager.inventory.inventorymovement.InventoryMovement;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunCommandApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;
import org.omt.labelmanager.shared.Format;
import org.omt.labelmanager.web.LabelScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/labels/{labelId}/releases/{releaseId}/production-runs")
public class ProductionRunController {

    private final ProductionRunCommandApi commandApi;
    private final ProductionRunQueryApi queryApi;
    private final InventoryMovementQueryApi inventoryMovementQueryApi;
    private final DistributorQueryApi distributorQueryApi;
    private final LabelScope labelScope;

    public ProductionRunController(
            ProductionRunCommandApi commandApi,
            ProductionRunQueryApi queryApi,
            InventoryMovementQueryApi inventoryMovementQueryApi,
            DistributorQueryApi distributorQueryApi,
            LabelScope labelScope) {
        this.commandApi = commandApi;
        this.queryApi = queryApi;
        this.inventoryMovementQueryApi = inventoryMovementQueryApi;
        this.distributorQueryApi = distributorQueryApi;
        this.labelScope = labelScope;
    }

    record AddProductionRunRequest(
            Format format,
            String description,
            String manufacturer,
            LocalDate manufacturingDate,
            int quantity) {}

    /**
     * The release's production runs, each with its current inventory and movement history.
     *
     * <p>Replaces the {@code productionRuns} field of the release detail response. Distributor
     * names are resolved once for the whole collection rather than per movement.
     */
    @GetMapping
    public List<ProductionRunWithAllocation> productionRuns(
            @PathVariable Long labelId, @PathVariable Long releaseId) {
        labelScope.requireRelease(labelId, releaseId);
        List<Distributor> distributors = distributorQueryApi.findByLabelId(labelId);
        return queryApi.findByReleaseId(releaseId).stream()
                .map(run -> withAllocation(run, distributors))
                .toList();
    }

    @PostMapping
    public ResponseEntity<Void> addProductionRun(
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            @RequestBody AddProductionRunRequest request) {
        labelScope.requireRelease(labelId, releaseId);
        commandApi.createProductionRun(
                releaseId,
                request.format(),
                request.description(),
                request.manufacturer(),
                request.manufacturingDate(),
                request.quantity());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{productionRunId}")
    public ResponseEntity<Void> deleteProductionRun(
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            @PathVariable Long productionRunId) {
        labelScope.requireRelease(labelId, releaseId);
        boolean underThisRelease =
                queryApi.findById(productionRunId)
                        .map(run -> releaseId.equals(run.releaseId()))
                        .orElse(false);
        if (!underThisRelease) {
            throw new EntityNotFoundException(
                    "Production run "
                            + productionRunId
                            + " does not belong to release "
                            + releaseId);
        }
        commandApi.delete(productionRunId);
        return ResponseEntity.noContent().build();
    }

    private ProductionRunWithAllocation withAllocation(
            ProductionRun run, List<Distributor> distributors) {
        int warehouseInventory =
                run.quantity() + inventoryMovementQueryApi.getWarehouseInventory(run.id());
        int bandcampInventory = inventoryMovementQueryApi.getBandcampInventory(run.id());
        Map<Long, Integer> currentByDistributor =
                inventoryMovementQueryApi.getCurrentInventoryByDistributor(run.id());
        List<InventoryMovement> movements =
                inventoryMovementQueryApi.getMovementsForProductionRun(run.id());

        return new ProductionRunWithAllocation(
                run,
                bandcampInventory,
                warehouseInventory,
                distributorInventories(currentByDistributor, distributors),
                movementHistory(movements, distributors));
    }

    private List<DistributorInventoryView> distributorInventories(
            Map<Long, Integer> currentByDistributor, List<Distributor> distributors) {
        return currentByDistributor.entrySet().stream()
                .map(
                        entry ->
                                new DistributorInventoryView(
                                        distributorName(entry.getKey(), distributors),
                                        entry.getValue()))
                .sorted(Comparator.comparing(DistributorInventoryView::name))
                .toList();
    }

    private List<MovementHistoryView> movementHistory(
            List<InventoryMovement> movements, List<Distributor> distributors) {
        return movements.stream()
                .map(
                        m ->
                                new MovementHistoryView(
                                        m.occurredAt(),
                                        m.movementType(),
                                        locationName(
                                                m.fromLocationType(),
                                                m.fromLocationId(),
                                                distributors),
                                        locationName(
                                                m.toLocationType(), m.toLocationId(), distributors),
                                        m.quantity()))
                .toList();
    }

    private String locationName(
            LocationType locationType, Long locationId, List<Distributor> distributors) {
        return switch (locationType) {
            case WAREHOUSE -> "Warehouse";
            case EXTERNAL -> "External (sold)";
            case DISTRIBUTOR -> distributorName(locationId, distributors);
            case BANDCAMP -> "Bandcamp";
        };
    }

    private String distributorName(Long distributorId, List<Distributor> distributors) {
        return distributors.stream()
                .filter(d -> d.id().equals(distributorId))
                .findFirst()
                .map(Distributor::name)
                .orElse("Unknown");
    }
}
