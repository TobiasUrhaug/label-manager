package org.omt.labelmanager.catalog.artist.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<ArtistEntity, Long> {

    Optional<ArtistEntity> findByArtistName(String artistName);

    List<ArtistEntity> findByUserId(Long userId);

}
