package org.omt.labelmanager.finance.cost;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.finance.cost.CostOwner;
import org.omt.labelmanager.finance.cost.CostOwnerType;

import static org.assertj.core.api.Assertions.assertThat;

class CostOwnerTest {

    @Test
    void createsReleaseOwner() {
        var owner = CostOwner.release(42L);

        assertThat(owner.type()).isEqualTo(CostOwnerType.RELEASE);
        assertThat(owner.id()).isEqualTo(42L);
    }

    @Test
    void createsLabelOwner() {
        var owner = CostOwner.label(10L);

        assertThat(owner.type()).isEqualTo(CostOwnerType.LABEL);
        assertThat(owner.id()).isEqualTo(10L);
    }

    @Test
    void createsUserOwner() {
        var owner = CostOwner.user(5L);

        assertThat(owner.type()).isEqualTo(CostOwnerType.USER);
        assertThat(owner.id()).isEqualTo(5L);
    }
}
