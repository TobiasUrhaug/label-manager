package org.omt.labelmanager.finance.domain.shared;

import java.math.BigDecimal;

public record Money(BigDecimal amount, String currency) {

    private static final String DEFAULT_CURRENCY = "EUR";

    public static Money of(BigDecimal amount) {
        return new Money(amount, DEFAULT_CURRENCY);
    }
}
