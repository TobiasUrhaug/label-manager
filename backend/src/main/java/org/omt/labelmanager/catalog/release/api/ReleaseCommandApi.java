package org.omt.labelmanager.catalog.release.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.omt.labelmanager.catalog.release.domain.TrackInput;
import org.omt.labelmanager.shared.Format;

public interface ReleaseCommandApi {

    void createRelease(
            String name,
            LocalDate releaseDate,
            Long labelId,
            List<Long> artistIds,
            List<TrackInput> tracks,
            Set<Format> formats);

    void updateRelease(
            Long id,
            String name,
            LocalDate releaseDate,
            List<Long> artistIds,
            List<TrackInput> tracks,
            Set<Format> formats);

    void delete(Long id);
}
