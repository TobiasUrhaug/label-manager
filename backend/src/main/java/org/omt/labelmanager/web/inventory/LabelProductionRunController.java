package org.omt.labelmanager.web.inventory;

import java.util.List;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.catalog.release.domain.Release;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;
import org.omt.labelmanager.web.LabelScope;
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
    private final LabelScope labelScope;

    public LabelProductionRunController(
            ReleaseQueryApi releaseQueryApi,
            ProductionRunQueryApi productionRunQueryApi,
            LabelScope labelScope) {
        this.releaseQueryApi = releaseQueryApi;
        this.productionRunQueryApi = productionRunQueryApi;
        this.labelScope = labelScope;
    }

    @GetMapping("/api/labels/{labelId}/production-runs")
    public List<ProductionRun> productionRuns(@PathVariable Long labelId) {
        labelScope.requireLabel(labelId);
        return releaseQueryApi.getReleasesForLabel(labelId).stream()
                .map(Release::id)
                .flatMap(releaseId -> productionRunQueryApi.findByReleaseId(releaseId).stream())
                .toList();
    }
}
