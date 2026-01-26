package org.omt.labelmanager.finance.domain.cost;

import java.math.BigDecimal;
import org.omt.labelmanager.finance.domain.shared.Money;

public record VatAmount(Money amount, BigDecimal rate) {
}
