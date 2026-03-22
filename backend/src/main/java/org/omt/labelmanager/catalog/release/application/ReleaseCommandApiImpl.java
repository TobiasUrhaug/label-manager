package org.omt.labelmanager.catalog.release.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.omt.labelmanager.catalog.release.api.ReleaseCommandApi;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.catalog.release.domain.TrackInput;
import org.springframework.stereotype.Service;

@Service
class ReleaseCommandApiImpl implements ReleaseCommandApi {

    private final CreateReleaseUseCase createReleaseUseCase;
    private final UpdateReleaseUseCase updateReleaseUseCase;
    private final DeleteReleaseUseCase deleteReleaseUseCase;

    ReleaseCommandApiImpl(
            CreateReleaseUseCase createReleaseUseCase,
            UpdateReleaseUseCase updateReleaseUseCase,
            DeleteReleaseUseCase deleteReleaseUseCase
    ) {
        this.createReleaseUseCase = createReleaseUseCase;
        this.updateReleaseUseCase = updateReleaseUseCase;
        this.deleteReleaseUseCase = deleteReleaseUseCase;
    }

    @Override
    public void createRelease(
            String name,
            LocalDate releaseDate,
            Long labelId,
            List<Long> artistIds,
            List<TrackInput> tracks,
            Set<ReleaseFormat> formats
    ) {
        createReleaseUseCase.execute(
                name, releaseDate, labelId, artistIds, tracks, formats
        );
    }

    @Override
    public void updateRelease(
            Long id,
            String name,
            LocalDate releaseDate,
            List<Long> artistIds,
            List<TrackInput> tracks,
            Set<ReleaseFormat> formats
    ) {
        updateReleaseUseCase.execute(
                id, name, releaseDate, artistIds, tracks, formats
        );
    }

    @Override
    public void delete(Long id) {
        deleteReleaseUseCase.execute(id);
    }
}
