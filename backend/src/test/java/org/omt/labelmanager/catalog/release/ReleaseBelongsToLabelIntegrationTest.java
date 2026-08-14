package org.omt.labelmanager.catalog.release;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.api.ReleaseCommandApi;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.catalog.release.domain.TrackDuration;
import org.omt.labelmanager.catalog.release.domain.TrackInput;
import org.omt.labelmanager.shared.Format;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Every web guard that decides whether one label's release may be served under another's id reduces
 * to this one comparison, and every controller test mocks it away. Without this, inverting it would
 * leave the whole suite green.
 */
public class ReleaseBelongsToLabelIntegrationTest extends AbstractIntegrationTest {

    @Autowired LabelTestHelper labelTestHelper;

    @Autowired ReleaseCommandApi releaseCommandApi;

    @Autowired ReleaseQueryApi releaseQueryApi;

    @Test
    void belongsToLabel_isTrueOnlyForTheOwningLabel() {
        var label = labelTestHelper.createLabel("Owning Label");
        var otherLabel = labelTestHelper.createLabel("Other Label");
        releaseCommandApi.createRelease(
                "A Release",
                LocalDate.of(2026, 3, 1),
                label.id(),
                List.of(),
                List.of(
                        new TrackInput(
                                List.of(), "A Track", TrackDuration.parse("3:30"), 1, List.of())),
                Set.of(Format.VINYL));
        var releaseId = releaseQueryApi.getReleasesForLabel(label.id()).getFirst().id();

        assertThat(releaseQueryApi.belongsToLabel(releaseId, label.id())).isTrue();
        assertThat(releaseQueryApi.belongsToLabel(releaseId, otherLabel.id())).isFalse();
    }

    @Test
    void belongsToLabel_isFalseWhenTheReleaseDoesNotExist() {
        var label = labelTestHelper.createLabel("Owning Label");

        assertThat(releaseQueryApi.belongsToLabel(999_999L, label.id())).isFalse();
    }
}
