package org.omt.labelmanager.catalog.application;

import org.omt.labelmanager.catalog.domain.artist.Artist;
import org.omt.labelmanager.catalog.domain.release.Release;
import org.omt.labelmanager.catalog.domain.release.ReleaseFormat;
import org.omt.labelmanager.catalog.domain.track.Track;
import org.omt.labelmanager.catalog.domain.track.TrackDuration;
import org.omt.labelmanager.catalog.domain.track.TrackInput;
import org.omt.labelmanager.catalog.infrastructure.persistence.artist.ArtistEntity;
import org.omt.labelmanager.catalog.infrastructure.persistence.artist.ArtistRepository;
import org.omt.labelmanager.catalog.infrastructure.persistence.release.ReleaseArtistRepository;
import org.omt.labelmanager.catalog.infrastructure.persistence.release.ReleaseEntity;
import org.omt.labelmanager.catalog.infrastructure.persistence.release.ReleaseRepository;
import org.omt.labelmanager.catalog.infrastructure.persistence.track.TrackArtistRepository;
import org.omt.labelmanager.catalog.infrastructure.persistence.track.TrackEntity;
import org.omt.labelmanager.catalog.infrastructure.persistence.track.TrackRepository;
import org.omt.labelmanager.catalog.label.Label;
import org.omt.labelmanager.catalog.label.LabelQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ReleaseCRUDHandler {

    private static final Logger log =
            LoggerFactory.getLogger(ReleaseCRUDHandler.class);

    private final ReleaseRepository releaseRepository;
    private final LabelQueryService labelQueryService;
    private final ArtistRepository artistRepository;
    private final TrackRepository trackRepository;
    private final ReleaseArtistRepository releaseArtistRepository;
    private final TrackArtistRepository trackArtistRepository;

    public ReleaseCRUDHandler(
            ReleaseRepository releaseRepository,
            LabelQueryService labelQueryService,
            ArtistRepository artistRepository,
            TrackRepository trackRepository,
            ReleaseArtistRepository releaseArtistRepository,
            TrackArtistRepository trackArtistRepository
    ) {
        this.releaseRepository = releaseRepository;
        this.labelQueryService = labelQueryService;
        this.artistRepository = artistRepository;
        this.trackRepository = trackRepository;
        this.releaseArtistRepository = releaseArtistRepository;
        this.trackArtistRepository = trackArtistRepository;
    }

    public List<Release> getReleasesForLabel(Long labelId) {
        Label label = labelQueryService.findById(labelId)
                .orElseThrow(() -> new IllegalArgumentException("Label not found"));

        List<ReleaseEntity> releaseEntities = releaseRepository.findByLabelId(labelId);
        List<Release> releases = releaseEntities.stream()
                .map(releaseEntity -> buildRelease(releaseEntity, label))
                .toList();

        log.debug("Retrieved {} releases for label {}", releases.size(), labelId);
        return releases;
    }

    private Release buildRelease(ReleaseEntity releaseEntity, Label label) {
        List<ArtistEntity> artistEntities =
                releaseArtistRepository.findArtistsByReleaseId(releaseEntity.getId());
        List<Artist> artists = artistEntities.stream()
                .map(Artist::fromEntity)
                .toList();

        List<TrackEntity> trackEntities =
                trackRepository.findByReleaseIdOrderByPosition(releaseEntity.getId());
        List<Track> tracks = trackEntities.stream()
                .map(this::buildTrack)
                .toList();

        return new Release(
                releaseEntity.getId(),
                releaseEntity.getName(),
                releaseEntity.getReleaseDate(),
                label,
                artists,
                tracks,
                releaseEntity.getFormats()
        );
    }

    private Track buildTrack(TrackEntity trackEntity) {
        List<ArtistEntity> artistEntities =
                trackArtistRepository.findArtistsByTrackId(trackEntity.getId());
        List<Artist> artists = artistEntities.stream()
                .map(Artist::fromEntity)
                .toList();

        return new Track(
                trackEntity.getId(),
                artists,
                trackEntity.getName(),
                TrackDuration.ofSeconds(trackEntity.getDurationSeconds()),
                trackEntity.getPosition()
        );
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

        if (!labelQueryService.exists(labelId)) {
            log.warn("Cannot create release: label {} not found", labelId);
            throw new IllegalArgumentException("Label not found");
        }

        ReleaseEntity release = createReleaseEntity(name, releaseDate, formats, labelId);
        releaseRepository.save(release);

        log.debug("Found {} artists for release", artistIds.size());
        for (Long artistId : artistIds) {
            releaseArtistRepository.addArtistToRelease(release.getId(), artistId);
        }

        createTracksForRelease(tracks, release.getId());
    }

    private void requireAtLeastOneTrack(
            List<TrackInput> tracks,
            String releaseIdentifier
    ) {
        if (tracks.isEmpty()) {
            log.warn("Release '{}' requires at least one track", releaseIdentifier);
            throw new IllegalArgumentException("At least one track is required");
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
        release.setFormats(new java.util.HashSet<>(formats));
        return release;
    }

    private void createTracksForRelease(List<TrackInput> tracks, Long releaseId) {
        for (TrackInput trackInput : tracks) {
            TrackEntity trackEntity = new TrackEntity();
            trackEntity.setName(trackInput.name());
            trackEntity.setDurationSeconds(trackInput.duration().totalSeconds());
            trackEntity.setPosition(trackInput.position());
            trackEntity.setReleaseId(releaseId);
            trackRepository.save(trackEntity);

            for (Long artistId : trackInput.artistIds()) {
                trackArtistRepository.addArtistToTrack(trackEntity.getId(), artistId);
            }
        }
    }

    public Optional<Release> findById(long id) {
        Optional<ReleaseEntity> releaseEntity = releaseRepository.findById(id);
        if (releaseEntity.isEmpty()) {
            log.debug("Release with id {} not found", id);
            return Optional.empty();
        }

        ReleaseEntity entity = releaseEntity.get();
        Label label = labelQueryService.findById(entity.getLabelId())
                .orElseThrow(() -> new IllegalStateException("Label not found"));

        return Optional.of(buildRelease(entity, label));
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
        log.info("Updating release {} with {} tracks", id, tracks.size());
        requireAtLeastOneTrack(tracks, id.toString());

        ReleaseEntity release = releaseRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cannot update release: release {} not found", id);
                    return new IllegalArgumentException();
                });

        release.setName(name);
        release.setReleaseDate(releaseDate);
        release.setFormats(new java.util.HashSet<>(formats));
        releaseRepository.save(release);

        releaseArtistRepository.deleteAllByReleaseId(id);
        for (Long artistId : artistIds) {
            releaseArtistRepository.addArtistToRelease(id, artistId);
        }

        List<TrackEntity> existingTracks =
                trackRepository.findByReleaseIdOrderByPosition(id);
        for (TrackEntity trackEntity : existingTracks) {
            trackArtistRepository.deleteAllByTrackId(trackEntity.getId());
        }
        trackRepository.deleteAll(existingTracks);

        createTracksForRelease(tracks, id);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting release with id {}", id);
        releaseRepository.deleteById(id);
    }
}
