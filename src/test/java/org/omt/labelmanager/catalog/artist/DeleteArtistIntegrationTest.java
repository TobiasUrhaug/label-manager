package org.omt.labelmanager.catalog.artist;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.artist.api.ArtistCommandApi;
import org.omt.labelmanager.catalog.artist.api.ArtistQueryApi;
import org.omt.labelmanager.catalog.artist.infrastructure.ArtistEntity;
import org.omt.labelmanager.catalog.artist.infrastructure.ArtistRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class DeleteArtistIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ArtistCommandApi artistCommandApi;

    @Autowired
    ArtistQueryApi artistQueryApi;

    @Autowired
    ArtistRepository artistRepository;

    @Test
    void deleteArtist_removesFromDatabase() {
        var entity = new ArtistEntity("Artist To Delete");
        artistRepository.save(entity);

        assertThat(artistQueryApi.findById(entity.getId())).isPresent();

        artistCommandApi.delete(entity.getId());

        assertThat(artistQueryApi.findById(entity.getId())).isEmpty();
    }
}
