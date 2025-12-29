package org.omt.labelmanager.release;

import org.omt.labelmanager.label.Label;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ReleaseService {

    private final ReleaseRepository repo;

    public ReleaseService(ReleaseRepository repo) {
        this.repo = repo;
    }

    public List<Release> getReleasesForLabel(Long labelId) {
        return repo.findByLabelId(labelId);
    }

    public Release createRelease(String name, LocalDate releaseDate, Label label) {
        Release release = new Release();
        release.setName(name);
        release.setReleaseDate(releaseDate);
        release.setLabel(label);
        return repo.save(release);
    }

    public Optional<Release> findById(long id) {
        return repo.findById(id);
    }
}
