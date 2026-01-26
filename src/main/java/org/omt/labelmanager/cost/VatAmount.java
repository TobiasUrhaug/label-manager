package org.omt.labelmanager.cost;

import java.math.BigDecimal;
import org.omt.labelmanager.common.Money;

public record VatAmount(Money amount, BigDecimal rate) {
}
