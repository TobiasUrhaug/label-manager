package org.omt.labelmanager.inventory.productionrun.api;

import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;

import java.util.List;

public interface ProductionRunQueryApi {

    List<ProductionRun> findByReleaseId(Long releaseId);
}
