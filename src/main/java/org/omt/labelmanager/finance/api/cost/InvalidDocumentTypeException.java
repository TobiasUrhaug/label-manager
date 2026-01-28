package org.omt.labelmanager.finance.api.cost;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidDocumentTypeException extends RuntimeException {

    public InvalidDocumentTypeException(String contentType) {
        super("Invalid document type: " + contentType
                + ". Allowed types: PDF, PNG, JPEG, Word, Excel");
    }
}
