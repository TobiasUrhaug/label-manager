package org.omt.labelmanager.inventory.productionrun.api;

import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.inventory.InventoryLocation;
import org.omt.labelmanager.inventory.LocationType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/labels/{labelId}/releases/{releaseId}/production-runs/{runId}")
public class AllocateController {

    private final ProductionRunCommandApi productionRunCommandApi;

    public AllocateController(ProductionRunCommandApi productionRunCommandApi) {
        this.productionRunCommandApi = productionRunCommandApi;
    }

    record AllocateRequest(LocationType locationType, Long distributorId, int quantity) {}

    record CancelBandcampReservationRequest(int quantity) {}

    @PostMapping("/allocations")
    public ResponseEntity<Void> allocate(
            @PathVariable Long runId,
            @RequestBody AllocateRequest request
    ) {
        if (request.quantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than zero");
        }
        if (request.locationType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A location type must be selected");
        }
        if (request.locationType() == LocationType.DISTRIBUTOR && request.distributorId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A distributor must be selected");
        }
        productionRunCommandApi.allocate(runId, resolveToLocation(request), request.quantity());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bandcamp-cancellations")
    public ResponseEntity<Void> cancelBandcampReservation(
            @PathVariable Long runId,
            @RequestBody CancelBandcampReservationRequest request
    ) {
        if (request.quantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than zero");
        }
        productionRunCommandApi.cancelBandcampReservation(runId, request.quantity());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(InsufficientInventoryException.class)
    public ResponseEntity<Void> handleInsufficientInventory() {
        return ResponseEntity.badRequest().build();
    }

    private InventoryLocation resolveToLocation(AllocateRequest request) {
        if (request.locationType() == LocationType.BANDCAMP) {
            return InventoryLocation.bandcamp();
        }
        return InventoryLocation.distributor(request.distributorId());
    }
}
