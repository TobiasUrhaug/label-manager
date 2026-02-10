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
import org.omt.labelmanager.catalog.release.infrastructure.TrackArtistRepository;
import org.omt.labelmanager.catalog.release.infrastructure.TrackEntity;
import org.omt.labelmanager.catalog.release.infrastructure.TrackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UpdateReleaseUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(UpdateReleaseUseCase.class);

    private final ReleaseRepository releaseRepository;
    private final TrackRepository trackRepository;
    private final ReleaseArtistRepository releaseArtistRepository;
    private final TrackArtistRepository trackArtistRepository;

    UpdateReleaseUseCase(
            ReleaseRepository releaseRepository,
            TrackRepository trackRepository,
            ReleaseArtistRepository releaseArtistRepository,
            TrackArtistRepository trackArtistRepository
    ) {
        this.releaseRepository = releaseRepository;
        this.trackRepository = trackRepository;
        this.releaseArtistRepository = releaseArtistRepository;
        this.trackArtistRepository = trackArtistRepository;
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

        List<TrackEntity> existingTracks =
                trackRepository.findByReleaseIdOrderByPosition(id);
        for (TrackEntity trackEntity : existingTracks) {
            trackArtistRepository.deleteAllByTrackId(
                    trackEntity.getId()
            );
        }
        trackRepository.deleteAll(existingTracks);

        createTracksForRelease(tracks, id);
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
