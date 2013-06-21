package org.fcrepo.exception;

import javax.jcr.RepositoryException;

public class TransactionMissingException extends RepositoryException {

    public TransactionMissingException(final String s) {
        super(s);
    }
}
