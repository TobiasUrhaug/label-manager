package org.omt.labelmanager.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omt.labelmanager.catalog.label.LabelFactory;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.release.ReleaseFactory;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.distribution.distributor.DistributorFactory;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;

/**
 * Every controller mocks LabelScope, so without this the comparison that decides whether one
 * label's data is served under another's would have no coverage at all.
 */
@ExtendWith(MockitoExtension.class)
class LabelScopeTest {

    private static final Long LABEL_ID = 1L;
    private static final Long OTHER_LABEL_ID = 2L;
    private static final Long RELEASE_ID = 10L;
    private static final Long DISTRIBUTOR_ID = 20L;

    @Mock private LabelQueryApi labelQueryApi;

    @Mock private ReleaseQueryApi releaseQueryApi;

    @Mock private DistributorQueryApi distributorQueryApi;

    private LabelScope labelScope;

    @BeforeEach
    void setUp() {
        labelScope = new LabelScope(labelQueryApi, releaseQueryApi, distributorQueryApi);
    }

    @Test
    void requireLabel_passesWhenTheLabelExists() {
        when(labelQueryApi.findById(LABEL_ID))
                .thenReturn(Optional.of(LabelFactory.aLabel().id(LABEL_ID).build()));

        assertThatCode(() -> labelScope.requireLabel(LABEL_ID)).doesNotThrowAnyException();
    }

    @Test
    void requireLabel_throwsWhenTheLabelDoesNotExist() {
        when(labelQueryApi.findById(LABEL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labelScope.requireLabel(LABEL_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Label not found: 1");
    }

    @Test
    void isReleaseOfLabel_isTrueOnlyForTheOwningLabel() {
        when(releaseQueryApi.findById(RELEASE_ID))
                .thenReturn(
                        Optional.of(
                                ReleaseFactory.aRelease()
                                        .id(RELEASE_ID)
                                        .labelId(LABEL_ID)
                                        .build()));

        assertThat(labelScope.isReleaseOfLabel(LABEL_ID, RELEASE_ID)).isTrue();
        assertThat(labelScope.isReleaseOfLabel(OTHER_LABEL_ID, RELEASE_ID)).isFalse();
    }

    @Test
    void isReleaseOfLabel_isFalseWhenTheReleaseDoesNotExist() {
        when(releaseQueryApi.findById(RELEASE_ID)).thenReturn(Optional.empty());

        assertThat(labelScope.isReleaseOfLabel(LABEL_ID, RELEASE_ID)).isFalse();
    }

    @Test
    void requireRelease_throwsForAReleaseOfAnotherLabel() {
        when(releaseQueryApi.findById(RELEASE_ID))
                .thenReturn(
                        Optional.of(
                                ReleaseFactory.aRelease()
                                        .id(RELEASE_ID)
                                        .labelId(OTHER_LABEL_ID)
                                        .build()));

        assertThatThrownBy(() -> labelScope.requireRelease(LABEL_ID, RELEASE_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Release 10 does not belong to label 1");
    }

    @Test
    void requireDistributor_returnsTheDistributorForItsOwnLabel() {
        var distributor =
                DistributorFactory.aDistributor()
                        .id(DISTRIBUTOR_ID)
                        .labelId(LABEL_ID)
                        .name("Cargo")
                        .build();
        when(distributorQueryApi.findById(DISTRIBUTOR_ID)).thenReturn(Optional.of(distributor));

        assertThat(labelScope.requireDistributor(LABEL_ID, DISTRIBUTOR_ID).name())
                .isEqualTo("Cargo");
    }

    @Test
    void requireDistributor_throwsForADistributorOfAnotherLabel() {
        when(distributorQueryApi.findById(DISTRIBUTOR_ID))
                .thenReturn(
                        Optional.of(
                                DistributorFactory.aDistributor()
                                        .id(DISTRIBUTOR_ID)
                                        .labelId(OTHER_LABEL_ID)
                                        .build()));

        assertThatThrownBy(() -> labelScope.requireDistributor(LABEL_ID, DISTRIBUTOR_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Distributor 20 does not belong to label 1");
    }

    @Test
    void requireDistributor_throwsWhenTheDistributorDoesNotExist() {
        when(distributorQueryApi.findById(DISTRIBUTOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labelScope.requireDistributor(LABEL_ID, DISTRIBUTOR_ID))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
