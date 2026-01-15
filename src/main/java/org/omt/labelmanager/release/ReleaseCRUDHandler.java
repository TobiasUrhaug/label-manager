package org.omt.labelmanager.release;

import org.omt.labelmanager.label.persistence.LabelEntity;
import org.omt.labelmanager.label.persistence.LabelRepository;
import org.omt.labelmanager.release.persistence.ReleaseEntity;
import org.omt.labelmanager.release.persistence.ReleaseRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ReleaseCRUDHandler {

    private final ReleaseRepository releaseRepository;
    private final LabelRepository labelRepository;

    public ReleaseCRUDHandler(ReleaseRepository releaseRepository, LabelRepository labelRepository) {
        this.releaseRepository = releaseRepository;
        this.labelRepository = labelRepository;
    }

    public List<Release> getReleasesForLabel(Long labelId) {
        return releaseRepository.findByLabelId(labelId).stream().map(Release::fromEntity).toList();
    }

    public void createRelease(String name, LocalDate releaseDate, Long labelId) {
        LabelEntity labelEntity = labelRepository.findById(labelId).orElseThrow(IllegalArgumentException::new);
        ReleaseEntity release = new ReleaseEntity();
        release.setName(name);
        release.setReleaseDate(releaseDate);
        release.setLabel(labelEntity);
        releaseRepository.save(release);
    }

    public Optional<Release> findById(long id) {
        return releaseRepository.findById(id).map(Release::fromEntity);
    }
}
