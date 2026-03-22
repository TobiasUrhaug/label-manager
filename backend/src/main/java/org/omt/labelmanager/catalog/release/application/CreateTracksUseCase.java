package org.omt.labelmanager.catalog.release.application;

import java.util.List;
import org.omt.labelmanager.catalog.release.domain.TrackInput;
import org.omt.labelmanager.catalog.release.infrastructure.TrackArtistRepository;
import org.omt.labelmanager.catalog.release.infrastructure.TrackEntity;
import org.omt.labelmanager.catalog.release.infrastructure.TrackRemixerRepository;
import org.omt.labelmanager.catalog.release.infrastructure.TrackRepository;
import org.springframework.stereotype.Service;

@Service
class CreateTracksUseCase {

    private final TrackRepository trackRepository;
    private final TrackArtistRepository trackArtistRepository;
    private final TrackRemixerRepository trackRemixerRepository;

    CreateTracksUseCase(
            TrackRepository trackRepository,
            TrackArtistRepository trackArtistRepository,
            TrackRemixerRepository trackRemixerRepository
    ) {
        this.trackRepository = trackRepository;
        this.trackArtistRepository = trackArtistRepository;
        this.trackRemixerRepository = trackRemixerRepository;
    }

    public void createTracksForRelease(
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

            for (Long remixerId : trackInput.remixerIds()) {
                trackRemixerRepository.addRemixerToTrack(
                        trackEntity.getId(), remixerId
                );
            }
        }
    }
}
