
package org.fcrepo.exception;

/**
 * Exception thrown when the calculated digest does not match the stored digest
 */

public class InvalidChecksumException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidChecksumException(final String message) {
        super(message);
    }
}
