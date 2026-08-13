package org.omt.labelmanager.sales.sale.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.shared.Money;

class SaleLineItemTest {

    @Test
    void lineTotal_calculatesCorrectly() {
        var unitPrice = new Money(new BigDecimal("15.00"), "EUR");
        int quantity = 3;
        var expectedTotal = new Money(new BigDecimal("45.00"), "EUR");

        var lineItem =
                new SaleLineItem(1L, 100L, ReleaseFormat.VINYL, quantity, unitPrice, expectedTotal);

        assertThat(lineItem.lineTotal().amount()).isEqualByComparingTo(new BigDecimal("45.00"));
    }
}
