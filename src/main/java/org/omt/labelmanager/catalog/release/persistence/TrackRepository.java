package org.omt.labelmanager.catalog.release.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackRepository extends JpaRepository<TrackEntity, Long> {

    List<TrackEntity> findByReleaseIdOrderByPosition(
            Long releaseId
    );

}
