package org.omt.labelmanager.finance.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void createsMoneyWithAmountAndCurrency() {
        var money = new Money(new BigDecimal("100.50"), "EUR");

        assertThat(money.amount()).isEqualTo(new BigDecimal("100.50"));
        assertThat(money.currency()).isEqualTo("EUR");
    }

    @Test
    void defaultCurrencyIsEUR() {
        var money = Money.of(new BigDecimal("50.00"));

        assertThat(money.currency()).isEqualTo("EUR");
    }
}
