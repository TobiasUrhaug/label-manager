package org.omt.labelmanager.finance.cost;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.finance.cost.domain.VatAmount;
import org.omt.labelmanager.finance.domain.shared.Money;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class VatAmountTest {

    @Test
    void createsVatAmountWithAmountAndRate() {
        var vat = new VatAmount(Money.of(new BigDecimal("25.00")), new BigDecimal("0.25"));

        assertThat(vat.amount().amount()).isEqualTo(new BigDecimal("25.00"));
        assertThat(vat.rate()).isEqualTo(new BigDecimal("0.25"));
    }
}
