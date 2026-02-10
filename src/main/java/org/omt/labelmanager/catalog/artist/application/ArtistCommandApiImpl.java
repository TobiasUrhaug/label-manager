package org.omt.labelmanager.catalog.artist.application;

import org.omt.labelmanager.catalog.artist.api.ArtistCommandApi;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.springframework.stereotype.Service;

@Service
class ArtistCommandApiImpl implements ArtistCommandApi {

    private final CreateArtistUseCase createArtist;
    private final UpdateArtistUseCase updateArtist;
    private final DeleteArtistUseCase deleteArtist;

    ArtistCommandApiImpl(
            CreateArtistUseCase createArtist,
            UpdateArtistUseCase updateArtist,
            DeleteArtistUseCase deleteArtist
    ) {
        this.createArtist = createArtist;
        this.updateArtist = updateArtist;
        this.deleteArtist = deleteArtist;
    }

    @Override
    public void createArtist(
            String artistName,
            Person realName,
            String email,
            Address address,
            Long userId
    ) {
        createArtist.execute(artistName, realName, email, address, userId);
    }

    @Override
    public void updateArtist(
            Long id,
            String artistName,
            Person realName,
            String email,
            Address address
    ) {
        updateArtist.execute(id, artistName, realName, email, address);
    }

    @Override
    public void delete(Long id) {
        deleteArtist.execute(id);
    }
}
