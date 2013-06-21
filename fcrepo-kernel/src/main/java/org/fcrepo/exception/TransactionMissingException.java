package org.fcrepo.exception;

import javax.jcr.RepositoryException;

public class TransactionMissingException extends RepositoryException {

    private static final long serialVersionUID = 2139084821001303830L;

    public TransactionMissingException(final String s) {
        super(s);
    }
}
