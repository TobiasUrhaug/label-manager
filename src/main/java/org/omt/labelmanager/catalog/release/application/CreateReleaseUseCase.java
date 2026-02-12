package org.omt.labelmanager.catalog.release.application;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
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
class CreateReleaseUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(CreateReleaseUseCase.class);

    private final ReleaseRepository releaseRepository;
    private final LabelQueryApi labelQueryFacade;
    private final ReleaseArtistRepository releaseArtistRepository;
    private final CreateTracksUseCase createTracks;

    CreateReleaseUseCase(
            ReleaseRepository releaseRepository,
            LabelQueryApi labelQueryFacade,
            ReleaseArtistRepository releaseArtistRepository,
            CreateTracksUseCase createTracks
    ) {
        this.releaseRepository = releaseRepository;
        this.labelQueryFacade = labelQueryFacade;
        this.releaseArtistRepository = releaseArtistRepository;
        this.createTracks = createTracks;
    }

    @Transactional
    public void execute(
            String name,
            LocalDate releaseDate,
            Long labelId,
            List<Long> artistIds,
            List<TrackInput> tracks,
            Set<ReleaseFormat> formats
    ) {
        log.info(
                "Creating release '{}' for label {} with {} tracks",
                name,
                labelId,
                tracks.size()
        );
        requireAtLeastOneTrack(tracks, name);

        if (!labelQueryFacade.exists(labelId)) {
            log.warn(
                    "Cannot create release: label {} not found",
                    labelId
            );
            throw new IllegalArgumentException("Label not found");
        }

        ReleaseEntity release = createReleaseEntity(
                name, releaseDate, formats, labelId
        );
        releaseRepository.save(release);

        log.debug(
                "Found {} artists for release", artistIds.size()
        );
        for (Long artistId : artistIds) {
            releaseArtistRepository.addArtistToRelease(
                    release.getId(), artistId
            );
        }

        createTracks.createTracksForRelease(tracks, release.getId());
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

    private static ReleaseEntity createReleaseEntity(
            String name,
            LocalDate releaseDate,
            Set<ReleaseFormat> formats,
            Long labelId
    ) {
        ReleaseEntity release = new ReleaseEntity();
        release.setName(name);
        release.setReleaseDate(releaseDate);
        release.setLabelId(labelId);
        release.setFormats(new HashSet<>(formats));
        return release;
    }
}
