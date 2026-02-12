package org.omt.labelmanager.catalog.release.application;

import java.util.List;
import org.omt.labelmanager.catalog.release.infrastructure.TrackArtistRepository;
import org.omt.labelmanager.catalog.release.infrastructure.TrackEntity;
import org.omt.labelmanager.catalog.release.infrastructure.TrackRemixerRepository;
import org.omt.labelmanager.catalog.release.infrastructure.TrackRepository;
import org.springframework.stereotype.Service;

@Service
class DeleteTracksUseCase {

    private final TrackRepository trackRepository;
    private final TrackArtistRepository trackArtistRepository;
    private final TrackRemixerRepository trackRemixerRepository;

    DeleteTracksUseCase(
            TrackRepository trackRepository,
            TrackArtistRepository trackArtistRepository,
            TrackRemixerRepository trackRemixerRepository
    ) {
        this.trackRepository = trackRepository;
        this.trackArtistRepository = trackArtistRepository;
        this.trackRemixerRepository = trackRemixerRepository;
    }

    public void deleteTracksForRelease(Long releaseId) {
        List<TrackEntity> existingTracks =
                trackRepository.findByReleaseIdOrderByPosition(releaseId);

        for (TrackEntity trackEntity : existingTracks) {
            trackArtistRepository.deleteAllByTrackId(
                    trackEntity.getId()
            );
            trackRemixerRepository.deleteAllByTrackId(
                    trackEntity.getId()
            );
        }

        trackRepository.deleteAll(existingTracks);
    }
}
