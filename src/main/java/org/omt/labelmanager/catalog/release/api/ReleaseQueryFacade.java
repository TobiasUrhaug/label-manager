package org.omt.labelmanager.catalog.release.api;

import java.util.List;
import java.util.Optional;
import org.omt.labelmanager.catalog.release.Release;

public interface ReleaseQueryFacade {

    Optional<Release> findById(Long id);

    List<Release> getReleasesForLabel(Long labelId);

    boolean exists(Long id);

}
