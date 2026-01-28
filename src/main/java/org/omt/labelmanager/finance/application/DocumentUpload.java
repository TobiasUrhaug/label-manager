package org.omt.labelmanager.finance.application;

import java.io.InputStream;

/**
 * Represents a document to be uploaded and stored.
 */
public record DocumentUpload(
        String filename,
        String contentType,
        InputStream content
) {
}
