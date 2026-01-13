package org.omt.labelmanager.release;

import org.omt.labelmanager.label.persistence.LabelEntity;
import org.omt.labelmanager.label.persistence.LabelRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ReleaseService {

    private final ReleaseRepository releaseRepository;
    private final LabelRepository labelRepository;

    public ReleaseService(ReleaseRepository releaseRepository, LabelRepository labelRepository) {
        this.releaseRepository = releaseRepository;
        this.labelRepository = labelRepository;
    }

    public List<Release> getReleasesForLabel(Long labelId) {
        return releaseRepository.findByLabelId(labelId);
    }

    public void createRelease(String name, LocalDate releaseDate, Long labelId) {
        LabelEntity labelEntity = labelRepository.findById(labelId).orElseThrow(IllegalArgumentException::new);
        Release release = new Release();
        release.setName(name);
        release.setReleaseDate(releaseDate);
        release.setLabel(labelEntity);
        releaseRepository.save(release);
    }

    public Optional<Release> findById(long id) {
        return releaseRepository.findById(id);
    }
}
