package org.fcrepo.exception;

import javax.jcr.RepositoryException;

/**
 * A transaction was not found in the transaction registry
 */
public class TransactionMissingException extends RepositoryException {

    private static final long serialVersionUID = 2139084821001303830L;

    /**
     *
     * @param s the exception message
     */
    public TransactionMissingException(final String s) {
        super(s);
    }
}
