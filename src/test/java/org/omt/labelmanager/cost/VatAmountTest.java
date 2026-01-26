package org.omt.labelmanager.cost;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.common.Money;

class VatAmountTest {

    @Test
    void createsVatAmountWithAmountAndRate() {
        var vat = new VatAmount(Money.of(new BigDecimal("25.00")), new BigDecimal("0.25"));

        assertThat(vat.amount().amount()).isEqualTo(new BigDecimal("25.00"));
        assertThat(vat.rate()).isEqualTo(new BigDecimal("0.25"));
    }
}
