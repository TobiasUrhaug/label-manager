package org.omt.labelmanager.finance.cost.domain;

import org.omt.labelmanager.finance.domain.shared.Money;

import java.math.BigDecimal;

public record VatAmount(Money amount, BigDecimal rate) {
}
