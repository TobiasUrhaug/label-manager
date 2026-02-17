package org.omt.labelmanager.sales.distributor_return.api;

import java.util.List;
import java.util.Optional;
import org.omt.labelmanager.sales.distributor_return.domain.DistributorReturn;

/**
 * Public query interface for reading distributor return data.
 */
public interface DistributorReturnQueryApi {

    /**
     * Returns all returns for a label, ordered by return date descending.
     */
    List<DistributorReturn> getReturnsForLabel(Long labelId);

    /**
     * Returns all returns attributed to a distributor, ordered by return date descending.
     */
    List<DistributorReturn> getReturnsForDistributor(Long distributorId);

    /**
     * Finds a single return by ID.
     */
    Optional<DistributorReturn> findById(Long returnId);
}
