package org.omt.labelmanager.catalog.release.application;

import java.util.List;
import org.omt.labelmanager.catalog.release.TrackMapper;
import org.omt.labelmanager.catalog.release.domain.Track;
import org.omt.labelmanager.catalog.release.infrastructure.TrackArtistRepository;
import org.omt.labelmanager.catalog.release.infrastructure.TrackEntity;
import org.omt.labelmanager.catalog.release.infrastructure.TrackRemixerRepository;
import org.omt.labelmanager.catalog.release.infrastructure.TrackRepository;
import org.springframework.stereotype.Service;

@Service
class BuildTracksUseCase {

    private final TrackRepository trackRepository;
    private final TrackArtistRepository trackArtistRepository;
    private final TrackRemixerRepository trackRemixerRepository;

    BuildTracksUseCase(
            TrackRepository trackRepository,
            TrackArtistRepository trackArtistRepository,
            TrackRemixerRepository trackRemixerRepository
    ) {
        this.trackRepository = trackRepository;
        this.trackArtistRepository = trackArtistRepository;
        this.trackRemixerRepository = trackRemixerRepository;
    }

    public List<Track> buildTracksForRelease(Long releaseId) {
        List<TrackEntity> trackEntities =
                trackRepository.findByReleaseIdOrderByPosition(releaseId);

        return trackEntities.stream()
                .map(this::buildTrack)
                .toList();
    }

    private Track buildTrack(TrackEntity trackEntity) {
        List<Long> artistIds =
                trackArtistRepository.findArtistIdsByTrackId(
                        trackEntity.getId()
                );
        List<Long> remixerIds =
                trackRemixerRepository.findRemixerIdsByTrackId(
                        trackEntity.getId()
                );

        return TrackMapper.fromEntity(trackEntity, artistIds, remixerIds);
    }
}
