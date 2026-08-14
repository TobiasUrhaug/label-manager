package org.omt.labelmanager.inventory.productionrun.web;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.inventory.LocationType;
import org.omt.labelmanager.inventory.inventorymovement.InventoryMovement;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunCommandApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;
import org.omt.labelmanager.shared.Format;
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
    private final ReleaseQueryApi releaseQueryApi;

    public ProductionRunController(
            ProductionRunCommandApi commandApi,
            ProductionRunQueryApi queryApi,
            InventoryMovementQueryApi inventoryMovementQueryApi,
            ReleaseQueryApi releaseQueryApi) {
        this.commandApi = commandApi;
        this.queryApi = queryApi;
        this.inventoryMovementQueryApi = inventoryMovementQueryApi;
        this.releaseQueryApi = releaseQueryApi;
    }

    private void requireRelease(Long labelId, Long releaseId) {
        if (!releaseQueryApi.belongsToLabel(releaseId, labelId)) {
            throw new EntityNotFoundException(
                    "Release " + releaseId + " does not belong to label " + labelId);
        }
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
     * <p>Locations are reported as a type and an id, not as a resolved name. Naming a distributor
     * here would mean inventory reading from distribution — sideways, and only ever for display.
     * The caller already has {@code /api/labels/{labelId}/distributors} and can join once.
     */
    @GetMapping
    public List<ProductionRunWithAllocation> productionRuns(
            @PathVariable Long labelId, @PathVariable Long releaseId) {
        requireRelease(labelId, releaseId);
        return queryApi.findByReleaseId(releaseId).stream().map(this::withAllocation).toList();
    }

    @PostMapping
    public ResponseEntity<Void> addProductionRun(
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            @RequestBody AddProductionRunRequest request) {
        requireRelease(labelId, releaseId);
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
        requireRelease(labelId, releaseId);
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

    private ProductionRunWithAllocation withAllocation(ProductionRun run) {
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
                distributorInventories(currentByDistributor),
                movementHistory(movements));
    }

    private List<DistributorInventoryView> distributorInventories(
            Map<Long, Integer> currentByDistributor) {
        return currentByDistributor.entrySet().stream()
                .map(entry -> new DistributorInventoryView(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(DistributorInventoryView::distributorId))
                .toList();
    }

    private List<MovementHistoryView> movementHistory(List<InventoryMovement> movements) {
        return movements.stream()
                .map(
                        m ->
                                new MovementHistoryView(
                                        m.occurredAt(),
                                        m.movementType(),
                                        location(m.fromLocationType(), m.fromLocationId()),
                                        location(m.toLocationType(), m.toLocationId()),
                                        m.quantity()))
                .toList();
    }

    private MovementHistoryView.Location location(LocationType type, Long id) {
        return new MovementHistoryView.Location(type, id);
    }
}
