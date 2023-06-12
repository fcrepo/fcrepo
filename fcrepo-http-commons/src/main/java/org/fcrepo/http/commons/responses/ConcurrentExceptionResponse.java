/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.fcrepo.kernel.api.exception.ConcurrentUpdateException;

/**
 * Response body for {@link ConcurrentUpdateException}. The message always is returned, and the transaction ids are
 * returned if the fcrepo.response.include.transaction property is set.
 *
 * @author mikejritter
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConcurrentExceptionResponse {

    private final String message;

    private String existingTransactionId;

    private String conflictingTransactionId;

    /**
     * Response for {@link org.fcrepo.kernel.api.exception.ConcurrentUpdateException}
     *
     * @param message the exception message
     */
    public ConcurrentExceptionResponse(final String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String getExistingTransactionId() {
        return existingTransactionId;
    }

    public String getConflictingTransactionId() {
        return conflictingTransactionId;
    }

    public void setExistingTransactionId(final String existingTransactionId) {
        this.existingTransactionId = existingTransactionId;
    }

    public void setConflictingTransactionId(final String conflictingTransactionId) {
        this.conflictingTransactionId = conflictingTransactionId;
    }
}
