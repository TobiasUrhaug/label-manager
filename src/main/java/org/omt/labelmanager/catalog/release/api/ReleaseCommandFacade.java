package org.omt.labelmanager.catalog.release.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.omt.labelmanager.catalog.release.ReleaseFormat;
import org.omt.labelmanager.catalog.release.TrackInput;

public interface ReleaseCommandFacade {

    void createRelease(
            String name,
            LocalDate releaseDate,
            Long labelId,
            List<Long> artistIds,
            List<TrackInput> tracks,
            Set<ReleaseFormat> formats
    );

    void updateRelease(
            Long id,
            String name,
            LocalDate releaseDate,
            List<Long> artistIds,
            List<TrackInput> tracks,
            Set<ReleaseFormat> formats
    );

    void delete(Long id);

}
