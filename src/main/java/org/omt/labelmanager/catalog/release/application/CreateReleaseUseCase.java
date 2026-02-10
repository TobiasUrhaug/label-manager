package org.omt.labelmanager.catalog.release.application;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.omt.labelmanager.catalog.label.api.LabelQueryFacade;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.catalog.release.domain.TrackInput;
import org.omt.labelmanager.catalog.release.infrastructure.ReleaseArtistRepository;
import org.omt.labelmanager.catalog.release.infrastructure.ReleaseEntity;
import org.omt.labelmanager.catalog.release.infrastructure.ReleaseRepository;
import org.omt.labelmanager.catalog.release.infrastructure.TrackArtistRepository;
import org.omt.labelmanager.catalog.release.infrastructure.TrackEntity;
import org.omt.labelmanager.catalog.release.infrastructure.TrackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CreateReleaseUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(CreateReleaseUseCase.class);

    private final ReleaseRepository releaseRepository;
    private final LabelQueryFacade labelQueryFacade;
    private final TrackRepository trackRepository;
    private final ReleaseArtistRepository releaseArtistRepository;
    private final TrackArtistRepository trackArtistRepository;

    CreateReleaseUseCase(
            ReleaseRepository releaseRepository,
            LabelQueryFacade labelQueryFacade,
            TrackRepository trackRepository,
            ReleaseArtistRepository releaseArtistRepository,
            TrackArtistRepository trackArtistRepository
    ) {
        this.releaseRepository = releaseRepository;
        this.labelQueryFacade = labelQueryFacade;
        this.trackRepository = trackRepository;
        this.releaseArtistRepository = releaseArtistRepository;
        this.trackArtistRepository = trackArtistRepository;
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

        createTracksForRelease(tracks, release.getId());
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

    private void createTracksForRelease(
            List<TrackInput> tracks,
            Long releaseId
    ) {
        for (TrackInput trackInput : tracks) {
            TrackEntity trackEntity = new TrackEntity();
            trackEntity.setName(trackInput.name());
            trackEntity.setDurationSeconds(
                    trackInput.duration().totalSeconds()
            );
            trackEntity.setPosition(trackInput.position());
            trackEntity.setReleaseId(releaseId);
            trackRepository.save(trackEntity);

            for (Long artistId : trackInput.artistIds()) {
                trackArtistRepository.addArtistToTrack(
                        trackEntity.getId(), artistId
                );
            }
        }
    }
}
