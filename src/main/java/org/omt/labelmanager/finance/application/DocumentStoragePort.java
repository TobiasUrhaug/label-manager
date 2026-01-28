package org.omt.labelmanager.finance.application;

import java.io.InputStream;

/**
 * Port for storing documents (invoices, receipts) associated with costs.
 * Implementations may use S3, local filesystem, or other storage backends.
 */
public interface DocumentStoragePort {

    /**
     * Stores a document and returns a reference key for later retrieval.
     *
     * @param filename the original filename
     * @param contentType the MIME type of the document
     * @param content the document content stream
     * @return a storage key that can be used to retrieve the document
     */
    String store(String filename, String contentType, InputStream content);
}
