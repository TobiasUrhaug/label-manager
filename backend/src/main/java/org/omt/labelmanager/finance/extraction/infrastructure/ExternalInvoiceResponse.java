package org.omt.labelmanager.finance.extraction.infrastructure;

record ExternalInvoiceResponse(
        String invoiceDate,
        String invoiceReference,
        MoneyAmount netAmount,
        MoneyAmount vatAmount,
        MoneyAmount totalAmount
) {
    record MoneyAmount(String amount, String currency) {}
}
