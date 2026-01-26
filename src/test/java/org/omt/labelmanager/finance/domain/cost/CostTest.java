package org.omt.labelmanager.finance.domain.cost;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.finance.domain.shared.Money;
import org.omt.labelmanager.finance.infrastructure.persistence.cost.CostEntity;
import org.omt.labelmanager.finance.infrastructure.persistence.cost.CostOwnerEmbeddable;

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

    @Test
    void fromEntity_mapsAllFields() {
        var entity = new CostEntity(
                "EUR",
                new BigDecimal("100.00"),
                new BigDecimal("25.00"),
                new BigDecimal("0.25"),
                new BigDecimal("125.00"),
                CostType.MANUFACTURING,
                LocalDate.of(2024, 3, 10),
                "Vinyl pressing",
                new CostOwnerEmbeddable(CostOwnerType.RELEASE, 42L),
                "INV-2024-002"
        );

        var cost = Cost.fromEntity(entity);

        assertThat(cost.netAmount().amount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(cost.netAmount().currency()).isEqualTo("EUR");
        assertThat(cost.vat().amount().amount()).isEqualTo(new BigDecimal("25.00"));
        assertThat(cost.vat().rate()).isEqualTo(new BigDecimal("0.25"));
        assertThat(cost.grossAmount().amount()).isEqualTo(new BigDecimal("125.00"));
        assertThat(cost.type()).isEqualTo(CostType.MANUFACTURING);
        assertThat(cost.incurredOn()).isEqualTo(LocalDate.of(2024, 3, 10));
        assertThat(cost.description()).isEqualTo("Vinyl pressing");
        assertThat(cost.owner().type()).isEqualTo(CostOwnerType.RELEASE);
        assertThat(cost.owner().id()).isEqualTo(42L);
        assertThat(cost.documentReference()).isEqualTo("INV-2024-002");
    }
}
