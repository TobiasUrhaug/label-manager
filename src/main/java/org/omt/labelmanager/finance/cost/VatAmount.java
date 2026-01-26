package org.omt.labelmanager.finance.cost;

import java.math.BigDecimal;
import org.omt.labelmanager.finance.shared.Money;

public record VatAmount(Money amount, BigDecimal rate) {
}
