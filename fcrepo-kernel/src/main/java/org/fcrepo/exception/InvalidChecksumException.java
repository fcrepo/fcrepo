/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.exception;

/**
 * Exception thrown when the calculated digest does not match the stored digest
 * @author ajs6f
 * @date Mar 10, 2013
 */
public class InvalidChecksumException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * @todo Add Documentation.
     */
    public InvalidChecksumException(final String message) {
        super(message);
    }
}
