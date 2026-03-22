package org.omt.labelmanager.catalog.release.application;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.catalog.release.domain.TrackInput;
import org.omt.labelmanager.catalog.release.infrastructure.ReleaseArtistRepository;
import org.omt.labelmanager.catalog.release.infrastructure.ReleaseEntity;
import org.omt.labelmanager.catalog.release.infrastructure.ReleaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UpdateReleaseUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(UpdateReleaseUseCase.class);

    private final ReleaseRepository releaseRepository;
    private final ReleaseArtistRepository releaseArtistRepository;
    private final CreateTracksUseCase createTracks;
    private final DeleteTracksUseCase deleteTracks;

    UpdateReleaseUseCase(
            ReleaseRepository releaseRepository,
            ReleaseArtistRepository releaseArtistRepository,
            CreateTracksUseCase createTracks,
            DeleteTracksUseCase deleteTracks
    ) {
        this.releaseRepository = releaseRepository;
        this.releaseArtistRepository = releaseArtistRepository;
        this.createTracks = createTracks;
        this.deleteTracks = deleteTracks;
    }

    @Transactional
    public void execute(
            Long id,
            String name,
            LocalDate releaseDate,
            List<Long> artistIds,
            List<TrackInput> tracks,
            Set<ReleaseFormat> formats
    ) {
        log.info(
                "Updating release {} with {} tracks",
                id,
                tracks.size()
        );
        requireAtLeastOneTrack(tracks, id.toString());

        ReleaseEntity release = releaseRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn(
                            "Cannot update release: release {} not found",
                            id
                    );
                    return new IllegalArgumentException();
                });

        release.setName(name);
        release.setReleaseDate(releaseDate);
        release.setFormats(new HashSet<>(formats));
        releaseRepository.save(release);

        releaseArtistRepository.deleteAllByReleaseId(id);
        for (Long artistId : artistIds) {
            releaseArtistRepository.addArtistToRelease(
                    id, artistId
            );
        }

        deleteTracks.deleteTracksForRelease(id);
        createTracks.createTracksForRelease(tracks, id);
    }

    private void requireAtLeastOneTrack(
            List<TrackInput> tracks,
            String releaseIdentifier
    ) {
        if (tracks.isEmpty()) {
            log.warn(
                    "Release '{}' requires at least one track",
                    releaseIdentifier
            );
            throw new IllegalArgumentException(
                    "At least one track is required"
            );
        }
    }
}
