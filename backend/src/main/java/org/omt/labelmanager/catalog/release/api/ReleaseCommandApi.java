package org.omt.labelmanager.catalog.release.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.catalog.release.domain.TrackInput;

public interface ReleaseCommandApi {

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
