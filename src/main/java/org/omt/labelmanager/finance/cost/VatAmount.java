package org.omt.labelmanager.finance.cost;

import org.omt.labelmanager.finance.domain.shared.Money;

import java.math.BigDecimal;

public record VatAmount(Money amount, BigDecimal rate) {
}
