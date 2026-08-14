package org.omt.labelmanager.catalog.release.api;

import java.util.List;
import java.util.Optional;
import org.omt.labelmanager.catalog.release.domain.Release;

public interface ReleaseQueryApi {

    Optional<Release> findById(Long id);

    List<Release> getReleasesForLabel(Long labelId);

    boolean exists(Long id);

    /**
     * Reports whether the release exists and belongs to this label.
     *
     * <p>Lives here because catalog owns the answer. Callers used to fetch the release and compare
     * labelIds themselves, which meant every caller had to remember to.
     *
     * @param releaseId the release id
     * @param labelId the label the caller believes owns it
     * @return true only if the release exists and its labelId matches
     */
    boolean belongsToLabel(Long releaseId, Long labelId);
}
