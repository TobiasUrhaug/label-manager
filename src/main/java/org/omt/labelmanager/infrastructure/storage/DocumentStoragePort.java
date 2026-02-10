package org.omt.labelmanager.infrastructure.storage;

import org.omt.labelmanager.finance.shared.RetrievedDocument;

import java.io.InputStream;

/**
 * Port for storing and retrieving documents across the application.
 * Used for invoices, receipts, contracts, and other business documents.
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

    /**
     * Retrieves a document from storage.
     *
     * @param storageKey the key returned from a previous store() call
     * @return the retrieved document with content stream and metadata
     */
    RetrievedDocument retrieve(String storageKey);

    /**
     * Deletes a document from storage.
     *
     * @param storageKey the key returned from a previous store() call
     */
    void delete(String storageKey);
}
