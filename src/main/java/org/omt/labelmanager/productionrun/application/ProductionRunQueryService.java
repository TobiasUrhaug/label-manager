package org.omt.labelmanager.productionrun.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.omt.labelmanager.catalog.domain.release.ReleaseFormat;
import org.omt.labelmanager.productionrun.domain.ProductionRun;
import org.omt.labelmanager.productionrun.infrastructure.persistence.ProductionRunRepository;
import org.springframework.stereotype.Service;

@Service
public class ProductionRunQueryService {

    private final ProductionRunRepository productionRunRepository;

    public ProductionRunQueryService(ProductionRunRepository productionRunRepository) {
        this.productionRunRepository = productionRunRepository;
    }

    public List<ProductionRun> getProductionRunsForRelease(Long releaseId) {
        return productionRunRepository.findByReleaseId(releaseId).stream()
                .map(ProductionRun::fromEntity)
                .toList();
    }

    public Map<ReleaseFormat, Integer> getTotalsForRelease(Long releaseId) {
        return productionRunRepository.findByReleaseId(releaseId).stream()
                .map(ProductionRun::fromEntity)
                .collect(Collectors.groupingBy(
                        ProductionRun::format,
                        Collectors.summingInt(ProductionRun::quantity)
                ));
    }
}
