package org.omt.labelmanager.catalog.infrastructure.persistence.release;

import java.util.List;
import org.omt.labelmanager.catalog.infrastructure.persistence.artist.ArtistEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

@org.springframework.stereotype.Repository
public interface ReleaseArtistRepository extends Repository<ArtistEntity, Long> {

    @Query(value = """
            SELECT a.* FROM artist a
            INNER JOIN release_artist ra ON ra.artist_id = a.id
            WHERE ra.release_id = :releaseId
            """, nativeQuery = true)
    List<ArtistEntity> findArtistsByReleaseId(
            @Param("releaseId") Long releaseId
    );

    @Modifying
    @Query(value = """
            INSERT INTO release_artist (release_id, artist_id)
            VALUES (:releaseId, :artistId)
            """, nativeQuery = true)
    void addArtistToRelease(
            @Param("releaseId") Long releaseId,
            @Param("artistId") Long artistId
    );

    @Modifying
    @Query(value = """
            DELETE FROM release_artist
            WHERE release_id = :releaseId
            """, nativeQuery = true)
    void deleteAllByReleaseId(@Param("releaseId") Long releaseId);
}
