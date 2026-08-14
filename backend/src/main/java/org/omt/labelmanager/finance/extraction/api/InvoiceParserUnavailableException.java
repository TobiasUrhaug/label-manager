package org.omt.labelmanager.finance.extraction.api;

/**
 * The external invoice parser could not be reached, or answered with an error status.
 *
 * <p>Distinct from a parse that succeeded and found nothing: this one means the integration is
 * broken and someone should look at it, so it surfaces as 502 rather than an empty 200.
 */
public class InvoiceParserUnavailableException extends RuntimeException {

    public InvoiceParserUnavailableException(String message) {
        super(message);
    }

    public InvoiceParserUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
