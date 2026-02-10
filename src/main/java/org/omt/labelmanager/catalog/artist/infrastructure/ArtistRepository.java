package org.omt.labelmanager.catalog.artist.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtistRepository extends JpaRepository<ArtistEntity, Long> {
    List<ArtistEntity> findByUserId(Long userId);

    Optional<ArtistEntity> findByArtistName(String artistName);
}
