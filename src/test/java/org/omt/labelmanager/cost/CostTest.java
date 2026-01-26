package org.omt.labelmanager.cost;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.common.Money;

class CostTest {

    @Test
    void createsCostWithAllFields() {
        var netAmount = Money.of(new BigDecimal("100.00"));
        var vatAmount = new VatAmount(Money.of(new BigDecimal("25.00")), new BigDecimal("0.25"));
        var grossAmount = Money.of(new BigDecimal("125.00"));
        var owner = CostOwner.release(1L);
        var incurredOn = LocalDate.of(2024, 6, 15);

        var cost = new Cost(
                1L,
                netAmount,
                vatAmount,
                grossAmount,
                CostType.MASTERING,
                incurredOn,
                "Mastering for album",
                owner,
                "INV-2024-001"
        );

        assertThat(cost.id()).isEqualTo(1L);
        assertThat(cost.netAmount()).isEqualTo(netAmount);
        assertThat(cost.vat()).isEqualTo(vatAmount);
        assertThat(cost.grossAmount()).isEqualTo(grossAmount);
        assertThat(cost.type()).isEqualTo(CostType.MASTERING);
        assertThat(cost.incurredOn()).isEqualTo(incurredOn);
        assertThat(cost.description()).isEqualTo("Mastering for album");
        assertThat(cost.owner()).isEqualTo(owner);
        assertThat(cost.documentReference()).isEqualTo("INV-2024-001");
    }
}
