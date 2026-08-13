package org.omt.labelmanager.finance.shared;

import java.io.InputStream;

/**
 * Port for storing and retrieving the documents that back financial records — invoices and
 * receipts. Owned by {@code finance}; implementations live in {@code infrastructure} and may use
 * S3, the local filesystem, or another storage backend.
 */
public interface DocumentStoragePort {

    /**
     * Stores a document and returns a reference key for later retrieval.
     *
     * @param filename the original filename
     * @param contentType the MIME type of the document
     * @param content the document content stream
     * @return a storage key that can be used to retrieve the document
     * @throws DocumentStorageException if the storage backend rejects or fails the write
     */
    String store(String filename, String contentType, InputStream content);

    /**
     * Retrieves a document from storage.
     *
     * @param storageKey the key returned from a previous store() call
     * @return the retrieved document with content stream and metadata
     * @throws DocumentStorageException if the document cannot be read from the storage backend
     */
    RetrievedDocument retrieve(String storageKey);

    /**
     * Deletes a document from storage.
     *
     * @param storageKey the key returned from a previous store() call
     * @throws DocumentStorageException if the storage backend fails the delete
     */
    void delete(String storageKey);
}
