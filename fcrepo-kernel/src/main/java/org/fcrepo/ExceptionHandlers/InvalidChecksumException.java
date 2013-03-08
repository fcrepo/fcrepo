package org.fcrepo.exceptionhandlers;

import javax.jcr.RepositoryException;

public class InvalidChecksumException
	extends RepositoryException {

	public InvalidChecksumException(String message) {
		super(message);
	}
}
