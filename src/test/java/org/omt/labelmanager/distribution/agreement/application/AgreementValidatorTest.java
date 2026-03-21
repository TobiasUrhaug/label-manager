package org.omt.labelmanager.distribution.agreement.application;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.distribution.agreement.domain.CommissionType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgreementValidatorTest {

    @Test
    void validateCommissionValue_percentage_zeroIsValid() {
        assertThatCode(() ->
                AgreementValidator.validateCommissionValue(CommissionType.PERCENTAGE, BigDecimal.ZERO)
        ).doesNotThrowAnyException();
    }

    @Test
    void validateCommissionValue_percentage_hundredIsValid() {
        assertThatCode(() ->
                AgreementValidator.validateCommissionValue(CommissionType.PERCENTAGE, new BigDecimal("100"))
        ).doesNotThrowAnyException();
    }

    @Test
    void validateCommissionValue_percentage_over100Throws() {
        assertThatThrownBy(() ->
                AgreementValidator.validateCommissionValue(CommissionType.PERCENTAGE, new BigDecimal("100.01"))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateCommissionValue_fixedAmount_positiveValueIsValid() {
        assertThatCode(() ->
                AgreementValidator.validateCommissionValue(CommissionType.FIXED_AMOUNT, new BigDecimal("0.01"))
        ).doesNotThrowAnyException();
    }

    @Test
    void validateCommissionValue_fixedAmount_zeroThrows() {
        assertThatThrownBy(() ->
                AgreementValidator.validateCommissionValue(CommissionType.FIXED_AMOUNT, BigDecimal.ZERO)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateCommissionValue_fixedAmount_over100IsValid() {
        assertThatCode(() ->
                AgreementValidator.validateCommissionValue(CommissionType.FIXED_AMOUNT, new BigDecimal("101"))
        ).doesNotThrowAnyException();
    }
}
