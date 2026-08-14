package org.omt.labelmanager.inventory.productionrun.web;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.catalog.release.domain.Release;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Every production run under a label, across all its releases.
 *
 * <p>Exists so a caller holding a {@code productionRunId} — an agreement, say — can resolve it
 * without the endpoint that returned that id reaching into inventory to render a name for it.
 */
@RestController
public class LabelProductionRunController {

    private final ReleaseQueryApi releaseQueryApi;
    private final ProductionRunQueryApi productionRunQueryApi;
    private final LabelQueryApi labelQueryApi;

    public LabelProductionRunController(
            ReleaseQueryApi releaseQueryApi,
            ProductionRunQueryApi productionRunQueryApi,
            LabelQueryApi labelQueryApi) {
        this.releaseQueryApi = releaseQueryApi;
        this.productionRunQueryApi = productionRunQueryApi;
        this.labelQueryApi = labelQueryApi;
    }

    private void requireLabel(Long labelId) {
        if (!labelQueryApi.exists(labelId)) {
            throw new EntityNotFoundException("Label not found: " + labelId);
        }
    }

    @GetMapping("/api/labels/{labelId}/production-runs")
    public List<ProductionRun> productionRuns(@PathVariable Long labelId) {
        requireLabel(labelId);
        List<Long> releaseIds =
                releaseQueryApi.getReleasesForLabel(labelId).stream().map(Release::id).toList();
        return productionRunQueryApi.findByReleaseIds(releaseIds);
    }
}
