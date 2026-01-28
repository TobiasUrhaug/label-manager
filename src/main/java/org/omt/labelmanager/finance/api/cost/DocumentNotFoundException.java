package org.omt.labelmanager.finance.api.cost;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(Long costId) {
        super("No document attached to cost: " + costId);
    }
}
