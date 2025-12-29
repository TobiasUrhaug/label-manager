package org.omt.labelmanager.release;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReleaseRepository extends JpaRepository<Release, Long> {

    List<Release> findByLabelId(Long labelId);

}
