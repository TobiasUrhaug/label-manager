package org.omt.labelmanager.distribution.agreement;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingAgreementTest {

    private static final Long ID = 1L;
    private static final Long DISTRIBUTOR_ID = 2L;
    private static final Long RUN_ID = 3L;

    @Test
    void constructor_withValidPercentage_doesNotThrow() {
        assertThatCode(() -> agreement(new BigDecimal("9.99"), CommissionType.PERCENTAGE, new BigDecimal("15.00")))
                .doesNotThrowAnyException();
    }

    @Test
    void constructor_withUnitPriceZero_throws() {
        assertThatThrownBy(() -> agreement(BigDecimal.ZERO, CommissionType.PERCENTAGE, new BigDecimal("15.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unit price must be greater than zero");
    }

    @Test
    void constructor_withUnitPriceNegative_throws() {
        assertThatThrownBy(() -> agreement(new BigDecimal("-1.00"), CommissionType.PERCENTAGE, new BigDecimal("15.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unit price must be greater than zero");
    }

    @Test
    void constructor_withPercentageZero_doesNotThrow() {
        assertThatCode(() -> agreement(new BigDecimal("9.99"), CommissionType.PERCENTAGE, BigDecimal.ZERO))
                .doesNotThrowAnyException();
    }

    @Test
    void constructor_withPercentageHundred_doesNotThrow() {
        assertThatCode(() -> agreement(new BigDecimal("9.99"), CommissionType.PERCENTAGE, new BigDecimal("100")))
                .doesNotThrowAnyException();
    }

    @Test
    void constructor_withPercentageOver100_throws() {
        assertThatThrownBy(() -> agreement(new BigDecimal("9.99"), CommissionType.PERCENTAGE, new BigDecimal("100.01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Commission percentage must be between 0 and 100");
    }

    @Test
    void constructor_withPercentageNegative_throws() {
        assertThatThrownBy(() -> agreement(new BigDecimal("9.99"), CommissionType.PERCENTAGE, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Commission percentage must be between 0 and 100");
    }

    @Test
    void constructor_withFixedAmountPositive_doesNotThrow() {
        assertThatCode(() -> agreement(new BigDecimal("9.99"), CommissionType.FIXED_AMOUNT, new BigDecimal("0.01")))
                .doesNotThrowAnyException();
    }

    @Test
    void constructor_withFixedAmountZero_throws() {
        assertThatThrownBy(() -> agreement(new BigDecimal("9.99"), CommissionType.FIXED_AMOUNT, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Commission value must be greater than zero");
    }

    @Test
    void constructor_withFixedAmountOver100_doesNotThrow() {
        assertThatCode(() -> agreement(new BigDecimal("9.99"), CommissionType.FIXED_AMOUNT, new BigDecimal("101")))
                .doesNotThrowAnyException();
    }

    private PricingAgreement agreement(BigDecimal unitPrice, CommissionType commissionType, BigDecimal commissionValue) {
        return new PricingAgreement(ID, DISTRIBUTOR_ID, RUN_ID, unitPrice, commissionType, commissionValue, Instant.now());
    }
}
