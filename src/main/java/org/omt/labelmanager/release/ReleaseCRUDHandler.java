package org.omt.labelmanager.release;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.omt.labelmanager.label.persistence.LabelEntity;
import org.omt.labelmanager.label.persistence.LabelRepository;
import org.omt.labelmanager.release.persistence.ReleaseEntity;
import org.omt.labelmanager.release.persistence.ReleaseRepository;
import org.omt.labelmanager.track.TrackInput;
import org.omt.labelmanager.track.persistence.TrackEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ReleaseCRUDHandler {

    private static final Logger log = LoggerFactory.getLogger(ReleaseCRUDHandler.class);

    private final ReleaseRepository releaseRepository;
    private final LabelRepository labelRepository;

    public ReleaseCRUDHandler(
            ReleaseRepository releaseRepository,
            LabelRepository labelRepository) {
        this.releaseRepository = releaseRepository;
        this.labelRepository = labelRepository;
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
            List<TrackInput> tracks
    ) {
        log.info("Creating release '{}' for label {} with {} tracks", name, labelId, tracks.size());
        if (tracks.isEmpty()) {
            log.warn("Cannot create release '{}': at least one track is required", name);
            throw new IllegalArgumentException("At least one track is required");
        }
        LabelEntity labelEntity = labelRepository.findById(labelId)
                .orElseThrow(() -> {
                    log.warn("Cannot create release: label {} not found", labelId);
                    return new IllegalArgumentException();
                });
        ReleaseEntity release = new ReleaseEntity();
        release.setName(name);
        release.setReleaseDate(releaseDate);
        release.setLabel(labelEntity);

        for (TrackInput trackInput : tracks) {
            TrackEntity trackEntity = new TrackEntity();
            trackEntity.setArtist(trackInput.artist());
            trackEntity.setName(trackInput.name());
            trackEntity.setDurationSeconds(trackInput.duration().totalSeconds());
            trackEntity.setPosition(trackInput.position());
            trackEntity.setRelease(release);
            release.getTracks().add(trackEntity);
        }

        releaseRepository.save(release);
    }

    public Optional<Release> findById(long id) {
        Optional<Release> release = releaseRepository.findById(id).map(Release::fromEntity);
        if (release.isEmpty()) {
            log.debug("Release with id {} not found", id);
        }
        return release;
    }
}
