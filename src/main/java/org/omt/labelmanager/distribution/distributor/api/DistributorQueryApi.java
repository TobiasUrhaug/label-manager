package org.omt.labelmanager.distribution.distributor.api;

import org.omt.labelmanager.distribution.distributor.domain.Distributor;

import java.util.List;

public interface DistributorQueryApi {

    List<Distributor> findByLabelId(Long labelId);
}
