package org.fcrepo.exceptionhandlers;

import javax.jcr.RepositoryException;

public class InvalidChecksumException
	extends RepositoryException {

    private static final long serialVersionUID = 1L;

    public InvalidChecksumException(String message) {
		super(message);
	}
}
