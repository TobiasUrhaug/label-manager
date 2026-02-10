package org.omt.labelmanager.catalog.release;

import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

@org.springframework.stereotype.Repository
interface TrackArtistRepository
        extends Repository<TrackEntity, Long> {

    @Query(value = """
            SELECT a.id FROM artist a
            INNER JOIN track_artist ta ON ta.artist_id = a.id
            WHERE ta.track_id = :trackId
            """, nativeQuery = true)
    List<Long> findArtistIdsByTrackId(
            @Param("trackId") Long trackId
    );

    @Modifying
    @Query(value = """
            INSERT INTO track_artist (track_id, artist_id)
            VALUES (:trackId, :artistId)
            """, nativeQuery = true)
    void addArtistToTrack(
            @Param("trackId") Long trackId,
            @Param("artistId") Long artistId
    );

    @Modifying
    @Query(value = """
            DELETE FROM track_artist
            WHERE track_id = :trackId
            """, nativeQuery = true)
    void deleteAllByTrackId(
            @Param("trackId") Long trackId
    );
}
