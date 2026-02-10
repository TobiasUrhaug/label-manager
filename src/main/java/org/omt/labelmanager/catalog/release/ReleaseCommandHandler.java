package org.omt.labelmanager.catalog.release;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.omt.labelmanager.catalog.label.api.LabelQueryFacade;
import org.omt.labelmanager.catalog.release.api.ReleaseCommandFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReleaseCommandHandler implements ReleaseCommandFacade {

    private static final Logger log =
            LoggerFactory.getLogger(ReleaseCommandHandler.class);

    private final ReleaseRepository releaseRepository;
    private final LabelQueryFacade labelQueryFacade;
    private final TrackRepository trackRepository;
    private final ReleaseArtistRepository releaseArtistRepository;
    private final TrackArtistRepository trackArtistRepository;

    ReleaseCommandHandler(
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
    public void createRelease(
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

    @Transactional
    public void updateRelease(
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

    @Transactional
    public void delete(Long id) {
        log.info("Deleting release with id {}", id);
        releaseRepository.deleteById(id);
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
