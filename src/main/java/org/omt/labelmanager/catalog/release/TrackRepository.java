package org.omt.labelmanager.catalog.release;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface TrackRepository extends JpaRepository<TrackEntity, Long> {

    List<TrackEntity> findByReleaseIdOrderByPosition(
            Long releaseId
    );

}
