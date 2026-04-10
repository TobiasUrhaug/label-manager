package org.omt.labelmanager.inventory.productionrun.api;

import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/labels/{labelId}/releases/{releaseId}/production-runs")
public class ProductionRunController {

    private final ProductionRunCommandApi commandApi;

    public ProductionRunController(ProductionRunCommandApi commandApi) {
        this.commandApi = commandApi;
    }

    record AddProductionRunRequest(
            ReleaseFormat format,
            String description,
            String manufacturer,
            LocalDate manufacturingDate,
            int quantity
    ) {}

    @PostMapping
    public ResponseEntity<Void> addProductionRun(
            @PathVariable Long releaseId,
            @RequestBody AddProductionRunRequest request
    ) {
        commandApi.createProductionRun(
                releaseId,
                request.format(),
                request.description(),
                request.manufacturer(),
                request.manufacturingDate(),
                request.quantity()
        );
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{productionRunId}")
    public ResponseEntity<Void> deleteProductionRun(@PathVariable Long productionRunId) {
        commandApi.delete(productionRunId);
        return ResponseEntity.noContent().build();
    }
}
