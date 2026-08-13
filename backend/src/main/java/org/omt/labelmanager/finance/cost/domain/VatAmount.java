package org.omt.labelmanager.finance.cost.domain;

import java.math.BigDecimal;
import org.omt.labelmanager.shared.Money;

public record VatAmount(Money amount, BigDecimal rate) {}
