package org.omt.labelmanager.web;

import jakarta.persistence.EntityNotFoundException;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.distribution.distributor.api.Distributor;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.springframework.stereotype.Component;

/**
 * Checks that a nested resource is actually reachable under the label in the path.
 *
 * <p>Splitting the page models into sibling collections made this necessary: the bundled responses
 * resolved the parent once and read everything else off it, so a release or distributor from
 * another label could not be named. A collection endpoint takes those ids straight from the
 * request, so each one has to check for itself — and every one of them must, or the split quietly
 * widens what the API returns.
 *
 * <p>This is a path-to-resource consistency check, not the tenant guard. It establishes that a
 * release belongs to the label named in the path, never that the caller owns the label. That is F2,
 * and it belongs in one place across every endpoint rather than scattered here.
 */
@Component
public class LabelScope {

    private final LabelQueryApi labelQueryApi;
    private final ReleaseQueryApi releaseQueryApi;
    private final DistributorQueryApi distributorQueryApi;

    public LabelScope(
            LabelQueryApi labelQueryApi,
            ReleaseQueryApi releaseQueryApi,
            DistributorQueryApi distributorQueryApi) {
        this.labelQueryApi = labelQueryApi;
        this.releaseQueryApi = releaseQueryApi;
        this.distributorQueryApi = distributorQueryApi;
    }

    /** Throws unless a label with this id exists. */
    public void requireLabel(Long labelId) {
        if (labelQueryApi.findById(labelId).isEmpty()) {
            throw new EntityNotFoundException("Label not found: " + labelId);
        }
    }

    /** Reports whether the release exists and belongs to this label. */
    public boolean isReleaseOfLabel(Long labelId, Long releaseId) {
        return releaseQueryApi
                .findById(releaseId)
                .map(release -> labelId.equals(release.labelId()))
                .orElse(false);
    }

    /** Throws unless the release exists and belongs to this label. */
    public void requireRelease(Long labelId, Long releaseId) {
        if (!isReleaseOfLabel(labelId, releaseId)) {
            throw new EntityNotFoundException(
                    "Release " + releaseId + " does not belong to label " + labelId);
        }
    }

    /** Returns the distributor, or throws unless it exists and belongs to this label. */
    public Distributor requireDistributor(Long labelId, Long distributorId) {
        return distributorQueryApi
                .findById(distributorId)
                .filter(distributor -> labelId.equals(distributor.labelId()))
                .orElseThrow(
                        () ->
                                new EntityNotFoundException(
                                        "Distributor "
                                                + distributorId
                                                + " does not belong to label "
                                                + labelId));
    }
}
