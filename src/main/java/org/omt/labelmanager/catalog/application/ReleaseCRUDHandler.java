package org.omt.labelmanager.catalog.application;

import org.omt.labelmanager.catalog.infrastructure.persistence.artist.ArtistEntity;
import org.omt.labelmanager.catalog.infrastructure.persistence.artist.ArtistRepository;
import org.omt.labelmanager.catalog.infrastructure.persistence.label.LabelEntity;
import org.omt.labelmanager.catalog.infrastructure.persistence.label.LabelRepository;
import org.omt.labelmanager.catalog.domain.release.Release;
import org.omt.labelmanager.catalog.domain.release.ReleaseFormat;
import org.omt.labelmanager.catalog.infrastructure.persistence.release.ReleaseEntity;
import org.omt.labelmanager.catalog.infrastructure.persistence.release.ReleaseRepository;
import org.omt.labelmanager.catalog.domain.track.TrackInput;
import org.omt.labelmanager.catalog.infrastructure.persistence.track.TrackEntity;
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

    private static final Logger log = LoggerFactory.getLogger(ReleaseCRUDHandler.class);

    private final ReleaseRepository releaseRepository;
    private final LabelRepository labelRepository;
    private final ArtistRepository artistRepository;

    public ReleaseCRUDHandler(
            ReleaseRepository releaseRepository,
            LabelRepository labelRepository,
            ArtistRepository artistRepository
    ) {
        this.releaseRepository = releaseRepository;
        this.labelRepository = labelRepository;
        this.artistRepository = artistRepository;
    }

    public List<Release> getReleasesForLabel(Long labelId) {
        List<Release> releases =
                releaseRepository.findByLabelId(labelId).stream().map(Release::fromEntity).toList();
        log.debug("Retrieved {} releases for label {}", releases.size(), labelId);
        return releases;
    }

    public void createRelease(
            String name,
            LocalDate releaseDate,
            Long labelId,
            List<Long> artistIds,
            List<TrackInput> tracks,
            Set<ReleaseFormat> formats
    ) {
        log.info("Creating release '{}' for label {} with {} tracks", name, labelId, tracks.size());
        requireAtLeastOneTrack(tracks, name);
        LabelEntity labelEntity = labelRepository.findById(labelId)
                .orElseThrow(() -> {
                    log.warn("Cannot create release: label {} not found", labelId);
                    return new IllegalArgumentException();
                });

        List<ArtistEntity> releaseArtists = artistRepository.findAllById(artistIds);
        log.debug("Found {} artists for release", releaseArtists.size());

        ReleaseEntity release = createReleaseEntity(name, releaseDate, formats, labelEntity, releaseArtists);
        addTracksToRelease(tracks, release);
        releaseRepository.save(release);
    }

    private void requireAtLeastOneTrack(List<TrackInput> tracks, String releaseIdentifier) {
        if (tracks.isEmpty()) {
            log.warn("Release '{}' requires at least one track", releaseIdentifier);
            throw new IllegalArgumentException("At least one track is required");
        }
    }

    private static ReleaseEntity createReleaseEntity(String name, LocalDate releaseDate, Set<ReleaseFormat> formats, LabelEntity labelEntity, List<ArtistEntity> releaseArtists) {
        ReleaseEntity release = new ReleaseEntity();
        release.setName(name);
        release.setReleaseDate(releaseDate);
        release.setLabel(labelEntity);
        release.setArtists(releaseArtists);
        release.setFormats(formats);
        return release;
    }

    private void addTracksToRelease(List<TrackInput> tracks, ReleaseEntity release) {
        tracks.stream()
                .map(trackInput -> createTrackEntity(trackInput, release))
                .forEach(release.getTracks()::add);
    }

    private TrackEntity createTrackEntity(TrackInput trackInput, ReleaseEntity release) {
        List<ArtistEntity> trackArtists = artistRepository.findAllById(trackInput.artistIds());
        TrackEntity trackEntity = new TrackEntity();
        trackEntity.setArtists(trackArtists);
        trackEntity.setName(trackInput.name());
        trackEntity.setDurationSeconds(trackInput.duration().totalSeconds());
        trackEntity.setPosition(trackInput.position());
        trackEntity.setRelease(release);
        return trackEntity;
    }

    public Optional<Release> findById(long id) {
        Optional<Release> release = releaseRepository.findById(id).map(Release::fromEntity);
        if (release.isEmpty()) {
            log.debug("Release with id {} not found", id);
        }
        return release;
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
        release.setFormats(formats);

        List<ArtistEntity> releaseArtists = artistRepository.findAllById(artistIds);
        release.setArtists(releaseArtists);

        release.getTracks().clear();
        addTracksToRelease(tracks, release);

        releaseRepository.save(release);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting release with id {}", id);
        releaseRepository.deleteById(id);
    }
}
