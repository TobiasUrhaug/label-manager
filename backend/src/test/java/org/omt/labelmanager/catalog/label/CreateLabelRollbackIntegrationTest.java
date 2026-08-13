package org.omt.labelmanager.catalog.label;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.api.LabelCommandApi;
import org.omt.labelmanager.catalog.label.infrastructure.LabelRepository;
import org.omt.labelmanager.distribution.distributor.api.DistributorCommandApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Provisioning the DIRECT distributor is driven by a LabelCreated event. It must stay in the
 * label's transaction, so a failure there takes the label with it. Running the listener after
 * commit instead would leave the label persisted, which this test forbids.
 */
public class CreateLabelRollbackIntegrationTest extends AbstractIntegrationTest {

    @Autowired LabelCommandApi labelCommandApi;

    @Autowired LabelRepository labelRepository;

    @MockitoBean DistributorCommandApi distributorCommandApi;

    @Test
    void createLabel_rollsBackTheLabel_whenDistributorProvisioningFails() {
        when(distributorCommandApi.createDistributor(anyLong(), anyString(), any()))
                .thenThrow(new IllegalStateException("provisioning failed"));

        assertThatThrownBy(
                        () ->
                                labelCommandApi.createLabel(
                                        "Doomed Label", null, null, null, null, null))
                .isInstanceOf(IllegalStateException.class);

        assertThat(labelRepository.findByName("Doomed Label")).isEmpty();
    }
}
