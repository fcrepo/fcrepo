
package org.fcrepo.exception;

public class InvalidChecksumException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidChecksumException(final String message) {
        super(message);
    }
}
