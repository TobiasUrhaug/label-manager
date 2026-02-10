package org.omt.labelmanager.finance.application.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.finance.extraction.domain.ExtractedInvoiceData;

class ExtractedInvoiceDataTest {

    @Test
    void emptyReturnsInstanceWithAllNullFields() {
        var empty = ExtractedInvoiceData.empty();

        assertThat(empty.netAmount()).isNull();
        assertThat(empty.vatAmount()).isNull();
        assertThat(empty.vatRate()).isNull();
        assertThat(empty.grossAmount()).isNull();
        assertThat(empty.invoiceDate()).isNull();
        assertThat(empty.invoiceReference()).isNull();
        assertThat(empty.currency()).isNull();
    }

    @Test
    void hasAnyDataReturnsFalseWhenAllFieldsAreNull() {
        var empty = ExtractedInvoiceData.empty();

        assertThat(empty.hasAnyData()).isFalse();
    }

    @Test
    void hasAnyDataReturnsTrueWhenNetAmountIsPresent() {
        var data = new ExtractedInvoiceData(
                new BigDecimal("100.00"), null, null, null, null, null, null);

        assertThat(data.hasAnyData()).isTrue();
    }

    @Test
    void hasAnyDataReturnsTrueWhenOnlyInvoiceDateIsPresent() {
        var data = new ExtractedInvoiceData(
                null, null, null, null, LocalDate.of(2024, 1, 15), null, null);

        assertThat(data.hasAnyData()).isTrue();
    }

    @Test
    void hasAnyDataReturnsTrueWhenOnlyCurrencyIsPresent() {
        var data = new ExtractedInvoiceData(
                null, null, null, null, null, null, "EUR");

        assertThat(data.hasAnyData()).isTrue();
    }
}
