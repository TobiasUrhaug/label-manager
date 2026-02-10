package org.omt.labelmanager.finance.shared;

import java.io.InputStream;

/**
 * Represents a document retrieved from storage.
 */
public record RetrievedDocument(
        InputStream content,
        String contentType,
        String filename,
        long contentLength
) {
}
