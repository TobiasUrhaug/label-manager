package org.omt.labelmanager.catalog.release;

import org.omt.labelmanager.catalog.release.api.ReleaseCommandFacade;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Public helper for creating test release data.
 * Used by integration tests in other modules that need release fixtures.
 */
@Component
public class ReleaseTestHelper {

    private final ReleaseRepository releaseRepository;
    private final ReleaseCommandFacade releaseCommandFacade;

    public ReleaseTestHelper(
            ReleaseRepository releaseRepository,
            ReleaseCommandFacade releaseCommandFacade
    ) {
        this.releaseRepository = releaseRepository;
        this.releaseCommandFacade = releaseCommandFacade;
    }

    public Long createRelease(
            String name,
            Long labelId,
            Long artistId
    ) {
        releaseCommandFacade.createRelease(
                name,
                LocalDate.now(),
                labelId,
                List.of(artistId),
                List.of(new TrackInput(
                        List.of(artistId),
                        "Default Track",
                        TrackDuration.ofSeconds(180),
                        1
                )),
                Set.of(ReleaseFormat.DIGITAL)
        );
        return releaseRepository.findByName(name)
                .orElseThrow()
                .getId();
    }

    public Long createReleaseEntity(
            String name,
            Long labelId
    ) {
        ReleaseEntity entity = new ReleaseEntity();
        entity.setName(name);
        entity.setReleaseDate(LocalDate.now());
        entity.setLabelId(labelId);
        return releaseRepository.save(entity).getId();
    }
}
