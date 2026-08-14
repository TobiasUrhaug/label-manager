package org.omt.labelmanager.inventory.productionrun.application;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.omt.labelmanager.inventory.InventoryLocation;
import org.omt.labelmanager.inventory.domain.RunStock;
import org.omt.labelmanager.inventory.domain.StockLedger;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.inventorymovement.api.LocationBalance;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;
import org.omt.labelmanager.inventory.productionrun.persistence.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.persistence.ProductionRunRepository;
import org.omt.labelmanager.shared.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class ProductionRunQueryApiImpl implements ProductionRunQueryApi {

    private static final Logger log = LoggerFactory.getLogger(ProductionRunQueryApiImpl.class);

    private final ProductionRunRepository repository;
    private final InventoryMovementQueryApi inventoryMovementQueryApi;

    ProductionRunQueryApiImpl(
            ProductionRunRepository repository,
            InventoryMovementQueryApi inventoryMovementQueryApi) {
        this.repository = repository;
        this.inventoryMovementQueryApi = inventoryMovementQueryApi;
    }

    @Override
    public Optional<ProductionRun> findById(Long productionRunId) {
        return repository.findById(productionRunId).map(ProductionRun::fromEntity);
    }

    @Override
    public List<ProductionRun> findByReleaseId(Long releaseId) {
        return repository.findByReleaseId(releaseId).stream()
                .map(ProductionRun::fromEntity)
                .toList();
    }

    @Override
    public List<ProductionRun> findByReleaseIds(Collection<Long> releaseIds) {
        if (releaseIds.isEmpty()) {
            return List.of();
        }
        return repository.findByReleaseIdIn(releaseIds).stream()
                .map(ProductionRun::fromEntity)
                .toList();
    }

    @Override
    public StockLedger ledgerAt(Long releaseId, Format format, InventoryLocation location) {
        return ledgerOf(repository.findByReleaseIdAndFormat(releaseId, format), location);
    }

    // MANDATORY, not REQUIRED: a lock is only worth taking if it is held until the caller's write
    // commits. Called without a transaction, REQUIRED would open one, take the lock, and release it
    // on return — no error, no lock, and the oversell back. This way there is no such caller.
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public StockLedger lockedLedgerAt(Long releaseId, Format format, InventoryLocation location) {
        return ledgerOf(repository.lockByReleaseIdAndFormat(releaseId, format), location);
    }

    private StockLedger ledgerOf(List<ProductionRunEntity> runs, InventoryLocation location) {
        if (runs.isEmpty()) {
            return StockLedger.of(List.of());
        }

        Map<Long, Integer> onHandByRun =
                inventoryMovementQueryApi
                        .balancesFor(runs.stream().map(ProductionRunEntity::getId).toList())
                        .stream()
                        .filter(balance -> balance.isAt(location))
                        .collect(
                                Collectors.toMap(
                                        LocationBalance::productionRunId, LocationBalance::onHand));

        // A negative balance means more was sold or returned than the location ever held — a data
        // error. It is clamped rather than propagated: a ledger cannot hold a negative quantity,
        // and letting one run's bad data throw would block every sale of the release. Logged
        // because a clamped balance is otherwise invisible — the stock simply reads as zero.
        return StockLedger.of(
                runs.stream()
                        .map(
                                run ->
                                        new RunStock(
                                                run.getId(),
                                                run.getManufacturingDate(),
                                                onHandOf(run, onHandByRun, location)))
                        .toList());
    }

    private int onHandOf(
            ProductionRunEntity run, Map<Long, Integer> onHandByRun, InventoryLocation location) {
        int onHand = onHandByRun.getOrDefault(run.getId(), 0);
        if (onHand < 0) {
            log.warn(
                    "Production run {} has a negative balance of {} at {} — more has been recorded"
                            + " leaving than ever arrived. Treating it as zero.",
                    run.getId(),
                    onHand,
                    location);
            return 0;
        }
        return onHand;
    }
}
