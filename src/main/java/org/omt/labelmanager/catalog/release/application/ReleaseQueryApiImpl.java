package org.omt.labelmanager.catalog.release.application;

import java.util.List;
import java.util.Optional;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.release.ReleaseMapper;
import org.omt.labelmanager.catalog.release.TrackMapper;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.catalog.release.domain.Release;
import org.omt.labelmanager.catalog.release.domain.Track;
import org.omt.labelmanager.catalog.release.infrastructure.ReleaseArtistRepository;
import org.omt.labelmanager.catalog.release.infrastructure.ReleaseEntity;
import org.omt.labelmanager.catalog.release.infrastructure.ReleaseRepository;
import org.omt.labelmanager.catalog.release.infrastructure.TrackArtistRepository;
import org.omt.labelmanager.catalog.release.infrastructure.TrackEntity;
import org.omt.labelmanager.catalog.release.infrastructure.TrackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class ReleaseQueryApiImpl implements ReleaseQueryApi {

    private static final Logger log =
            LoggerFactory.getLogger(ReleaseQueryApiImpl.class);

    private final ReleaseRepository releaseRepository;
    private final LabelQueryApi labelQueryFacade;
    private final TrackRepository trackRepository;
    private final ReleaseArtistRepository releaseArtistRepository;
    private final TrackArtistRepository trackArtistRepository;

    ReleaseQueryApiImpl(
            ReleaseRepository releaseRepository,
            LabelQueryApi labelQueryFacade,
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

    public Optional<Release> findById(Long id) {
        Optional<ReleaseEntity> releaseEntity =
                releaseRepository.findById(id);
        if (releaseEntity.isEmpty()) {
            log.debug("Release with id {} not found", id);
            return Optional.empty();
        }

        return Optional.of(buildRelease(releaseEntity.get()));
    }

    public List<Release> getReleasesForLabel(Long labelId) {
        if (!labelQueryFacade.exists(labelId)) {
            throw new IllegalArgumentException("Label not found");
        }

        List<ReleaseEntity> releaseEntities =
                releaseRepository.findByLabelId(labelId);
        List<Release> releases = releaseEntities.stream()
                .map(this::buildRelease)
                .toList();

        log.debug(
                "Retrieved {} releases for label {}",
                releases.size(),
                labelId
        );
        return releases;
    }

    public boolean exists(Long id) {
        return releaseRepository.existsById(id);
    }

    private Release buildRelease(ReleaseEntity releaseEntity) {
        List<Long> artistIds =
                releaseArtistRepository.findArtistIdsByReleaseId(
                        releaseEntity.getId()
                );

        List<TrackEntity> trackEntities =
                trackRepository.findByReleaseIdOrderByPosition(
                        releaseEntity.getId()
                );
        List<Track> tracks = trackEntities.stream()
                .map(this::buildTrack)
                .toList();

        return ReleaseMapper.fromEntity(
                releaseEntity, artistIds, tracks
        );
    }

    private Track buildTrack(TrackEntity trackEntity) {
        List<Long> artistIds =
                trackArtistRepository.findArtistIdsByTrackId(
                        trackEntity.getId()
                );
        List<Long> remixerIds = List.of();

        return TrackMapper.fromEntity(trackEntity, artistIds, remixerIds);
    }
}
